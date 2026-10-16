package demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkThemeMode
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.toJsonArray
import io.github.fvory.psdk.model.toJsonString
import io.github.fvory.psdk.rememberPdfSignatureViewer
import kotlinx.coroutines.launch

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Screen() }
    }
}

@Composable
fun Screen() {
    val viewer = rememberPdfSignatureViewer(
        PsdkConfig(
            file = PdfSource.AssetSource("doc.pdf"),
            theme = PsdkThemeMode.SYSTEM,
            labels = PsdkLabels.indonesian(),
            secureScreen = true,
            signatureOptions = listOf(PickerOption("dir", "Direktur", PsdkImage.of("https://example.com/s.png"))),
        ),
    )
    val state by viewer.state.collectAsState()
    val scope = rememberCoroutineScope()
    DisposableEffect(viewer) {
        val sub = viewer.on<PsdkEvent.SignaturePlaced> { Log.d("consumer", it.item.toJsonString()) }
        val sub2 = viewer.on<PsdkEvent.PasswordRequested> { it.updatePassword("x") }
        onDispose { sub.cancel(); sub2.cancel() }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Page ${state.currentPage}/${state.totalPages}")
        Button(onClick = {
            viewer.placeSignature(PlaceOptions(page = 1, x = 72.0, y = 600.0, label = "Direktur"))
            scope.launch { viewer.events.collect { } }
            viewer.updateConfig { it.copy(disabled = false) }
            Log.d("consumer", viewer.getSignatures().toJsonArray().toString())
        }) { Text("Place") }
        PdfSignatureViewer(viewer, Modifier.weight(1f))
    }
}
