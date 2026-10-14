package io.github.fvory.psdk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureSdk
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.model.toJsonString
import io.github.fvory.psdk.rememberPdfSignatureViewer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { SampleScreen() }
            }
        }
    }
}

/**
 * Temporary controls for milestones M1–M2. The SDK's own toolbar replaces them in M3.
 */
@Composable
private fun SampleScreen() {
    val viewer =
        rememberPdfSignatureViewer(
            PsdkConfig(
                file = PdfSource.AssetSource("sample.pdf"),
                lockScrollWhenPaginationLocked = true,
                labels = PsdkLabels.indonesian(),
            ),
        )
    val state by viewer.state.collectAsState()
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    var showJson by remember { mutableStateOf(false) }

    DisposableEffect(viewer) {
        val subscription =
            viewer.on<PsdkEvent> { event ->
                log.add(0, describe(event))
                if (log.size > 6) log.removeAt(log.lastIndex)
            }
        onDispose { subscription.cancel() }
    }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) scope.launch { loadSafely(viewer, PdfSource.UriSource(uri), log) }
        }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Buka PDF") }
            OutlinedButton(onClick = {
                scope.launch { loadSafely(viewer, PdfSource.AssetSource("sample.pdf"), log) }
            }) { Text("Contoh") }
            OutlinedButton(onClick = { viewer.prevPage() }) { Text("‹") }
            Text("${state.currentPage} / ${if (state.totalPages == 0) "—" else state.totalPages}")
            OutlinedButton(onClick = { viewer.nextPage() }) { Text("›") }
            OutlinedButton(onClick = { scope.launch { viewer.setScale(zoomStep(-1, viewer.currentScale)) } }) {
                Text("−")
            }
            Text("${(state.scale * 100).roundToInt()}%")
            OutlinedButton(onClick = { scope.launch { viewer.setScale(zoomStep(1, viewer.currentScale)) } }) {
                Text("+")
            }
            OutlinedButton(onClick = { scope.launch { viewer.fitToScreen() } }) { Text("Fit") }
            OutlinedButton(onClick = { scope.launch { viewer.fitToWidth() } }) { Text("Lebar") }
            OutlinedButton(onClick = { viewer.setPaginationLocked(!state.isPaginationLocked) }) {
                Text(if (state.isPaginationLocked) "Buka kunci" else "Kunci halaman")
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fun place(block: () -> Unit) {
                try {
                    block()
                } catch (e: Exception) {
                    log.add(0, "Error: ${e.message}")
                }
            }
            FilledTonalButton(onClick = { place { viewer.placeSignature(PlaceOptions(label = "Direktur")) } }) { Text("+ TTD") }
            FilledTonalButton(onClick = {
                place { viewer.placeSignature(PlaceOptions(label = "Klien", image = PsdkImage.BitmapImage(SampleImages.signature))) }
            }) { Text("+ TTD gambar") }
            FilledTonalButton(onClick = { place { viewer.placeEStamp() } }) { Text("+ Materai") }
            FilledTonalButton(onClick = {
                place { viewer.placeEStamp(PlaceOptions(image = PsdkImage.BitmapImage(SampleImages.stamp))) }
            }) { Text("+ Materai gambar") }
            OutlinedButton(onClick = {
                place { viewer.placeSignature(PlaceOptions(x = 40.0, y = 60.0, label = "Terkunci", locked = true)) }
            }) { Text("+ TTD terkunci") }
            OutlinedButton(onClick = { viewer.clearSignatures() }) { Text("Hapus semua") }
            OutlinedButton(onClick = { showJson = !showJson }) { Text(if (showJson) "Log" else "JSON") }
            OutlinedButton(onClick = {
                scope.launch {
                    showJson = false
                    val failures =
                        try {
                            ParityCheck.run(context, viewer)
                        } catch (e: Exception) {
                            listOf("Error: ${e.message}")
                        }
                    log.clear()
                    log +=
                        if (failures.isEmpty()) {
                            listOf("Paritas web: SEMUA LULUS")
                        } else {
                            listOf("Paritas web: ${failures.size} GAGAL") +
                                failures
                        }
                }
            }) { Text("Cek paritas") }
        }
        PdfSignatureViewer(viewer, Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            if (showJson) {
                Text("getSignatures() → ${state.signatures.size} item", fontSize = 11.sp)
                Text(state.signatures.toJsonString(), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            } else {
                Text("SDK ${PdfSignatureSdk.version} · event terakhir:", fontSize = 11.sp)
                log.forEach { Text(it, fontSize = 11.sp, maxLines = 1) }
            }
        }
    }
}

private suspend fun loadSafely(
    viewer: PdfSignatureViewer,
    source: PdfSource,
    log: MutableList<String>,
) {
    try {
        viewer.loadDocument(source)
    } catch (e: Exception) {
        log.add(0, "Error: ${e.message}")
    }
}

/** Zoom buttons step by 0.25 like the web SDK toolbar. */
private fun zoomStep(
    direction: Int,
    scale: Float,
): Float = ((scale + direction * 0.25f) * 100).roundToInt() / 100f

private fun describe(event: PsdkEvent): String =
    when (event) {
        is PsdkEvent.DocumentLoaded -> "documentLoaded total=${event.totalPages}"
        is PsdkEvent.PageChanged -> "pageChanged ${event.page}/${event.total} (${event.source.name.lowercase()})"
        is PsdkEvent.SignaturePlaced -> "signaturePlaced ${event.item.short()}"
        is PsdkEvent.EStampPlaced -> "eStampPlaced ${event.item.short()}"
        is PsdkEvent.SignatureMoved -> "signatureMoved ${event.item.short()}"
        is PsdkEvent.SignatureRemoved -> "signatureRemoved ${event.id}"
        PsdkEvent.SignaturesCleared -> "signaturesCleared"
    }

private fun SignatureItem.short(): String = "$label p$page x=${x.f()} y=${y.f()} w=${width.f()} h=${height.f()}"

private fun Double.f(): String = "%.1f".format(this)
