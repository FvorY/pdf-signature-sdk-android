# PdfSignatureSDK untuk Android

[![CI](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/FvorY/pdf-signature-sdk-android.svg)](https://jitpack.io/#FvorY/pdf-signature-sdk-android)
![minSdk 24](https://img.shields.io/badge/minSdk-24-blue)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

PDF viewer native Android (Kotlin). Dengan SDK ini, user bisa menaruh, menggeser,
me-resize, dan menghapus kotak **tanda tangan** dan **E-Materai** di dokumen. Posisi
dikembalikan dalam **PDF point**, sama persis dengan web SDK
[`pdf-signature-esign-sdk`](https://www.npmjs.com/package/pdf-signature-esign-sdk) v1.1.2,
sehingga backend yang sama bisa langsung menyelesaikan proses tanda tangan.

> 🚧 **Preview.** API viewer dan sample sudah dapat digunakan, tetapi API publik masih dalam
> tahap stabilisasi dan belum merupakan rilis 1.0.0.

English: [README.md](README.md)

Dokumentasi: [DOCUMENTATION.id.md](DOCUMENTATION.id.md) · [English documentation](DOCUMENTATION.en.md)

## Fitur

- Render PDF dengan `PdfRenderer` bawaan platform: halaman lazy, pinch-zoom, pagination, thumbnail
- Penempatan tanda tangan dan E-Materai: geser, resize, hapus, dan kunci per item
- Method, event, output JSON, opsi picker, dan skeleton overlay yang sama dengan web SDK
- API Jetpack Compose dan `View` klasik untuk layout XML
- Tema terang, gelap, dan ikut sistem; semua teks UI bisa diganti
- Opsi `secureScreen` untuk memblokir screenshot dan rekam layar
- Modul opsional `psdk-pdfium` untuk membuka PDF berpassword di Android 14 ke bawah
- Dependensi hanya AndroidX dan kotlinx-coroutines

## Persyaratan

- minSdk 24 (Android 7.0)
- Aplikasi pemakai harus compile dengan API 36 atau lebih baru

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

Lalu di `build.gradle.kts` modul aplikasi:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<versi>")
    // Opsional: PDF berpassword di Android 14 ke bawah
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<versi>")
}
```

## Pemakaian

```kotlin
val viewer = rememberPdfSignatureViewer(PsdkConfig(file = PdfSource.UriSource(uri)))

PdfSignatureViewer(viewer, Modifier.fillMaxSize())

// Nanti, kirim posisi ke backend:
val json = viewer.getSignatures().toJsonArray()
```

## Sistem koordinat

- Satuan: PDF point (1/72 inci). Kertas A4 = 595 × 842 pt.
- Titik nol: pojok **kiri-atas** halaman. Sumbu Y mengarah ke bawah.
- `x, y` adalah pojok kiri-atas item, sama dengan web SDK.

## Keamanan sumber remote

URL PDF dan gambar remote wajib menggunakan HTTPS secara default. Untuk endpoint HTTP lama yang
tepercaya saja, aktifkan secara eksplisit dengan `PsdkConfig(allowInsecureHttp = true)`. Redirect
divalidasi maksimal lima kali; ukuran download dibatasi 100 MiB untuk PDF dan 10 MiB untuk gambar.
Aplikasi host harus mendeklarasikan permission `INTERNET` saat menggunakan sumber remote.

## Struktur project

| Modul | Isi |
|---|---|
| `psdk-core` | Library utama |
| `psdk-pdfium` | Engine render Pdfium (opsional) |
| `sample` | Aplikasi demo |
| `parity/` | Fixture untuk membandingkan output dengan web SDK |

## Build & test

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

## Kontribusi

Lihat [CONTRIBUTING.md](CONTRIBUTING.md).

## Lisensi

[MIT](LICENSE)
