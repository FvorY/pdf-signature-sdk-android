# PdfSignatureSDK untuk Android — Panduan

SDK ini menampilkan PDF dan memungkinkan user menaruh **tanda tangan** dan **E-Materai** di atasnya.
Posisi dikembalikan dalam PDF point, dengan format JSON yang sama dengan web SDK
[`pdf-signature-esign-sdk`](https://www.npmjs.com/package/pdf-signature-esign-sdk) 1.1.2, sehingga
satu backend bisa menyelesaikan tanda tangan untuk web, Android, dan iOS.

[Panduan SDK iOS](https://github.com/FvorY/pdf-signature-sdk-ios/blob/main/DOCUMENTATION.id.md)
memakai bagian yang sama. English: [DOCUMENTATION.en.md](DOCUMENTATION.en.md).

1. [Persyaratan dan instalasi](#1-persyaratan-dan-instalasi)
2. [Menampilkan viewer](#2-menampilkan-viewer)
3. [Sumber PDF](#3-sumber-pdf)
4. [Menaruh item](#4-menaruh-item)
5. [Mengedit di layar](#5-mengedit-di-layar)
6. [Mengambil hasil](#6-mengambil-hasil)
7. [Event dan state](#7-event-dan-state)
8. [Halaman dan zoom](#8-halaman-dan-zoom)
9. [PDF berpassword](#9-pdf-berpassword)
10. [UI bawaan](#10-ui-bawaan)
11. [Tema, warna, font, dan label](#11-tema-warna-font-dan-label)
12. [Tombol upload](#12-tombol-upload)
13. [Overlay loading](#13-overlay-loading)
14. [Gambar halaman](#14-gambar-halaman)
15. [Perlindungan layar](#15-perlindungan-layar)
16. [Error](#16-error)
17. [Lifecycle dan pemulihan state](#17-lifecycle-dan-pemulihan-state)
18. [Semua opsi](#18-semua-opsi)
19. [Perbedaan platform](#19-perbedaan-platform)
20. [Build dari source](#20-build-dari-source)

## 1. Persyaratan dan instalasi

| | Minimum | Catatan |
|---|---|---|
| Android | minSdk 24 (7.0) | |
| compileSdk | 35 | |
| Android Gradle Plugin | 8.6 | Disarankan 8.9.1+. Di 8.6–8.8 tambahkan `lint { disable += "NullSafeMutableLiveData" }`: lint versi itu crash pada check bawaan AndroidX Lifecycle 2.10. |
| Kotlin | 2.1 | `psdk-pdfium` butuh Kotlin 2.2 (library Pdfium-nya dibuat dengan Kotlin 2.3). |
| JVM target | 1.8 | Aplikasi dengan target 1.8, 11, atau 17 semuanya bisa. |

Kombinasi di atas di-build oleh CI di setiap perubahan (`compat/`).

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

lalu SDK di modul aplikasi:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<versi>")
    // Opsional, lihat §9: PDF berpassword di perangkat yang sistemnya tidak bisa membukanya
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<versi>")
}
```

`<versi>` adalah tag atau hash commit (SDK masih preview, belum ada tag 1.0.0). URL remote butuh
`<uses-permission android:name="android.permission.INTERNET" />` di aplikasi.

`psdk-pdfium` menambah sekitar 18 MB library native untuk empat ABI pada APK universal. Rilis dalam
bentuk Android App Bundle atau pakai ABI split, supaya tiap perangkat hanya mengunduh miliknya
(sekitar 4–5 MB).

## 2. Menampilkan viewer

### Jetpack Compose

```kotlin
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.rememberPdfSignatureViewer

@Composable
fun SignScreen(uri: Uri) {
    val viewer = rememberPdfSignatureViewer(PsdkConfig(file = PdfSource.UriSource(uri)))
    PdfSignatureViewer(viewer, Modifier.fillMaxSize())
}
```

`rememberPdfSignatureViewer(config, key)` mempertahankan viewer saat rotasi dan memulihkannya
setelah Android menghentikan aplikasi di background (§17). `config` hanya dipakai pertama kali;
ubah setelahnya dengan `viewer.updateConfig`. Pakai `key` berbeda untuk beberapa viewer di satu layar.

Untuk membuka dokumen belakangan, buat viewer tanpa `file`:

```kotlin
val viewer = rememberPdfSignatureViewer()
LaunchedEffect(uri) { viewer.loadDocument(PdfSource.UriSource(uri)) }
```

### XML / View

```xml
<io.github.fvory.psdk.PdfSignatureView
    android:id="@+id/pdfSignatureView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val pdfView = findViewById<PdfSignatureView>(R.id.pdfSignatureView)
pdfView.setConfig(PsdkConfig(file = PdfSource.UriSource(uri)))
val viewer = pdfView.viewer
```

Host harus `ComponentActivity` (misalnya `AppCompatActivity`). View mengikuti lifecycle host.
Dengan XML, rotasi membuat viewer baru; simpan `getSignatures()` sendiri bila posisi harus tetap ada.

## 3. Sumber PDF

```kotlin
PdfSource.FileSource(file)
PdfSource.UriSource(uri)                       // content:// dari file picker
PdfSource.UrlSource("https://example.com/a.pdf", headers = mapOf("Authorization" to "Bearer …"))
PdfSource.BytesSource(bytes)
PdfSource.AssetSource("documents/example.pdf")  // assets aplikasi
```

PDF dan gambar remote wajib HTTPS. `PsdkConfig(allowInsecureHttp = true)` mengizinkan HTTP hanya
untuk server lama yang tepercaya. Download mengikuti maksimal lima redirect yang divalidasi, header
hanya dikirim ke origin awal, dan ukurannya dibatasi 100 MiB (PDF) serta 10 MiB (gambar).

## 4. Menaruh item

```kotlin
val signatureId = viewer.placeSignature(
    PlaceOptions(
        page = 1, x = 72.0, y = 680.0, label = "Direktur",
        image = PsdkImage.Url("https://example.com/ttd.png"), width = 140.0,
    )
)
val stampId = viewer.placeEStamp(PlaceOptions(page = 1, x = 420.0, y = 700.0))
```

| `PlaceOptions` | Arti | Default |
|---|---|---|
| `page` | Halaman, mulai dari 1 | halaman aktif |
| `x`, `y` | Pojok kiri-atas dalam PDF point | di tengah, kecuali keduanya diisi |
| `width`, `height` | Ukuran dalam PDF point | 160 × 48 (tanda tangan), 80 × 80 (E-Materai) |
| `label` | Teks tanda tangan | `"Signature"`; E-Materai selalu `"E-Materai"` |
| `image` | `PsdkImage.Url`, `Base64`, `BitmapImage`, `Resource`, atau `PsdkImage.of(string)` | tidak ada |
| `disableDrag`, `disableResize`, `disableDragging`, `locked` | Kunci untuk item ini (§5) | `false` |

Tanpa `height`, item bergambar mengikuti rasio gambar setelah gambarnya termuat (event
`SignatureMoved` melaporkan ukuran barunya), sama seperti web SDK.

**Koordinat**: PDF point (1/72 inci), titik nol di pojok kiri-atas halaman seperti yang tampil
(setelah /Rotate), sumbu Y ke bawah. A4 sekitar 595 × 842. `getPageSize(page)` memberi ukuran tiap
halaman.

Tombol tambah dan picker di UI bawaan dijelaskan di §10.

## 5. Mengedit di layar

- Geser item untuk memindahkan; tarik pegangan di pojok kanan-bawah untuk resize (minimal 20 pt);
  tap tombol di pojok kanan-atas untuk menghapus. Item tetap berada di dalam halamannya.
- Kunci mengikuti web SDK: `disableDrag` menahan geser, `disableResize` menahan resize,
  `disableDragging` dan `locked` menahan keduanya. Tombol hapus tetap berfungsi pada item yang
  dikunci; hanya `disabled` yang mematikannya. Kunci yang sama tersedia untuk semua item di `PsdkConfig`.
- `estampKeepAspectRatio = true` menjaga rasio lebar/tinggi E-Materai saat di-resize.
- `ui.haptics` memberi getaran singkat saat item mulai digeser atau dihapus.
- TalkBack membacakan tiap item ("Signature, halaman 1") dan menyediakan aksi geser,
  perbesar/perkecil, dan hapus.

## 6. Mengambil hasil

```kotlin
val items: List<SignatureItem> = viewer.getSignatures()
val diHalaman2 = viewer.getSignaturesByPage(2)
val json: String = items.toJsonString()   // sama persis dengan JSON.stringify di web SDK
val array: JSONArray = items.toJsonArray()
```

```json
{"id":"signature-1760600000000-k3x9a","type":"signature","x":72,"y":680,"page":1,"label":"Direktur",
 "width":140,"height":48,"disableDrag":false,"disableDragging":false,"disableResize":false,"locked":false}
```

`SignatureItem` juga punya `toMap()`, `toJson()`, dan `yFromBottom(pageHeight)` untuk library PDF
yang mengukur dari pojok kiri-bawah. `removeSignature(id)` dan `clearSignatures()` menghapus item.

## 7. Event dan state

```kotlin
val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event -> kirim(event.item.toJson()) }
viewer.once<PsdkEvent.DocumentLoaded> { println("${it.totalPages} halaman") }
subscription.cancel() // atau viewer.off(subscription)
```

| Event | Data | Kapan |
|---|---|---|
| `DocumentLoaded` | `totalPages` | dokumen terbuka |
| `PageChanged` | `page`, `total`, `source` (`SCROLL` atau `API`) | halaman aktif berubah |
| `SignaturePlaced` / `EStampPlaced` | `item` | item ditaruh |
| `SignatureMoved` | `item` | item digeser atau di-resize (juga setelah gambar mengatur tingginya) |
| `SignatureRemoved` | `id` | item dihapus |
| `SignaturesCleared` | – | semua item dihapus, juga saat dokumen baru dibuka |
| `PasswordRequested` | `isIncorrect`, `updatePassword()`, `cancel()` | password dibutuhkan (§9) |

Listener berjalan di main thread. Dengan coroutine, collect `viewer.events` (`SharedFlow`) atau
`viewer.state` (`StateFlow<PsdkState>`: `isLoading`, `isLoaded`, `currentPage`, `totalPages`,
`scale`, `isPaginationLocked`, `isPasswordProtected`, `pageSizes`, `signatures`, `error`).

## 8. Halaman dan zoom

```kotlin
viewer.nextPage(); viewer.prevPage(); viewer.goToPage(3) // false bila tidak bisa
viewer.setPaginationLocked(true) // mengunci tombol, input halaman, thumbnail, dan goToPage
viewer.setScale(1.5f)            // 0,25–4,0 (suspend)
viewer.fitToScreen(); viewer.fitToWidth()
```

Tanpa `PsdkConfig(scale = …)`, halaman terlebar menyesuaikan lebar layar. User bisa pinch dan
double-tap untuk zoom (`ui.viewer.zoom = false` mematikannya). `lockScrollWhenPaginationLocked =
true` juga menahan scroll saat pagination dikunci (default mati, sama seperti web SDK).

## 9. PDF berpassword

`loadDocument` menunggu sampai password membuka dokumen, dan melempar
`PsdkException.PasswordCancelled` bila prompt dibatalkan. Dialog bawaan (`ui.passwordDialog`,
default nyala) meminta password; event `PasswordRequested` tetap dikirim, jadi bisa diganti dialog
sendiri:

```kotlin
val viewer = rememberPdfSignatureViewer(PsdkConfig(ui = PsdkUiConfig(passwordDialog = false)))
viewer.on<PsdkEvent.PasswordRequested> { event ->
    tampilkanDialog(salah = event.isIncorrect, onOk = event::updatePassword, onBatal = event::cancel)
}
```

Password tidak pernah di-log, dikirim lewat event, atau disimpan. Android 15+ bisa membuka PDF
terenkripsi sendiri; Android 12–14 butuh pembaruan sistem S extension 13; perangkat lain butuh
`psdk-pdfium`. `state.isPasswordProtected` (dan badge 🔒) tetap aktif selama dokumen terenkripsi
terbuka.

## 10. UI bawaan

Viewer punya UI sendiri di sekitar halaman, sama di Android dan iOS:

| Bagian | Ponsel (lebar < 600 dp) | Tablet (≥ 600 dp) |
|---|---|---|
| Top bar | brand dan menu ⋮ (zoom, fit, tema, ke halaman) | brand, upload, tambah tanda tangan, E-Materai, hapus semua, halaman, zoom, tema |
| Bottom bar | upload, tambah tanda tangan, E-Materai, hapus semua (bisa digeser bila label panjang) | – |
| Indikator halaman | "‹ 1 / 5 ›" mengambang; tap angkanya untuk lompat halaman | di top bar |
| Thumbnail | strip di atas bottom bar (disembunyikan bila tinggi < 560 dp) | sidebar |
| Status bar | "Siap · 2 tanda tangan", badge 🔒, zoom (disembunyikan bila tinggi < 420 dp) | sama |
| Empty state | judul, teks, tombol upload, dan error bila gagal membuka | sama |

`ui.layout` memaksa `PHONE` atau `TABLET`. Semua bagian bisa dimatikan:

```kotlin
PsdkConfig(
    ui = PsdkUiConfig(
        topbar = TopBarConfig(upload = false, themeToggle = false, brandText = "Aplikasiku"),
        sidebar = SidebarConfig(thumbnails = false),
        statusbar = false,
    ),
)
```

Bila semua opsi `topbar` mati dan tidak ada brand, logo, atau konten kustom, top bar tidak tampil.
`topbar.customComponent` menambah composable sendiri; `topbar.logo` dan `topbar.style` (latar, warna
isi, tinggi, padding) mengatur tampilannya. `disabled = true` mematikan tombol toolbar dan semua
pengeditan item.

**Picker.** Tombol tambah serta `openSignatureModal()` / `openEStampModal()` menaruh item default,
atau menampilkan daftar bila opsi diisi:

```kotlin
PsdkConfig(
    signatureOptions = listOf(
        PickerOption("direktur", "Direktur", PsdkImage.Resource(R.drawable.ttd_direktur), group = "Internal"),
        PickerOption("klien", "Klien", group = "Eksternal"),
    ),
    estampOptions = listOf(PickerOption("materai-10000", "Materai 10.000")),
    groupByCategory = true, // judul per grup; opsi tanpa grup masuk labels.defaultGroup
)
```

## 11. Tema, warna, font, dan label

```kotlin
PsdkConfig(
    theme = PsdkThemeMode.SYSTEM, // LIGHT (default), DARK, atau SYSTEM
    lightColors = PsdkColors.light().copy(accent = Color(0xFF0057B8)),
    darkColors = PsdkColors.dark(),
    fontFamily = FontFamily(Font(R.font.inter)),
    labels = PsdkLabels.indonesian().copy(uploadBtn = "Pilih PDF"),
)
```

- `PsdkColors.light()` / `dark()` adalah warna web yang disesuaikan agar kontrasnya lolos WCAG AA;
  `webLight()` / `webDark()` adalah warna web asli. Ada 18 token (`background`, `surface`,
  `accent`, `danger`, `textPrimary`, …). Halaman PDF selalu putih.
- Tombol tema berpindah antara terang dan gelap tanpa mengirim event.
- `PsdkLabels` memuat semua teks (default Inggris, `PsdkLabels.indonesian()` untuk Bahasa
  Indonesia), termasuk teks TalkBack. Label item di hasil tetap `"Signature"` dan `"E-Materai"` di
  semua bahasa.

## 12. Tombol upload

Tombol upload (`topbar.upload`) membuka file picker sistem untuk PDF. `onUpload` menentukan apa yang
terjadi dengan file pilihan:

```kotlin
PsdkConfig(onUpload = { dipilih ->
    if (!diizinkan(dipilih)) UploadDecision.Cancel else UploadDecision.Proceed
    // atau UploadDecision.Replace(sumberLain)
})
```

## 13. Overlay loading

```kotlin
viewer.showSkeleton(true, SkeletonOptions(backgroundOpacity = 0.3f, blurRadius = 8.dp))
// … kirim ke backend …
viewer.showSkeleton(false)
```

`SkeletonOptions.content` mengganti spinner dengan composable sendiri. Blur butuh Android 12+.

## 14. Gambar halaman

```kotlin
val halaman: Bitmap = viewer.renderPage(page = 1, scale = 2f)               // piksel per PDF point
val denganItem: Bitmap = viewer.renderPageWithOverlay(page = 1, scale = 2f) // item digambar di atasnya
```

Berguna untuk pratinjau atau layar konfirmasi. Gambar mengikuti /Rotate seperti viewer.

## 15. Perlindungan layar

`secureScreen = true` memasang `FLAG_SECURE` pada window selama viewer tampil: screenshot dan rekaman
layar menjadi hitam, dan pratinjau recent apps disembunyikan. Flag dilepas lagi hanya bila SDK yang
memasangnya.

## 16. Error

`loadDocument`, `placeSignature` / `placeEStamp`, `getPageSize`, dan `renderPage` melempar
`PsdkException`:

| Exception | Arti |
|---|---|
| `InvalidSource` | file tidak ada atau tidak terbaca, atau tidak diizinkan (mis. HTTP tanpa `allowInsecureHttp`) |
| `Network` | download gagal (dengan kode status bila ada) |
| `NotAPdf` / `Corrupted` | bukan PDF / PDF rusak |
| `PasswordCancelled` | prompt password dibatalkan |
| `PasswordNotSupported` | PDF terenkripsi di perangkat yang tidak bisa membukanya tanpa `psdk-pdfium` |
| `InvalidPage` / `NotLoaded` / `Destroyed` | nomor halaman salah / belum ada dokumen / viewer sudah dihancurkan |

Setelah gagal membuka, empty state menampilkan pesan singkat; dengan `logLevel = PsdkLogLevel.DEBUG`
juga menampilkan exception-nya, dan SDK menulis detail ke Logcat.

## 17. Lifecycle dan pemulihan state

- Viewer dari `rememberPdfSignatureViewer` dan `PdfSignatureView` mengikuti lifecycle host. Panggil
  `destroy()` hanya untuk viewer yang dibuat sendiri.
- `rememberPdfSignatureViewer` bertahan saat rotasi. Setelah Android menghentikan aplikasi di
  background, dokumen dibuka lagi di halaman dan zoom yang sama dengan item yang sama (item yang
  dipulihkan tidak mengirim event `SignaturePlaced`; password tidak pernah disimpan).

## 18. Semua opsi

| `PsdkConfig` | Default | Lihat |
|---|---|---|
| `file` | `null` | §3 |
| `allowInsecureHttp` | `false` | §3 |
| `scale` | `null` (fit width) | §8 |
| `theme`, `lightColors`, `darkColors`, `fontFamily` | `LIGHT`, warna AA, font sistem | §11 |
| `disabled`, `disableDragging`, `disableDrag`, `disableResize` | `false` | §5 |
| `estampKeepAspectRatio` | `false` | §5 |
| `signatureOptions`, `estampOptions`, `groupByCategory` | kosong, `false` | §10 |
| `onUpload` | `null` (buka file pilihan) | §12 |
| `lockScrollWhenPaginationLocked` | `false` | §8 |
| `secureScreen` | `false` | §15 |
| `labels` | Inggris | §11 |
| `ui` | semua nyala | `topbar`, `sidebar.thumbnails`, `viewer.zoom`, `statusbar`, `passwordDialog`, `layout`, `haptics` (§10) |
| `logLevel` | `ERROR` | `NONE`, `ERROR`, `DEBUG` (§16) |

Ubah opsi saat berjalan dengan `viewer.updateConfig { it.copy(disabled = true) }`.

## 19. Perbedaan platform

| | Android | iOS |
|---|---|---|
| UI toolkit | Compose dan XML/View | UIKit (`PdfSignatureView`) dan SwiftUI (`PdfSignatureViewer`) |
| State | `viewer.state` (`StateFlow`) | `PdfSignatureController` (`@Published`) |
| Event | `on<T>`, `events` (`SharedFlow`) | `on(.type)`, `eventsPublisher` (Combine), delegate |
| Perlindungan layar | screenshot dan rekaman diblokir | ditutup saat direkam atau di-mirror; iOS tidak bisa memblokir screenshot |
| PDF terenkripsi | Android 15+ bawaan, 12–14 dengan pembaruan sistem, selain itu `psdk-pdfium` | selalu bawaan (PDFKit) |
| Pemulihan setelah aplikasi dihentikan | otomatis | tidak perlu untuk rotasi; simpan `getSignatures()` sendiri antar-peluncuran |
| Konten kustom di toolbar | `customComponent` (composable) | `customView` (`UIView`) |
| Font | `fontFamily` | `typography` (nama font dan ukuran dasar) |

Selebihnya (koordinat, JSON, event, opsi, UI bawaan) sama.

## 20. Build dari source

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

`sample/` adalah aplikasi demo, `parity/` berisi fixture pembanding dengan web SDK, dan `compat/`
aplikasi untuk mengecek kompatibilitas dengan build tool lama. SDK masih preview; API publik masih
bisa berubah sebelum 1.0.0.
