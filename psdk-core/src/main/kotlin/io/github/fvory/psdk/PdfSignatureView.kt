package io.github.fvory.psdk

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.github.fvory.psdk.config.PsdkConfig

/**
 * The viewer as a classic Android View, for XML layouts and Activities without Compose.
 *
 * ```xml
 * <io.github.fvory.psdk.PdfSignatureView android:id="@+id/psdk"
 *     android:layout_width="match_parent" android:layout_height="match_parent" />
 * ```
 * ```kotlin
 * psdk.setConfig(PsdkConfig(file = PdfSource.FileSource(file)))
 * psdk.viewer.on<PsdkEvent.SignatureMoved> { … }
 * ```
 * The host must be a `ComponentActivity` (AppCompatActivity, FragmentActivity) for the upload
 * button. The viewer is destroyed with the hosting lifecycle; unlike `rememberPdfSignatureViewer`,
 * a rotation recreates it, so keep item positions with `getSignatures()` if you need them.
 */
public class PdfSignatureView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private var current: PdfSignatureViewer? = null
        private var observedOwner: LifecycleOwner? = null

        private val destroyObserver =
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    current?.destroy()
                }
            }

        /** The viewer shown by this view. Created with a default config on first access. */
        public val viewer: PdfSignatureViewer
            get() = current ?: attach(PdfSignatureViewer(context))

        /** Replaces the viewer with a new one using [config]. Use `viewer.updateConfig` for later changes. */
        public fun setConfig(config: PsdkConfig) {
            current?.destroy()
            attach(PdfSignatureViewer(context, config))
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            val owner = findViewTreeLifecycleOwner()
            if (owner != observedOwner) {
                observedOwner?.lifecycle?.removeObserver(destroyObserver)
                owner?.lifecycle?.addObserver(destroyObserver)
                observedOwner = owner
            }
        }

        private fun attach(viewer: PdfSignatureViewer): PdfSignatureViewer {
            current = viewer
            removeAllViews()
            addView(
                ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent { PdfSignatureViewer(viewer) }
                },
            )
            return viewer
        }
    }
