# PdfSignatureSDK untuk Android

PDF viewer native Android (Kotlin). Dengan SDK ini, user bisa menaruh, menggeser,
me-resize, dan menghapus kotak **tanda tangan** dan **E-Materai** di dokumen. Posisi
dikembalikan dalam **PDF point**, sama persis dengan web SDK
[`pdf-signature-esign-sdk`](https://www.npmjs.com/package/pdf-signature-esign-sdk) v1.1.2,
sehingga backend yang sama bisa langsung menyelesaikan proses tanda tangan.

> 🚧 **Preview.** API viewer dan sample sudah dapat digunakan, tetapi API publik masih dalam
> tahap stabilisasi dan belum merupakan rilis 1.0.0.

English: [README.md](README.md)

## Instalasi

Tambahkan `maven("https://jitpack.io")` di `settings.gradle.kts`, lalu:

```kotlin
implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<versi>")
// Opsional: PDF berpassword di Android 14 ke bawah
implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<versi>")
```

Aplikasi pemakai harus memakai `compileSdk` 36 atau lebih baru. `minSdk` 24.

## Sistem koordinat

- Satuan: PDF point (1/72 inci). Kertas A4 = 595 × 842 pt.
- Titik nol: pojok **kiri-atas** halaman. Sumbu Y mengarah ke bawah.
- `x, y` adalah pojok kiri-atas item.

## Build & test

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

## Keamanan sumber remote

URL PDF dan gambar remote wajib menggunakan HTTPS secara default. Untuk endpoint HTTP lama yang
tepercaya saja, aktifkan secara eksplisit dengan `PsdkConfig(allowInsecureHttp = true)`. Redirect
divalidasi maksimal lima kali; ukuran download dibatasi 100 MiB untuk PDF dan 10 MiB untuk gambar.
Aplikasi host harus mendeklarasikan permission `INTERNET` saat menggunakan sumber remote.

## Lisensi

[MIT](LICENSE)
