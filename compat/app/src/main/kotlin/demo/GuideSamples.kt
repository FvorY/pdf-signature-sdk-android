@file:Suppress("unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER")

package demo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.fvory.psdk.PdfSignatureView
import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkColors
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkLogLevel
import io.github.fvory.psdk.config.PsdkThemeMode
import io.github.fvory.psdk.config.PsdkUiConfig
import io.github.fvory.psdk.config.SidebarConfig
import io.github.fvory.psdk.config.SkeletonOptions
import io.github.fvory.psdk.config.TopBarConfig
import io.github.fvory.psdk.config.UploadDecision
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.model.toJsonArray
import io.github.fvory.psdk.model.toJsonString
import io.github.fvory.psdk.rememberPdfSignatureViewer
import org.json.JSONArray
import java.io.File

/**
 * The code samples of DOCUMENTATION.en.md / .id.md and README.md, compiled with the app. If this stops
 * compiling, the guides are wrong: update them too. Values that stand for the reader's own data
 * (`uri`, `file`, `bytes`, `context`, resources) are parameters.
 */
@Composable
fun GuideSamples(
    uri: Uri,
    file: File,
    bytes: ByteArray,
    context: Context,
    drawable: Int,
) {
    // §2
    val viewer = rememberPdfSignatureViewer(PsdkConfig(file = PdfSource.UriSource(uri)))
    LaunchedEffect(uri) { viewer.loadDocument(PdfSource.UriSource(uri)) }

    // §3
    PdfSource.FileSource(file)
    PdfSource.UriSource(uri)
    PdfSource.UrlSource("https://example.com/a.pdf", headers = mapOf("Authorization" to "Bearer …"))
    PdfSource.BytesSource(bytes)
    PdfSource.AssetSource("documents/example.pdf")
    PsdkConfig(allowInsecureHttp = true)

    // §4
    val signatureId = viewer.placeSignature(
        PlaceOptions(
            page = 1, x = 72.0, y = 680.0, label = "Director",
            image = PsdkImage.Url("https://example.com/signature.png"), width = 140.0,
        ),
    )
    val stampId = viewer.placeEStamp(PlaceOptions(page = 1, x = 420.0, y = 700.0))

    // §6
    val items: List<SignatureItem> = viewer.getSignatures()
    val onPage2 = viewer.getSignaturesByPage(2)
    val json: String = items.toJsonString()
    val array: JSONArray = items.toJsonArray()
    viewer.removeSignature(signatureId)
    viewer.clearSignatures()

    // §7
    val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
        Log.d("psdk", event.item.toJsonString()) // send this to your backend
    }
    viewer.once<PsdkEvent.DocumentLoaded> { println("${it.totalPages} pages") }
    subscription.cancel() // or viewer.off(subscription)

    // §8
    viewer.nextPage(); viewer.prevPage(); viewer.goToPage(3)
    viewer.setPaginationLocked(true)
    LaunchedEffect(Unit) {
        viewer.setScale(1.5f)
        viewer.fitToScreen(); viewer.fitToWidth()
    }

    // §9
    PsdkConfig(ui = PsdkUiConfig(passwordDialog = false))
    viewer.on<PsdkEvent.PasswordRequested> { event ->
        event.updatePassword("test123") // the text the user typed
        // event.cancel()               // or the user gave up
    }

    // §10
    PsdkConfig(
        ui = PsdkUiConfig(
            topbar = TopBarConfig(upload = false, themeToggle = false, brandText = "My App"),
            sidebar = SidebarConfig(thumbnails = false),
            statusbar = false,
        ),
    )
    PsdkConfig(
        signatureOptions = listOf(
            PickerOption("director", "Director", PsdkImage.Resource(drawable), group = "Internal"),
            PickerOption("client", "Client", group = "External"),
        ),
        estampOptions = listOf(PickerOption("stamp-10000", "E-Materai 10,000")),
        groupByCategory = true,
    )

    // §11
    PsdkConfig(
        theme = PsdkThemeMode.SYSTEM,
        lightColors = PsdkColors.light().copy(accent = Color(0xFF0057B8)),
        darkColors = PsdkColors.dark(),
        labels = PsdkLabels.indonesian().copy(uploadBtn = "Pilih PDF"),
    )

    // §12
    PsdkConfig(
        onUpload = { picked ->
            val pickedUri = (picked as? PdfSource.UriSource)?.uri
            val size = pickedUri?.let { context.contentResolver.openAssetFileDescriptor(it, "r")?.use { d -> d.length } }
            if (size != null && size in 0..20_000_000) UploadDecision.Proceed else UploadDecision.Cancel
            // or open another document instead: UploadDecision.Replace(PdfSource.AssetSource("other.pdf"))
        },
    )

    // §13, §14
    viewer.showSkeleton(true, SkeletonOptions(backgroundOpacity = 0.3f, blurRadius = 8.dp))
    viewer.showSkeleton(false)
    LaunchedEffect(Unit) {
        val page: Bitmap = viewer.renderPage(page = 1, scale = 2f)
        val withItems: Bitmap = viewer.renderPageWithOverlay(page = 1, scale = 2f)
    }

    // §15, §18
    PsdkConfig(secureScreen = true, logLevel = PsdkLogLevel.DEBUG, scale = 1.25f)
    viewer.updateConfig { it.copy(disabled = true) }
}

/** §2 XML / View */
fun xmlSample(
    pdfView: PdfSignatureView,
    uri: Uri,
) {
    pdfView.setConfig(PsdkConfig(file = PdfSource.UriSource(uri)))
    val viewer = pdfView.viewer
}
