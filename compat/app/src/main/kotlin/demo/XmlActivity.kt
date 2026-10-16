package demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import demo.compat.R
import io.github.fvory.psdk.PdfSignatureView
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PdfSource

class XmlActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml)
        val view = findViewById<PdfSignatureView>(R.id.pdf)
        view.setConfig(PsdkConfig(file = PdfSource.AssetSource("doc.pdf")))
        view.viewer.on<PsdkEvent.DocumentLoaded> { }
    }
}
