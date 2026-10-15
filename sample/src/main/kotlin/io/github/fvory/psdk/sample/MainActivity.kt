package io.github.fvory.psdk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureSdk
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkLayout
import io.github.fvory.psdk.config.PsdkThemeMode
import io.github.fvory.psdk.config.PsdkUiConfig
import io.github.fvory.psdk.config.TopBarConfig
import io.github.fvory.psdk.config.UploadDecision
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.model.toJsonString
import io.github.fvory.psdk.rememberPdfSignatureViewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private val signatureOptions =
    listOf(
        PickerOption("direktur", "Direktur", PsdkImage.BitmapImage(SampleImages.signature), group = "Internal"),
        PickerOption("manajer", "Manajer", group = "Internal"),
        PickerOption("klien", "Klien", group = "Eksternal"),
        PickerOption("saksi", "Saksi"),
    )

private val estampOptions =
    listOf(
        PickerOption("materai-10000", "Materai 10.000", PsdkImage.BitmapImage(SampleImages.stamp)),
        PickerOption("materai-polos", "Tanpa gambar"),
    )

/** The SDK's own toolbar does the work; the "Demo" panel exercises the API and the parity check. */
@Composable
private fun SampleScreen() {
    val viewer =
        rememberPdfSignatureViewer(
            PsdkConfig(
                file = PdfSource.AssetSource("sample.pdf"),
                theme = PsdkThemeMode.SYSTEM,
                signatureOptions = signatureOptions,
                estampOptions = estampOptions,
                groupByCategory = true,
                lockScrollWhenPaginationLocked = true,
                labels = PsdkLabels.indonesian(),
                ui = PsdkUiConfig(topbar = TopBarConfig(brandText = "PSDK Sample")),
                onUpload = { UploadDecision.Proceed },
            ),
        )
    val state by viewer.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    var panel by remember { mutableStateOf(false) }
    var showJson by remember { mutableStateOf(false) }

    DisposableEffect(viewer) {
        val subscription =
            viewer.on<PsdkEvent> { event ->
                log.add(0, describe(event))
                if (log.size > 6) log.removeAt(log.lastIndex)
            }
        onDispose { subscription.cancel() }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PdfSignatureViewer(viewer, Modifier.weight(1f))
        TextButton(onClick = { panel = !panel }, modifier = Modifier.fillMaxWidth()) {
            Text(if (panel) "Tutup demo ▾" else "Demo & log ▴  (SDK ${PdfSignatureSdk.version})")
        }
        if (!panel) return@Column
        val config = viewer.config
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = false,
                onClick = {
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
                                listOf("Paritas web: ${failures.size} GAGAL") + failures
                            }
                    }
                },
                label = { Text("Cek paritas") },
            )
            FilterChip(
                selected = false,
                onClick = {
                    scope.launch {
                        showJson = false
                        log.clear()
                        log +=
                            try {
                                listOf("Cek Pdfium (geometry.pdf):") + PdfiumCheck.run(context)
                            } catch (e: Exception) {
                                listOf("Pdfium error: ${e.javaClass.simpleName}: ${e.message}")
                            }
                    }
                },
                label = { Text("Cek Pdfium") },
            )
            // Debug builds ship a 300-page PDF for memory and scrolling tests
            if (context.assets
                    .list("")
                    .orEmpty()
                    .contains("big.pdf")
            ) {
                FilterChip(
                    selected = false,
                    onClick = { scope.launch { loadSafely(viewer, PdfSource.AssetSource("big.pdf"), log) } },
                    label = { Text("PDF 300 hlm") },
                )
            }
            FilterChip(selected = showJson, onClick = { showJson = !showJson }, label = { Text("JSON") })
            for ((asset, title) in listOf("password.pdf" to "PDF password AES", "password-rc4.pdf" to "PDF password RC4")) {
                FilterChip(
                    selected = false,
                    onClick = {
                        scope.launch {
                            try {
                                viewer.loadDocument(PdfSource.AssetSource(asset))
                            } catch (e: Exception) {
                                log.add(0, "Error: ${e.javaClass.simpleName}: ${e.message}")
                            }
                        }
                    },
                    label = { Text(title) },
                )
            }
            FilterChip(
                selected = config.disabled,
                onClick = { viewer.updateConfig { it.copy(disabled = !it.disabled) } },
                label = { Text("Nonaktif") },
            )
            FilterChip(
                selected = config.disableDragging,
                onClick = { viewer.updateConfig { it.copy(disableDragging = !it.disableDragging) } },
                label = { Text("Kunci item") },
            )
            FilterChip(
                selected = state.isPaginationLocked,
                onClick = { viewer.setPaginationLocked(!state.isPaginationLocked) },
                label = { Text("Kunci halaman") },
            )
            FilterChip(
                selected = false,
                onClick = {
                    try {
                        viewer.placeSignature(PlaceOptions(x = 40.0, y = 60.0, label = "Terkunci", locked = true))
                    } catch (e: Exception) {
                        log.add(0, "Error: ${e.message}")
                    }
                },
                label = { Text("+ TTD terkunci") },
            )
            FilterChip(
                selected = false,
                onClick = {
                    scope.launch {
                        viewer.showSkeleton(true)
                        delay(2_000)
                        viewer.showSkeleton(false)
                    }
                },
                label = { Text("Skeleton 2 dtk") },
            )
            FilterChip(
                selected = config.labels.uploadBtn == PsdkLabels().uploadBtn,
                onClick = {
                    viewer.updateConfig {
                        it.copy(labels = if (it.labels.uploadBtn == PsdkLabels().uploadBtn) PsdkLabels.indonesian() else PsdkLabels())
                    }
                },
                label = { Text("English") },
            )
            FilterChip(
                selected = config.ui.layout != PsdkLayout.AUTO,
                onClick = {
                    viewer.updateConfig {
                        val next =
                            when (it.ui.layout) {
                                PsdkLayout.AUTO -> PsdkLayout.TABLET
                                PsdkLayout.TABLET -> PsdkLayout.PHONE
                                PsdkLayout.PHONE -> PsdkLayout.AUTO
                            }
                        it.copy(ui = it.ui.copy(layout = next))
                    }
                },
                label = { Text("Layout: ${config.ui.layout.name.lowercase()}") },
            )
        }
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
                log.forEach { Text(it, fontSize = 11.sp, maxLines = 1) }
            }
        }
    }
}

private fun describe(event: PsdkEvent): String =
    when (event) {
        is PsdkEvent.DocumentLoaded -> "documentLoaded total=${event.totalPages}"
        is PsdkEvent.PageChanged -> "pageChanged ${event.page}/${event.total} (${event.source.name.lowercase()})"
        is PsdkEvent.SignaturePlaced -> "signaturePlaced ${event.item.short()}"
        is PsdkEvent.EStampPlaced -> "eStampPlaced ${event.item.short()}"
        is PsdkEvent.SignatureMoved -> "signatureMoved ${event.item.short()}"
        is PsdkEvent.SignatureRemoved -> "signatureRemoved ${event.id}"
        PsdkEvent.SignaturesCleared -> "signaturesCleared"
        is PsdkEvent.PasswordRequested -> "passwordRequested isIncorrect=${event.isIncorrect}"
    }

private fun SignatureItem.short(): String = "$label p$page x=${x.f()} y=${y.f()} w=${width.f()} h=${height.f()}"

private fun Double.f(): String = "%.1f".format(this)

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
