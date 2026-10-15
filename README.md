# PdfSignatureSDK for Android

[![CI](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/FvorY/pdf-signature-sdk-android.svg)](https://jitpack.io/#FvorY/pdf-signature-sdk-android)
![minSdk 24](https://img.shields.io/badge/minSdk-24-blue)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

Native Android (Kotlin) PDF viewer that lets users place, drag, resize and remove
**signature** and **E-Materai** boxes on a document. Positions are returned in
**PDF points**, exactly like the web SDK
[`pdf-signature-esign-sdk`](https://www.npmjs.com/package/pdf-signature-esign-sdk) v1.1.2,
so the same backend can complete the signing.

> 🚧 **Preview.** The viewer API and sample are usable and the public API is under stabilization;
> it is not yet a 1.0.0 release.

Bahasa Indonesia: [README.id.md](README.id.md)

Documentation: [DOCUMENTATION.en.md](DOCUMENTATION.en.md) · [Dokumentasi Bahasa Indonesia](DOCUMENTATION.id.md)

## Features

- PDF rendering with the platform `PdfRenderer`: lazy pages, pinch-zoom, pagination, thumbnails
- Signature and E-Materai placement with drag, resize, delete and per-item locks
- Web-parity methods, events, JSON output, picker options and skeleton overlay
- Jetpack Compose API plus a classic `View` for XML layouts
- Light, dark and system themes; every UI string can be replaced
- Optional `psdk-pdfium` module that opens password-protected PDFs on Android 14 and lower
- Only AndroidX and kotlinx-coroutines as dependencies

## Requirements

- minSdk 24 (Android 7.0)
- Apps must compile against API 36 or newer

## Installation

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then in your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<version>")
    // Optional: password-protected PDFs on Android 14 and lower
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<version>")
}
```

## Usage

```kotlin
val viewer = rememberPdfSignatureViewer(PsdkConfig(file = PdfSource.UriSource(uri)))

PdfSignatureViewer(viewer, Modifier.fillMaxSize())

// Later, send the positions to your backend:
val json = viewer.getSignatures().toJsonArray()
```

Coordinates use the top-left corner of the page as origin, in PDF points
(1/72 inch), and `x, y` is the top-left corner of the item, the same as the web SDK.

### Remote source security

Remote PDF and image URLs require HTTPS by default. For a trusted legacy HTTP endpoint only,
opt in explicitly with `PsdkConfig(allowInsecureHttp = true)`. Downloads follow at most five
validated redirects and are limited to 100 MiB for PDFs and 10 MiB for images. The host app must
declare the `INTERNET` permission when using remote sources.

## Project layout

| Module | Contents |
|---|---|
| `psdk-core` | The library |
| `psdk-pdfium` | Optional Pdfium rendering engine |
| `sample` | Demo app |
| `parity/` | Fixtures that compare output with the web SDK |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE)
