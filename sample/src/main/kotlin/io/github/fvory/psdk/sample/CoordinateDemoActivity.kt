package io.github.fvory.psdk.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkUiConfig
import io.github.fvory.psdk.config.TopBarConfig
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.toInitSignData
import io.github.fvory.psdk.rememberPdfSignatureViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoordinateDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { CoordinateDemo() } } }
    }
}

@Composable
private fun CoordinateDemo() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewer =
        rememberPdfSignatureViewer(
            PsdkConfig(
                labels = PsdkLabels.indonesian(),
                ui =
                    PsdkUiConfig(
                        topbar =
                            TopBarConfig(
                                upload = false,
                                signature = false,
                                eStamp = false,
                                brandText = "Tes koordinat ezSign v2",
                            ),
                    ),
            ),
            key = "coordinate-source",
        )
    val state by viewer.state.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pilih PDF, isi specimenId, lalu tambah dan geser TTD.") }
    var name by rememberSaveable { mutableStateOf("document.pdf") }
    var sizes by rememberSaveable(
        stateSaver =
            listSaver<List<PageSize>, Double>(
                save = { it.flatMap { page -> listOf(page.width, page.height) } },
                restore = { it.chunked(2).map { pair -> PageSize(pair[0], pair[1]) } },
            ),
    ) { mutableStateOf(emptyList()) }
    var specimenId by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("I approve this Document") }
    var location by rememberSaveable { mutableStateOf("Surabaya") }
    var otp by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<PsdkImage?>(null) }
    val bindings =
        rememberSaveable(
            saver =
                mapSaver(
                    save = { it.toMap() },
                    restore = { it.mapValues { entry -> entry.value as String }.toList().toMutableStateMap() },
                ),
        ) { mutableStateMapOf<String, String>() }
    val exportFile = remember { java.io.File(context.cacheDir, "initsign-pending.json") }
    var resultUri by remember { mutableStateOf<Uri?>(null) }
    var details by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.signatures) { details = null }

    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) exportFile.delete()
            if (uri != null && exportFile.isFile) {
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) {
                            checkNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                                exportFile.inputStream().use { it.copyTo(output) }
                            }
                            exportFile.delete()
                        }
                        status = "JSON tersimpan. Buka di komputer, salin isinya ke Postman Body > raw > JSON."
                    } catch (e: Exception) {
                        status = "Gagal menyimpan: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    busy = true
                    sizes = emptyList()
                    bindings.clear()
                    try {
                        val source =
                            withContext(Dispatchers.IO) {
                                val bytes = readLimited(context, uri)
                                Triple(bytes, CoordinatePdfMetadata.read(bytes), displayName(context, uri))
                            }
                        viewer.loadDocument(PdfSource.BytesSource(source.first))
                        sizes = source.second
                        name = source.third
                        status = "$name: ${sizes.size} halaman. Isi specimenId backend sebelum menambah TTD."
                    } catch (e: Exception) {
                        status = "PDF gagal dibuka (gunakan PDF tanpa password): ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    busy = true
                    try {
                        val bitmap =
                            withContext(Dispatchers.IO) {
                                val bytes = readLimited(context, uri, 10 * 1024 * 1024)
                                requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Gambar tidak valid" }
                            }
                        image = PsdkImage.BitmapImage(bitmap)
                        status = "Gambar spesimen dipilih. Berlaku untuk TTD yang ditambahkan berikutnya."
                    } catch (e: Exception) {
                        status = "Gambar gagal dibuka: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    val checkJson =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    busy = true
                    try {
                        val count =
                            withContext(Dispatchers.IO) {
                                val body = org.json.JSONObject(readLimited(context, uri).toString(Charsets.UTF_8))
                                val bytes = android.util.Base64.decode(body.getString("base64Pdf"), android.util.Base64.DEFAULT)
                                val actual = CoordinatePdfMetadata.read(bytes)
                                CoordinatePayloadValidator.validate(body, actual)
                                body.getJSONArray("signatureData").length()
                            }
                        status = "JSON valid: $count TTD berada di dalam halaman PDF yang tertanam di base64Pdf."
                    } catch (e: Exception) {
                        status = "JSON TIDAK SESUAI: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    val openResult = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri = it }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PdfSignatureViewer(viewer, Modifier.weight(1f))
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Tutup panel Postman ▾" else "Buka panel Postman ▴")
        }
        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open.launch(arrayOf("application/pdf")) }, enabled = !busy) { Text("1. Buka PDF") }
                    OutlinedButton(onClick = { checkJson.launch(arrayOf("application/json", "text/plain")) }, enabled = !busy) {
                        Text("Cek JSON Postman")
                    }
                    OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }, enabled = !busy) { Text("Gambar TTD") }
                    OutlinedButton(onClick = { openResult.launch(arrayOf("application/pdf")) }, enabled = !busy) { Text("Buka PDF hasil") }
                }
                Text(status, fontSize = 12.sp)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = specimenId,
                    onValueChange = { specimenId = it },
                    label = { Text("specimenId backend (wajib)") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (image == null) {
                        "Preview memakai gambar demo. Pilih gambar milik specimenId agar bentuknya sama."
                    } else {
                        "Gambar pilihan dipakai untuk TTD berikutnya."
                    },
                    fontSize = 11.sp,
                )
                Button(enabled = !busy && sizes.isNotEmpty() && specimenId.isNotBlank(), onClick = {
                    try {
                        val size = viewer.getPageSize(viewer.currentPage)
                        val width = minOf(160.0, size.width * 0.35)
                        val id =
                            viewer.placeSignature(
                                PlaceOptions(
                                    width = width,
                                    height = if (image == null) width * 0.3 else null,
                                    image = image ?: PsdkImage.BitmapImage(SampleImages.signature),
                                    label = specimenId.trim(),
                                ),
                            )
                        bindings[id] = specimenId.trim()
                        status = "TTD ditambahkan. Geser atau resize langsung di PDF, lalu ekspor JSON."
                    } catch (e: Exception) {
                        status = "TTD gagal: ${e.message}"
                    }
                }) { Text("2. Tambah TTD") }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("reason") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("location") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("OTP (boleh isi nanti di Postman)") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "POST https://gtm.myesign.id/api/signing/initsign\nPostman: Authorization Bearer token; Body raw JSON.",
                    fontSize = 11.sp,
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy && sizes.isNotEmpty() && state.signatures.isNotEmpty(), onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val doc = viewer.exportInitSignDocument(bindings.toMap(), sizes)
                                val body = CoordinatePayload.build(doc, name, reason, location, otp)
                                withContext(Dispatchers.IO) {
                                    val bytes = android.util.Base64.decode(doc.base64Pdf, android.util.Base64.DEFAULT)
                                    CoordinatePayloadValidator.validate(body, CoordinatePdfMetadata.read(bytes))
                                    exportFile.writeText(body.toString(2))
                                }
                                status = "SHA-256 PDF: ${doc.documentSha256}"
                                save.launch("initsign-${System.currentTimeMillis()}.json")
                            } catch (e: Exception) {
                                status = "Ekspor ditolak: ${e.message}"
                            } finally {
                                busy = false
                            }
                        }
                    }) { Text("3. Simpan JSON Postman") }
                    OutlinedButton(enabled = !busy && sizes.isNotEmpty() && state.signatures.isNotEmpty(), onClick = {
                        try {
                            val rows =
                                state.signatures.map { item ->
                                    item.toInitSignData(viewer.getPageSize(item.page), sizes[item.page - 1], bindings[item.id].orEmpty())
                                }
                            val json =
                                org.json
                                    .JSONArray(
                                        rows.map { row ->
                                            org.json.JSONObject(
                                                row.mapValues {
                                                    it.value
                                                        ?: org.json.JSONObject.NULL
                                                },
                                            )
                                        },
                                    ).toString(2)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("signatureData", json))
                            details = json
                            status = "signatureData disalin. Untuk request lengkap termasuk base64Pdf gunakan Simpan JSON."
                        } catch (e: Exception) {
                            status = "Koordinat ditolak: ${e.message}"
                        }
                    }) { Text("Salin signatureData") }
                }
                SelectionContainer {
                    Column {
                        for (item in state.signatures) {
                            val size = sizes.getOrNull(item.page - 1)
                            val backend =
                                runCatching {
                                    item.toInitSignData(viewer.getPageSize(item.page), requireNotNull(size), bindings[item.id].orEmpty())
                                }
                            Text(
                                backend.fold(
                                    onSuccess = { "Payload kiri bawah: x=${it["x"]}, y=${it["y"]}, w=${it["width"]}, h=${it["height"]}" },
                                    onFailure = { "Posisi belum valid: ${it.message}" },
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            )
                            Text(
                                "Halaman ${item.page} / ${size?.width} × ${size?.height} pt\nSDK kiri atas: x=${item.x}, y=${item.y}, w=${item.width}, h=${item.height}\nSpecimen: ${bindings[item.id]}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            )
                        }
                        details?.let { Text(it, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    resultUri?.let { uri -> ResultPreview(uri, onClose = { resultUri = null }) }
}

@Composable
private fun ResultPreview(
    uri: Uri,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val result =
        rememberPdfSignatureViewer(
            PsdkConfig(
                labels = PsdkLabels.indonesian(),
                ui = PsdkUiConfig(topbar = TopBarConfig(upload = false, signature = false, eStamp = false, brandText = "PDF hasil server")),
            ),
            key = "coordinate-result",
        )
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uri) {
        try {
            val bytes = withContext(Dispatchers.IO) { readLimited(context, uri) }
            result.loadDocument(PdfSource.BytesSource(bytes))
        } catch (e: Exception) {
            error = e.message
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties =
            androidx.compose.ui.window
                .DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column {
                TextButton(onClick = onClose) { Text("Kembali ke PDF asli & posisi TTD") }
                error?.let { Text(it) }
                PdfSignatureViewer(result, Modifier.weight(1f))
            }
        }
    }
}

private fun readLimited(
    context: Context,
    uri: Uri,
    limit: Int = 100 * 1024 * 1024,
): ByteArray =
    checkNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= limit) { "File terlalu besar" }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }

private fun displayName(
    context: Context,
    uri: Uri,
): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "document.pdf"
