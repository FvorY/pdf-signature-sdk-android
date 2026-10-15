# Dokumentasi PdfSignatureSDK Android

Dokumen ini berisi cara memasang dan memakai SDK di aplikasi Android. SDK menyediakan viewer PDF
dengan fitur penempatan tanda tangan dan E-Materai. Hasil penempatan bisa dikirim ke backend dalam
format yang sama dengan web SDK.

## Fitur

- Membuka PDF dari `File`, `Uri`, URL, byte array, atau folder `assets`.
- Menampilkan PDF dengan pagination, thumbnail, pinch-zoom, dan fit-to-screen.
- Menambah, memindahkan, mengubah ukuran, dan menghapus tanda tangan atau E-Materai.
- Menyediakan API Kotlin, event listener, `StateFlow`, dan output JSON.
- Bisa dipakai dengan Jetpack Compose atau XML/View biasa.
- Mendukung tema, warna, label, picker, item lock, skeleton, dan `FLAG_SECURE`.
- Modul opsional Pdfium untuk PDF berpassword pada Android 14 ke bawah.

## Persyaratan

- Android 7.0 / API 24 atau lebih baru.
- `compileSdk` 36 atau lebih baru.
- Kotlin dan AndroidX.
- Permission `INTERNET` untuk PDF atau gambar dari URL.

## Instalasi

Tambahkan JitPack di `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Tambahkan dependency di module aplikasi:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<version>")

    // Opsional: dukungan PDF berpassword pada Android 14 dan lebih lama.
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<version>")
}
```

Ganti `<version>` dengan tag atau commit yang ingin dipakai. Untuk versi yang belum dirilis,
JitPack juga dapat memakai commit hash.

## Pemakaian dengan Jetpack Compose

`rememberPdfSignatureViewer` menyimpan viewer di `ViewModel`, sehingga dokumen dan posisi item tetap
tersedia saat rotasi atau proses aplikasi dipulihkan.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkConfig
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.rememberPdfSignatureViewer

@Composable
fun SignScreen(uri: Uri) {
    val viewer = rememberPdfSignatureViewer(
        PsdkConfig(file = PdfSource.UriSource(uri))
    )

    PdfSignatureViewer(
        viewer = viewer,
        modifier = Modifier.fillMaxSize(),
    )
}
```

Jika viewer dibuat tanpa `file`, dokumen bisa dibuka kemudian:

```kotlin
val viewer = rememberPdfSignatureViewer()

LaunchedEffect(pdfUri) {
    viewer.loadDocument(PdfSource.UriSource(pdfUri))
}
```

## Pemakaian dengan XML atau View

Tambahkan view ke layout:

```xml
<io.github.fvory.psdk.PdfSignatureView
    android:id="@+id/pdfSignatureView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Atur sumber PDF dari Activity atau Fragment:

```kotlin
val pdfView = findViewById<PdfSignatureView>(R.id.pdfSignatureView)
pdfView.setConfig(PsdkConfig(file = PdfSource.UriSource(uri)))

val viewer = pdfView.viewer
```

Host harus menggunakan `ComponentActivity` atau turunannya, misalnya `AppCompatActivity`.
Saat memakai XML, rotasi akan membuat viewer baru; simpan `getSignatures()` sendiri bila posisi
item perlu dipertahankan.

## Sumber PDF

```kotlin
PdfSource.FileSource(file)
PdfSource.UriSource(uri)
PdfSource.UrlSource("https://example.com/document.pdf")
PdfSource.UrlSource(
    url = "https://example.com/document.pdf",
    headers = mapOf("Authorization" to "Bearer <token>"),
)
PdfSource.BytesSource(pdfBytes)
PdfSource.AssetSource("documents/example.pdf")
```

Untuk URL, HTTPS diwajibkan secara default. HTTP hanya bisa dipakai dengan opt-in:

```kotlin
val config = PsdkConfig(allowInsecureHttp = true)
```

Gunakan opsi ini hanya untuk endpoint lama yang memang dipercaya. Download PDF dibatasi 100 MiB,
gambar 10 MiB, dan redirect divalidasi maksimal lima kali.

## Menempatkan item dari kode

```kotlin
val signatureId = viewer.placeSignature(
    PlaceOptions(
        page = 1,
        x = 72.0,
        y = 680.0,
        label = "Direktur",
        image = PsdkImage.Url("https://example.com/signature.png"),
        width = 140.0,
        height = 60.0,
    )
)

val estampId = viewer.placeEStamp(
    PlaceOptions(page = 1, x = 420.0, y = 700.0)
)
```

`page` dimulai dari 1. `x` dan `y` memakai titik kiri-atas halaman, satuannya PDF point
(1/72 inci). A4 umumnya berukuran 595 × 842 point. `x`, `y`, `width`, dan `height` akan dihitung
otomatis bila tidak diisi.

Item yang sudah dibuat dapat dihapus atau dibaca:

```kotlin
viewer.removeSignature(signatureId)
val allItems = viewer.getSignatures()
val pageItems = viewer.getSignaturesByPage(1)
val json = allItems.toJsonString()
```

`SignatureItem` juga memiliki `toMap()`, `toJson()`, dan `yFromBottom(pageHeight)`. SDK menyimpan
`y` dari atas, sedangkan beberapa library PDF memakai koordinat dari bawah.

## Picker tanda tangan dan E-Materai

Jika `signatureOptions` atau `estampOptions` kosong, tombol langsung menambahkan item default. Untuk
menampilkan pilihan, isi `PickerOption` pada konfigurasi:

```kotlin
val config = PsdkConfig(
    signatureOptions = listOf(
        PickerOption(
            id = "director",
            label = "Direktur",
            image = PsdkImage.Resource(R.drawable.signature_director),
        )
    ),
    estampOptions = listOf(
        PickerOption(
            id = "materai-10000",
            label = "Materai 10.000",
            image = PsdkImage.Resource(R.drawable.materai_10000),
        )
    ),
)
```

Sumber gambar yang tersedia adalah `PsdkImage.Resource`, `BitmapImage`, `Url`, dan `Base64`.
`PsdkImage.of(value)` dapat dipakai untuk membedakan URL dan Base64 dari sebuah string.

## Event dan state

Listener dipanggil di main thread dan subscription bisa dibatalkan:

```kotlin
val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
    val item = event.item
    sendToBackend(item.toJson())
}

// Saat screen selesai digunakan:
subscription.cancel()
```

Event yang tersedia meliputi `DocumentLoaded`, `PageChanged`, `SignaturePlaced`, `EStampPlaced`,
`SignatureMoved`, `SignatureRemoved`, `SignaturesCleared`, dan `PasswordRequested`.

Untuk aplikasi Compose, status viewer tersedia melalui `viewer.state`:

```kotlin
val state by viewer.state.collectAsState()
Text("Halaman ${state.currentPage} dari ${state.totalPages}")
```

Alternatif coroutine adalah `viewer.events`, yaitu `SharedFlow<PsdkEvent>`.

## Navigasi dan zoom

```kotlin
viewer.nextPage()
viewer.prevPage()
viewer.goToPage(2)
viewer.setPaginationLocked(true)

viewer.setScale(1.5f)
viewer.fitToScreen()
viewer.fitToWidth()
```

`currentPage` dimulai dari 1, sedangkan `totalPages` bernilai 0 sebelum dokumen selesai dibuka.
Zoom yang didukung berada pada rentang 0.25 sampai 4.0.

## PDF berpassword

Dialog password bawaan aktif secara default. Jika aplikasi ingin menangani sendiri:

```kotlin
val config = PsdkConfig(
    ui = PsdkUiConfig(passwordDialog = false)
)

viewer.on<PsdkEvent.PasswordRequested> { event ->
    showPasswordDialog { password ->
        event.updatePassword(password)
    }
}
```

Password tidak dimasukkan ke event log atau snapshot state. Modul `psdk-pdfium` diperlukan untuk
dukungan PDF berpassword pada Android 14 dan lebih lama.

## Konfigurasi umum

`PsdkConfig` dapat dipakai untuk mengganti tema, warna, label UI, font, ukuran awal, dan perilaku
interaksi. Beberapa contoh:

```kotlin
val config = PsdkConfig(
    theme = PsdkThemeMode.SYSTEM,
    scale = 1.25f,
    disabled = false,
    disableDragging = true,
    disableResize = true,
    secureScreen = true,
    labels = PsdkLabels(
        signatureBtn = "Tambah tanda tangan",
        estampBtn = "Tambah E-Materai",
    ),
)
```

`secureScreen = true` memasang `FLAG_SECURE` pada window untuk mencegah screenshot, screen
recording, dan preview recent apps ketika viewer ditampilkan.

## Lifecycle

Panggil `destroy()` bila viewer dibuat manual dan tidak lagi dipakai. Viewer dari
`rememberPdfSignatureViewer` dan `PdfSignatureView` mengikuti lifecycle host.

## Build dan test dari source

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

Sample berada di module `sample`. Fixture parity untuk membandingkan output dengan web SDK berada
di folder `parity/`.

## Status API

Project ini masih berstatus preview. API publik dapat berubah sebelum rilis 1.0.0.

