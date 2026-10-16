# PdfSignatureSDK untuk Android

[![CI](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/FvorY/pdf-signature-sdk-android.svg)](https://jitpack.io/#FvorY/pdf-signature-sdk-android)
![Android API 24+](https://img.shields.io/badge/Android-API%2024%2B-blue)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

PDF viewer native Android untuk menempatkan **tanda tangan** dan **E-Materai** pada dokumen. User
bisa menambah, menggeser, me-resize, mengunci, dan menghapus item. SDK mengembalikan posisi dalam
PDF point, sehingga payload yang sama dapat dikirim ke backend yang dipakai SDK web dan iOS.

SDK Android dan iOS memakai konsep inti yang sama: sumber PDF, penempatan item, event, koordinat,
output JSON/Codable, dan proses signing di backend. Perbedaannya hanya pada integrasi UI sesuai
platform.

> Preview: viewer dan sample sudah dapat digunakan, tetapi API publik masih bisa berubah sebelum
> rilis 1.0.0.

Bahasa dan dokumentasi lengkap:

- [English README](README.md)
- [Dokumentasi Bahasa Indonesia](DOCUMENTATION.id.md)
- [English documentation](DOCUMENTATION.en.md)

## Fitur

- Render PDF dengan `PdfRenderer`, pagination, thumbnail, pinch-zoom, dan fit-to-width.
- Penempatan tanda tangan dan E-Materai dengan drag, resize, hapus, dan kunci per item.
- API Kotlin, event, `StateFlow`, dan output JSON yang kompatibel dengan web SDK.
- Dukungan Jetpack Compose dan XML/View klasik.
- Toolbar bawaan untuk ponsel dan tablet (upload, tambah, hapus semua, halaman, zoom, tema), indikator
  halaman, thumbnail, status bar, dan empty state, sama seperti SDK iOS.
- Tema terang, gelap, dan sistem; label, picker, skeleton loading, dan `FLAG_SECURE`.
- Modul opsional `psdk-pdfium` untuk PDF berpassword di Android 14 ke bawah.

## Persyaratan

| Komponen | Persyaratan |
|---|---|
| Android | minSdk 24 / Android 7.0+ |
| compileSdk | 35+ |
| Kotlin | 2.1+; `psdk-pdfium` membutuhkan Kotlin 2.2+ |
| UI | Jetpack Compose atau XML/View |

`psdk-pdfium` menambah sekitar 18 MB library native pada APK universal. Untuk production,
disarankan memakai Android App Bundle atau ABI split.

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

Tambahkan SDK utama di module aplikasi:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<versi>")
}
```

Untuk PDF berpassword di Android 14 ke bawah, tambahkan juga:

```kotlin
implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<versi>")
```

Ganti `<versi>` dengan tag atau commit yang ingin digunakan. Saat ini belum ada rilis stabil 1.0.0;
JitPack dapat membuild commit hash selama API masih dalam tahap preview.

## Mulai cepat: Jetpack Compose

Di contoh, `uri`, `file`, `bytes`, dan `context` mewakili nilai milikmu sendiri; selebihnya adalah
API SDK.

```kotlin
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

`rememberPdfSignatureViewer` menyimpan viewer di `ViewModel` saat terjadi perubahan konfigurasi.
Untuk membuka dokumen setelah viewer dibuat, panggil `viewer.loadDocument(...)`.

## Mulai cepat: XML/View

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

Host harus berupa `ComponentActivity` atau turunannya seperti `AppCompatActivity`. Pada XML,
rotasi membuat viewer baru; simpan hasil `getSignatures()` sendiri jika posisi harus dipulihkan.

## Sumber PDF

```kotlin
PdfSource.FileSource(file)
PdfSource.UriSource(uri)
PdfSource.UrlSource("https://example.com/document.pdf")
PdfSource.BytesSource(pdfBytes)
PdfSource.AssetSource("documents/example.pdf")
```

`UrlSource` juga menerima request headers. URL PDF dan gambar remote wajib HTTPS secara default.
Aplikasi host harus mendeklarasikan `android.permission.INTERNET` untuk sumber remote. HTTP hanya
bisa diaktifkan untuk endpoint lama yang tepercaya dengan `PsdkConfig(allowInsecureHttp = true)`.

Download dibatasi 100 MiB untuk PDF dan 10 MiB untuk gambar; redirect divalidasi maksimal lima kali.

## Menempatkan item dan mengirim hasil

```kotlin
val id = viewer.placeSignature(
    PlaceOptions(
        page = 1,
        x = 72.0,
        y = 680.0,
        label = "Direktur",
        width = 140.0,
        height = 60.0,
    )
)

viewer.placeEStamp(PlaceOptions(page = 1, x = 420.0, y = 700.0))

val items = viewer.getSignatures()
val json = items.toJsonString()
viewer.removeSignature(id)
```

Koordinat memakai PDF point (1/72 inci), dengan pojok kiri-atas halaman sebagai `(0, 0)` dan
sumbu Y ke bawah. `page` dimulai dari 1. Ukuran A4 umumnya 595 × 842 point. Model item juga
memiliki `toMap()`, `toJson()`, dan `yFromBottom(pageHeight)` untuk library PDF yang memakai titik
awal kiri-bawah.

## Event dan state

```kotlin
val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
    Log.d("psdk", event.item.toJsonString()) // kirim ini ke backend
}

subscription.cancel()
```

Event yang tersedia antara lain `DocumentLoaded`, `PageChanged`, `SignaturePlaced`, `EStampPlaced`,
`SignatureMoved`, `SignatureRemoved`, `SignaturesCleared`, dan `PasswordRequested`. Pengguna
Compose dan coroutine dapat mengumpulkan `viewer.state` dan `viewer.events`.

## Konfigurasi dan keamanan

`PsdkConfig` mengatur tema, label, picker, zoom awal, kunci item, UI password, warna, dan
`secureScreen`. Set `secureScreen = true` untuk memasang `FLAG_SECURE` saat viewer ditampilkan;
fitur ini mencegah screenshot, rekam layar, dan preview recent apps.

SDK hanya mengembalikan data posisi item. Signing kriptografis, verifikasi identitas, otorisasi,
dan pemeriksaan integritas dokumen akhir tetap harus ditangani oleh backend.

## Struktur project dan test

| Path | Isi |
|---|---|
| `psdk-core` | Library Android utama |
| `psdk-pdfium` | Engine Pdfium opsional |
| `sample` | Aplikasi demo |
| `parity/` | Fixture perbandingan dengan web SDK |

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

## Lisensi

[MIT](LICENSE)
