package io.github.fvory.psdk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkUiConfig
import io.github.fvory.psdk.config.TopBarConfig

/** Debug-only host for a screenshot of the real SDK, with no file picker or demo metadata state. */
class NativeGeometryActivity : ComponentActivity() {
    lateinit var viewer: PdfSignatureViewer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewer =
            PdfSignatureViewer(
                this,
                PsdkConfig(
                    labels = PsdkLabels.indonesian(),
                    ui =
                        PsdkUiConfig(
                            topbar =
                                TopBarConfig(
                                    upload = false,
                                    signature = false,
                                    eStamp = false,
                                    brandText = "Native SDK regression",
                                ),
                        ),
                ),
            )
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val state by viewer.state.collectAsState()
                    Column {
                        Text("Native page points: ${state.pageSizes.firstOrNull()}")
                        PdfSignatureViewer(viewer, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        viewer.destroy()
        super.onDestroy()
    }
}
