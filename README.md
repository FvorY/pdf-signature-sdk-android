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

> 🚧 **Work in progress.** The project skeleton is in place (milestone M0). The viewer API
> described below lands in the next milestones and is not usable yet.

Bahasa Indonesia: [README.id.md](README.id.md)

## Features (planned for 1.0.0)

- PDF rendering with the platform `PdfRenderer`: lazy pages, pinch-zoom, pagination, thumbnails
- Signature and E-Materai placement with drag, resize, delete and per-item locks
- Same methods, events and JSON output as the web SDK
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

## Usage (preview of the 1.0.0 API)

```kotlin
val viewer = rememberPdfSignatureViewer(PsdkConfig(file = PdfSource.UriSource(uri)))

PdfSignatureViewer(viewer, Modifier.fillMaxSize())

// Later, send the positions to your backend:
val json = viewer.getSignatures().toJsonArray()
```

Coordinates use the top-left corner of the page as origin, in PDF points
(1/72 inch), and `x, y` is the top-left corner of the item, the same as the web SDK.

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
