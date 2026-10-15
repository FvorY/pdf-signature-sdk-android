package io.github.fvory.psdk

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.MainThread
import io.github.fvory.psdk.cache.BitmapCache
import io.github.fvory.psdk.cache.ImageLoader
import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkThemeMode
import io.github.fvory.psdk.config.SkeletonOptions
import io.github.fvory.psdk.config.UploadDecision
import io.github.fvory.psdk.core.GlobalLocks
import io.github.fvory.psdk.core.IdGenerator
import io.github.fvory.psdk.core.ItemBox
import io.github.fvory.psdk.core.ItemStore
import io.github.fvory.psdk.core.PaginationController
import io.github.fvory.psdk.core.ZoomMath
import io.github.fvory.psdk.engine.DocumentEngine
import io.github.fvory.psdk.engine.PasswordFlow
import io.github.fvory.psdk.engine.PasswordRequiredException
import io.github.fvory.psdk.engine.PdfRendererEngine
import io.github.fvory.psdk.engine.RenderSize
import io.github.fvory.psdk.engine.SourceResolver
import io.github.fvory.psdk.engine.TempFileManager
import io.github.fvory.psdk.error.PsdkException
import io.github.fvory.psdk.event.EventBus
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.event.PsdkListener
import io.github.fvory.psdk.event.PsdkSubscription
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.ui.StaticOverlayPainter
import io.github.fvory.psdk.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PDF viewer with signature and E-Materai placement — the Android counterpart of the web SDK's
 * `createViewer()` instance. Method, getter and event names follow the web SDK.
 *
 * Call every method on the main thread. Show it with the `PdfSignatureViewer` composable; in
 * Compose, create it with `rememberPdfSignatureViewer` so it survives rotation.
 */
public class PdfSignatureViewer(
    context: Context,
    config: PsdkConfig = PsdkConfig(),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bus = EventBus()
    private val logger = Logger(config.logLevel)
    private val temp = TempFileManager(appContext.cacheDir)
    private val resolver = SourceResolver(appContext, temp)
    private val cache = BitmapCache()
    private val pagination = PaginationController(::onPageChanged)
    private val mutableState = MutableStateFlow(PsdkState())
    private val mutableCommands = MutableSharedFlow<ViewerCommand>(extraBufferCapacity = 16)
    private val imageLoader = ImageLoader(appContext)
    private val mutableItemBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    private val store =
        ItemStore(
            ids = IdGenerator(),
            onChanged = { items -> mutableState.update { it.copy(signatures = items) } },
            emit = { bus.emit(it) },
        )

    private var engine: DocumentEngine? = null
    private var loadToken = 0L
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    private var fitWidthPending = false

    private val configState = MutableStateFlow(config)

    /** Current configuration. Change it with [updateConfig]. */
    public val config: PsdkConfig get() = configState.value

    internal val configFlow: StateFlow<PsdkConfig> = configState.asStateFlow()

    /** Which picker is open: SIGNATURE, ESTAMP or null. */
    internal val picker = MutableStateFlow<ItemType?>(null)

    /** Skeleton overlay options while it is shown, null when hidden. */
    internal val skeleton = MutableStateFlow<SkeletonOptions?>(null)

    /** Built-in password dialog state while a password is needed, null otherwise. */
    internal val passwordPrompt = MutableStateFlow<PasswordPrompt?>(null)

    private var pendingPassword: CompletableDeferred<String?>? = null

    /** Observable snapshot of the viewer for Compose (`collectAsState`) and Flow users. */
    public val state: StateFlow<PsdkState> = mutableState.asStateFlow()

    /** Every event, for coroutine users. Same events as [on] listeners receive. */
    public val events: SharedFlow<PsdkEvent> = bus.flow

    internal val commands: SharedFlow<ViewerCommand> = mutableCommands.asSharedFlow()

    /** Loaded item images by item id. */
    internal val itemBitmaps: StateFlow<Map<String, Bitmap>> = mutableItemBitmaps.asStateFlow()

    // ── Getters (web parity) ─────────────────────────────────────────────

    /** Whether the loaded PDF is encrypted. */
    public val isPasswordProtected: Boolean get() = mutableState.value.isPasswordProtected

    /** Active page, 1-based. 1 when no document is loaded. */
    public val currentPage: Int get() = pagination.currentPage

    /** Number of pages, 0 when no document is loaded. */
    public val totalPages: Int get() = pagination.totalPages

    /** Current zoom. 1.0 draws one PDF point as one dp. */
    public val currentScale: Float get() = mutableState.value.scale

    /** Whether page navigation is locked with [setPaginationLocked]. */
    public val isPaginationLocked: Boolean get() = pagination.isLocked

    // ── Extra getters ────────────────────────────────────────────────────

    public val isLoaded: Boolean get() = engine != null
    public val isLoading: Boolean get() = mutableState.value.isLoading
    public var isDestroyed: Boolean = false
        private set

    init {
        config.file?.let { source ->
            scope.launch {
                try {
                    loadDocument(source)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e("Auto-load failed", e)
                }
            }
        }
    }

    // ── Document ─────────────────────────────────────────────────────────

    /**
     * Opens a PDF, replacing the current one. Resets to page 1 and removes all signatures and
     * E-Materai (`signaturesCleared` is sent before `documentLoaded`).
     *
     * @throws PsdkException when the document cannot be opened; the viewer then shows its empty state.
     */
    @MainThread
    public suspend fun loadDocument(source: PdfSource) {
        ensureAlive()
        val token = ++loadToken
        cancelPassword()
        closeEngine()
        mutableState.update {
            it.copy(isLoading = true, isLoaded = false, pageSizes = emptyList(), error = null, isPasswordProtected = false)
        }
        try {
            val file = resolver.resolve(source)
            val opened =
                PasswordFlow.open(
                    open = { password -> PdfRendererEngine.open(file, password) },
                    ask = { isIncorrect -> askPassword(token, isIncorrect) },
                )
            passwordPrompt.value = null
            if (token != loadToken || isDestroyed) {
                opened.close()
                return
            }
            engine = opened
            savedScroll = SavedScroll()
            val sizes = List(opened.pageCount) { opened.pageSize(it + 1) }
            pagination.setTotal(opened.pageCount)
            val initialScale = config.scale?.let(ZoomMath::clamp)
            fitWidthPending = initialScale == null
            mutableState.update {
                it.copy(
                    isLoading = false,
                    isLoaded = true,
                    currentPage = 1,
                    totalPages = opened.pageCount,
                    scale = initialScale ?: it.scale,
                    pageSizes = sizes,
                    documentId = it.documentId + 1,
                )
            }
            applyPendingFitWidth()
            clearItems()
            bus.emit(PsdkEvent.DocumentLoaded(opened.pageCount))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (token == loadToken) passwordPrompt.value = null
            if (token != loadToken) throw e
            val error =
                when (e) {
                    is PsdkException -> e
                    is PasswordRequiredException -> PsdkException.PasswordNotSupported()
                    else -> PsdkException.Corrupted(e)
                }
            resetToEmpty(
                error,
                passwordProtected = mutableState.value.isPasswordProtected || error is PsdkException.PasswordNotSupported,
            )
            throw error
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────

    /** Goes to the next page. Returns false when already on the last page or navigation is locked. */
    @MainThread
    public fun nextPage(): Boolean = !isDestroyed && pagination.nextPage()

    /** Goes to the previous page. Returns false when already on page 1 or navigation is locked. */
    @MainThread
    public fun prevPage(): Boolean = !isDestroyed && pagination.prevPage()

    /** Goes to [page] (1-based). Returns false when out of range, already there, or locked. */
    @MainThread
    public fun goToPage(page: Int): Boolean = !isDestroyed && pagination.goToPage(page)

    /** Locks or unlocks page navigation (buttons, page input, thumbnails and [goToPage]). */
    @MainThread
    public fun setPaginationLocked(locked: Boolean) {
        if (isDestroyed) return
        pagination.setLocked(locked)
        mutableState.update { it.copy(isPaginationLocked = locked) }
    }

    // ── Zoom ─────────────────────────────────────────────────────────────

    /** Sets the zoom (clamped to 0.25–4.0) and keeps the current page at the top. No event is sent. */
    @MainThread
    public suspend fun setScale(scale: Float) {
        if (isDestroyed) return
        applyScale(ZoomMath.clamp(scale), scrollToCurrentPage = true)
    }

    /** Zooms so the whole current page is visible and returns the new scale. */
    @MainThread
    public suspend fun fitToScreen(): Float {
        val size = currentPageSize() ?: return currentScale
        if (viewportWidth <= 0f) return currentScale
        val scale = ZoomMath.fitToScreen(viewportWidth, viewportHeight, size.width, size.height)
        setScale(scale)
        return scale
    }

    /** Zooms so the widest page fills the screen width and returns the new scale. */
    @MainThread
    public suspend fun fitToWidth(): Float {
        val scale = fitToWidthScale() ?: return currentScale
        setScale(scale)
        return scale
    }

    // ── Pages ────────────────────────────────────────────────────────────

    /** Size of [page] in PDF points, after its `/Rotate`. */
    public fun getPageSize(page: Int): PageSize {
        val sizes = mutableState.value.pageSizes
        if (sizes.isEmpty()) throw PsdkException.NotLoaded()
        if (page !in 1..sizes.size) throw PsdkException.InvalidPage(page)
        return sizes[page - 1]
    }

    /**
     * Renders [page] at [scale] (bitmap size = points × scale × screen density, capped at 4096 px).
     * Replaces the web SDK's `getCanvas(page)`.
     */
    public suspend fun renderPage(
        page: Int = currentPage,
        scale: Float = currentScale,
    ): Bitmap {
        ensureAlive()
        val size = getPageSize(page)
        val current = engine ?: throw PsdkException.NotLoaded()
        val pixels = RenderSize.compute(size.width, size.height, scale, appContext.resources.displayMetrics.density)
        return current.render(page, pixels.width, pixels.height)
    }

    // ── Signatures & E-Materai ──────────────────────────────────────────

    /**
     * Places a signature and returns its id (B-OVL-03…07). Without x/y it is centered on the page;
     * without a page it goes on the current page. Sends `signaturePlaced`.
     *
     * @throws PsdkException.NotLoaded when no document is open.
     * @throws PsdkException.InvalidPage when [PlaceOptions.page] does not exist.
     */
    @MainThread
    public fun placeSignature(options: PlaceOptions = PlaceOptions()): String = place(ItemType.SIGNATURE, options)

    /** Places an E-Materai and returns its id. Its label is always "E-Materai". Sends `eStampPlaced`. */
    @MainThread
    public fun placeEStamp(options: PlaceOptions = PlaceOptions()): String = place(ItemType.ESTAMP, options)

    /** Removes one signature or E-Materai and sends `signatureRemoved`. Unknown ids are ignored. */
    @MainThread
    public fun removeSignature(id: String) {
        if (isDestroyed) return
        if (store.remove(id)) mutableItemBitmaps.update { it - id }
    }

    /** Removes every signature and E-Materai on every page. Always sends `signaturesCleared`. */
    @MainThread
    public fun clearSignatures() {
        if (!isDestroyed) clearItems()
    }

    /** All placed items, in the order they were placed. Positions in PDF points. */
    public fun getSignatures(): List<SignatureItem> = store.all()

    /** Items on [page]. */
    public fun getSignaturesByPage(page: Int = currentPage): List<SignatureItem> = store.byPage(page)

    /**
     * Renders [page] with its signatures and E-Materai drawn on top (without handles).
     * Replaces the web SDK's `getOverlayCanvas(page)`.
     */
    public suspend fun renderPageWithOverlay(
        page: Int = currentPage,
        scale: Float = currentScale,
    ): Bitmap {
        val bitmap = renderPage(page, scale)
        val pxPerPoint = (bitmap.width / getPageSize(page).width).toFloat()
        StaticOverlayPainter.draw(android.graphics.Canvas(bitmap), store.byPage(page), mutableItemBitmaps.value, pxPerPoint)
        return bitmap
    }

    // ── Pickers, skeleton, config ───────────────────────────────────────

    /**
     * Opens the signature picker (web: `openSignatureModal`). Without `signatureOptions` it places a
     * default signature right away (B-MOD-01). Does nothing when no document is loaded.
     */
    @MainThread
    public fun openSignatureModal() {
        openPicker(ItemType.SIGNATURE)
    }

    /** Opens the E-Materai picker, or places a default E-Materai when `estampOptions` is empty. */
    @MainThread
    public fun openEStampModal() {
        openPicker(ItemType.ESTAMP)
    }

    /**
     * Shows or hides a loading overlay over the viewer, e.g. while your backend signs the document.
     * Pass [options] to change the background, blur or content.
     */
    @MainThread
    public fun showSkeleton(
        visible: Boolean,
        options: SkeletonOptions? = null,
    ) {
        if (isDestroyed) return
        skeleton.value = if (visible) options ?: SkeletonOptions() else null
    }

    /**
     * Changes the configuration at runtime (web: `updateConfig`), e.g.
     * `viewer.updateConfig { it.copy(disabled = true, theme = PsdkThemeMode.DARK) }`.
     * Toolbar, labels, theme, locks and pickers update immediately; a new `scale` zooms (B-CFG-02).
     */
    @MainThread
    public fun updateConfig(transform: (PsdkConfig) -> PsdkConfig) {
        if (isDestroyed) return
        val old = configState.value
        val new = transform(old)
        configState.value = new
        logger.level = new.logLevel
        val newScale = new.scale
        if (newScale != null && newScale != old.scale) applyScale(ZoomMath.clamp(newScale), scrollToCurrentPage = true)
    }

    // ── Events ───────────────────────────────────────────────────────────

    /** Listens to events of [type] (web: `on(event, fn)`). Cancel the returned subscription to stop (web: `off`). */
    public fun <E : PsdkEvent> on(
        type: Class<E>,
        listener: PsdkListener<E>,
    ): PsdkSubscription = bus.on(type, listener)

    /** Kotlin shortcut: `viewer.on<PsdkEvent.PageChanged> { … }`. */
    public inline fun <reified E : PsdkEvent> on(noinline listener: (E) -> Unit): PsdkSubscription =
        on(E::class.java, PsdkListener { listener(it) })

    /** Like [on], but removed after the first event. */
    public fun <E : PsdkEvent> once(
        type: Class<E>,
        listener: PsdkListener<E>,
    ): PsdkSubscription = bus.on(type, listener, once = true)

    /** Stops a listener (web: `off`). Same as `subscription.cancel()`. */
    public fun off(subscription: PsdkSubscription) {
        subscription.cancel()
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    /** Closes the document, removes all listeners and deletes temporary files. The viewer cannot be used afterwards. */
    @MainThread
    public fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        loadToken++
        scope.cancel()
        closeEngine()
        temp.deleteAll()
        pagination.reset()
        bus.clear()
        store.clear()
        mutableItemBitmaps.value = emptyMap()
        picker.value = null
        skeleton.value = null
        cancelPassword()
        passwordPrompt.value = null
        mutableState.value = PsdkState()
    }

    // ── Internal API for the Compose UI ─────────────────────────────────

    internal val labels get() = config.labels

    internal val locks: GlobalLocks
        get() = GlobalLocks(config.disabled, config.disableDragging, config.disableDrag, config.disableResize)

    /** Commits a drag or resize from the overlay and sends `signatureMoved`. */
    internal fun commitItemBox(
        id: String,
        box: ItemBox,
    ) {
        if (!isDestroyed) store.move(id, box)
    }

    /**
     * Last scroll position of the page list. Kept here because the viewer outlives the Activity,
     * so the list returns to the same place after rotation.
     */
    internal var savedScroll: SavedScroll = SavedScroll()

    internal fun onViewportChanged(
        width: Float,
        height: Float,
    ) {
        viewportWidth = width
        viewportHeight = height
        applyPendingFitWidth()
    }

    /** Called by the page list when another page becomes the most visible one (B-PG-04). */
    internal fun onScrolledToPage(page: Int) {
        if (!isDestroyed) pagination.goToPage(page, PsdkEvent.PageChanged.Source.SCROLL)
    }

    /** Applies a pinch or double-tap zoom. The UI keeps the focal point in place itself. */
    internal fun applyGestureScale(scale: Float) {
        if (isDestroyed) return
        applyScale(ZoomMath.clamp(scale), scrollToCurrentPage = false)
    }

    internal fun fitToWidthScale(): Float? {
        val sizes = mutableState.value.pageSizes
        if (sizes.isEmpty() || viewportWidth <= 0f) return null
        return ZoomMath.fitToWidth(viewportWidth, sizes.maxOf { it.width }, PAGE_HORIZONTAL_PADDING_DP)
    }

    /** Renders a page for the screen, reusing cached bitmaps. Returns null if the document was closed meanwhile. */
    internal suspend fun renderForDisplay(
        page: Int,
        width: Int,
        height: Int,
    ): Bitmap? {
        val current = engine ?: return null
        val key = "${mutableState.value.documentId}:$page:${width}x$height"
        cache.get(key)?.let { return it }
        return try {
            current.render(page, width, height).also { cache.put(key, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("Rendering page $page failed", e)
            null
        }
    }

    internal fun closePicker() {
        picker.value = null
    }

    /** A picker option was chosen: close the picker and place it on the current page (B-MOD-05). */
    internal fun choose(
        type: ItemType,
        option: PickerOption,
    ) {
        closePicker()
        if (isDestroyed || !isLoaded) return
        place(type, PlaceOptions(label = option.label, image = option.image))
    }

    /** Upload button flow (B-UPL-02/03): ask `onUpload`, then open the file. Errors are only logged. */
    internal suspend fun handleUpload(picked: PdfSource) {
        try {
            val decision = config.onUpload?.invoke(picked) ?: UploadDecision.Proceed
            when (decision) {
                UploadDecision.Cancel -> return
                UploadDecision.Proceed -> loadDocument(picked)
                is UploadDecision.Replace -> loadDocument(decision.source)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("Upload failed", e)
        }
    }

    /** Built-in dialog: the user entered a password (B-PWD-02). */
    internal fun submitPassword(password: String) {
        val pending = pendingPassword ?: return
        passwordPrompt.update { it?.copy(submitting = true) }
        pending.complete(password)
    }

    /** Built-in dialog or a new load: stop waiting for a password (B-PWD-04). */
    internal fun cancelPassword() {
        pendingPassword?.complete(null)
    }

    /** Runs the upload flow in the viewer's scope, so it survives the UI being recreated. */
    internal fun startUpload(picked: PdfSource) {
        if (!isDestroyed) scope.launch { handleUpload(picked) }
    }

    internal suspend fun loadImage(image: io.github.fvory.psdk.model.PsdkImage): Bitmap? = imageLoader.load(image)

    /** Theme button: switch between light and dark (B-TB-06). No event. */
    internal fun toggleTheme(currentlyDark: Boolean) {
        updateConfig { it.copy(theme = if (currentlyDark) PsdkThemeMode.LIGHT else PsdkThemeMode.DARK) }
    }

    // ── Private ──────────────────────────────────────────────────────────

    private fun onPageChanged(
        page: Int,
        total: Int,
        source: PsdkEvent.PageChanged.Source,
    ) {
        mutableState.update { it.copy(currentPage = page) }
        if (source == PsdkEvent.PageChanged.Source.API) {
            mutableCommands.tryEmit(ViewerCommand.ScrollToPage(page, animate = true, settleMillis = 150))
        }
        bus.emit(PsdkEvent.PageChanged(page, total, source))
    }

    private fun applyScale(
        scale: Float,
        scrollToCurrentPage: Boolean,
    ) {
        fitWidthPending = false
        mutableState.update { it.copy(scale = scale) }
        if (scrollToCurrentPage && isLoaded) {
            mutableCommands.tryEmit(ViewerCommand.ScrollToPage(currentPage, animate = false, settleMillis = 850))
        }
    }

    private fun applyPendingFitWidth() {
        if (!fitWidthPending) return
        val scale = fitToWidthScale() ?: return
        fitWidthPending = false
        mutableState.update { it.copy(scale = scale) }
    }

    /**
     * Asks for a password through the `passwordRequested` event and, unless turned off, the built-in
     * dialog (B-LOAD-04). Returns null when cancelled or when a newer load replaced this one.
     */
    private suspend fun askPassword(
        token: Long,
        isIncorrect: Boolean,
    ): String? {
        if (token != loadToken || isDestroyed) return null
        mutableState.update { it.copy(isPasswordProtected = true) }
        val answer = CompletableDeferred<String?>()
        pendingPassword = answer
        passwordPrompt.value = if (config.ui.passwordDialog) PasswordPrompt(isIncorrect) else null
        bus.emit(
            PsdkEvent.PasswordRequested(
                isIncorrect = isIncorrect,
                submit = { if (pendingPassword === answer) submitPassword(it) },
                dismiss = { if (pendingPassword === answer) answer.complete(null) },
            ),
        )
        return try {
            answer.await()
        } finally {
            if (pendingPassword === answer) pendingPassword = null
        }
    }

    private fun openPicker(type: ItemType) {
        if (isDestroyed || !isLoaded) return
        val options = if (type == ItemType.SIGNATURE) config.signatureOptions else config.estampOptions
        if (options.isEmpty()) {
            place(type, PlaceOptions())
        } else {
            picker.value = type
        }
    }

    private fun place(
        type: ItemType,
        options: PlaceOptions,
    ): String {
        ensureAlive()
        val sizes = mutableState.value.pageSizes
        if (!isLoaded || sizes.isEmpty()) throw PsdkException.NotLoaded()
        val page = options.page ?: currentPage
        if (page !in 1..sizes.size) throw PsdkException.InvalidPage(page)
        val item = store.place(type, options, page, sizes[page - 1])
        options.image?.let { loadItemImage(item.id, it) }
        return item.id
    }

    private fun loadItemImage(
        id: String,
        image: io.github.fvory.psdk.model.PsdkImage,
    ) {
        scope.launch {
            val bitmap = imageLoader.load(image)
            if (bitmap == null) {
                logger.e("Could not load the image of item $id")
                return@launch
            }
            if (store.get(id) == null) return@launch
            mutableItemBitmaps.update { it + (id to bitmap) }
            store.onImageLoaded(id, bitmap.width, bitmap.height)
        }
    }

    private fun clearItems() {
        store.clear()
        mutableItemBitmaps.value = emptyMap()
    }

    private fun currentPageSize(): PageSize? = mutableState.value.pageSizes.getOrNull(currentPage - 1)

    private fun closeEngine() {
        engine?.close()
        engine = null
        cache.clear()
    }

    private fun resetToEmpty(
        error: Throwable,
        passwordProtected: Boolean,
    ) {
        closeEngine()
        pagination.setTotal(0)
        clearItems()
        mutableState.update {
            it.copy(
                isLoading = false,
                isLoaded = false,
                currentPage = 1,
                totalPages = 0,
                pageSizes = emptyList(),
                isPasswordProtected = passwordProtected,
                error = error,
            )
        }
    }

    private fun ensureAlive() {
        if (isDestroyed) throw PsdkException.Destroyed()
    }

    /** What the built-in password dialog shows. */
    internal data class PasswordPrompt(
        val isIncorrect: Boolean,
        val submitting: Boolean = false,
    )

    internal data class SavedScroll(
        val index: Int = 0,
        val offset: Int = 0,
        val horizontal: Int = 0,
    )

    internal companion object {
        /** Space on the left and right of the pages, in dp. */
        const val PAGE_HORIZONTAL_PADDING_DP: Float = 16f
    }
}
