# PdfSignatureSDK for Android

## Demo TTD Cepat 1.0

Try the [TTD Cepat demo release](https://github.com/FvorY/pdf-signature-sdk-ios/releases/tag/demo-ttd-cepat-1.0): draw a signature, open a PDF, place signatures, stamps or E-Materai, and share the resulting PDF. This is a preview demo, not a stable SDK 1.0 release.

- **Android:** download `TTD-Cepat-Android.apk` from the demo release Assets. It is a debug build. Android SDK source is in this repository.
- **iOS:** download `TTD-Cepat-iOS-unsigned.ipa` from the same release and follow its signing and installation instructions. Swift source is in [pdf-signature-sdk-ios](https://github.com/FvorY/pdf-signature-sdk-ios).

The demo binaries are hosted together in the iOS repository release. GitHub automatically generates `Source code (zip)` and `Source code (tar.gz)` from that repository tag, so those archives contain only iOS source. Android source is available here. The automatic archives cannot be removed individually from release Assets.


[![CI](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/FvorY/pdf-signature-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/FvorY/pdf-signature-sdk-android.svg)](https://jitpack.io/#FvorY/pdf-signature-sdk-android)
![Android API 24+](https://img.shields.io/badge/Android-API%2024%2B-blue)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

Native Android PDF viewer for placing **signatures** and **E-Materai** on a document. Users can
add, drag, resize, lock, and remove items. The SDK returns positions in PDF points, so the same
payload can be sent to the backend used by the web and iOS SDKs.

The Android and iOS SDKs expose the same core concepts: PDF source, item placement, events,
coordinates, JSON/Codable output, and backend signing. Only the UI integration is platform-specific.

> Preview: the viewer and sample are usable, but the public API may still change before 1.0.0.

Languages and full API documentation:

- [Bahasa Indonesia](README.id.md)
- [English documentation](DOCUMENTATION.en.md)
- [Dokumentasi Bahasa Indonesia](DOCUMENTATION.id.md)

## What it provides

- PDF rendering with `PdfRenderer`, pagination, thumbnails, pinch-zoom, and fit-to-width.
- Signature and E-Materai placement with drag, resize, delete, and per-item locks.
- Kotlin API, events, `StateFlow`, and JSON output compatible with the web SDK.
- Jetpack Compose and classic XML/View integration.
- Built-in toolbar for phones and tablets (upload, add, clear, pages, zoom, theme), page indicator,
  thumbnails, status line and empty state, the same as the iOS SDK.
- Light, dark, and system themes, custom labels, picker options, skeleton loading, and `FLAG_SECURE`.
- Optional `psdk-pdfium` module for password-protected PDFs on Android 14 and lower.

## Requirements

| Component | Requirement |
|---|---|
| Android | minSdk 24 / Android 7.0+ |
| compileSdk | 35+ |
| Kotlin | 2.1+; `psdk-pdfium` requires Kotlin 2.2+ |
| UI | Jetpack Compose or XML/View |

`psdk-pdfium` adds about 18 MB of native libraries to a universal APK. Prefer an Android App
Bundle or ABI splits for production builds.

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

Add the core SDK to the application module:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<version>")
}
```

For password-protected PDFs on Android 14 and lower, also add:

```kotlin
implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<version>")
```

Replace `<version>` with the tag or commit you want to use. There is currently no stable 1.0.0
release; JitPack can build a commit hash while the API is in preview.

## Quick start: Jetpack Compose

In the samples, `uri`, `file`, `bytes` and `context` stand for your own values; everything else is
SDK API.

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

`rememberPdfSignatureViewer` keeps the viewer in a `ViewModel` during configuration changes. To
load a document later, create it without `file` and call `viewer.loadDocument(...)`.

## Quick start: XML/View

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

The host must be a `ComponentActivity` or subclass such as `AppCompatActivity`. With XML, a
rotation creates a new viewer; save `getSignatures()` yourself if positions must be restored.

## PDF sources

```kotlin
PdfSource.FileSource(file)
PdfSource.UriSource(uri)
PdfSource.UrlSource("https://example.com/document.pdf")
PdfSource.BytesSource(pdfBytes)
PdfSource.AssetSource("documents/example.pdf")
```

`UrlSource` also accepts request headers. Remote PDF and image URLs require HTTPS by default. The
host app must declare `android.permission.INTERNET` for remote sources. HTTP can be enabled only
explicitly for a trusted legacy endpoint with `PsdkConfig(allowInsecureHttp = true)`.

Downloads are limited to 100 MiB for PDFs and 10 MiB for images; redirects are validated up to five
times.

## Place items and export the result

```kotlin
import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.toJsonString

viewer.once<PsdkEvent.DocumentLoaded> {
    val id = viewer.placeSignature(
        PlaceOptions(
            page = 1,
            x = 72.0,
            y = 680.0,
            label = "Director",
            width = 140.0,
            height = 60.0,
        )
    )

    viewer.placeEStamp(PlaceOptions(page = 1, x = 420.0, y = 700.0))

    val items = viewer.getSignatures()
    val json = items.toJsonString()
    viewer.removeSignature(id)
}
```

Items can be placed only after the document has loaded. Calling `placeSignature` or
`placeEStamp` earlier, for example right after `setConfig`, throws `PsdkException.NotLoaded`.
After `loadDocument(...)` returns, you can place items directly.

Coordinates use PDF points (1/72 inch), with the page's top-left corner as `(0, 0)` and Y
increasing downward. `page` starts at 1. A4 is commonly 595 × 842 points. The item model also
provides `toMap()`, `toJson()`, and `yFromBottom(pageHeight)` for PDF libraries using a bottom-left
origin.

## Events and state

```kotlin
import io.github.fvory.psdk.event.PsdkEvent

val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
    Log.d("psdk", event.item.toJsonString()) // send this to your backend
}

subscription.cancel()
```

Available events include `DocumentLoaded`, `PageChanged`, `SignaturePlaced`, `EStampPlaced`,
`SignatureMoved`, `SignatureRemoved`, `SignaturesCleared`, and `PasswordRequested`. Compose and
coroutine users can collect `viewer.state` and `viewer.events`.

## Configuration and security

`PsdkConfig` controls theme, labels, picker options, initial zoom, item locks, password UI,
custom colors, and `secureScreen`. Set `secureScreen = true` to apply `FLAG_SECURE` while the
viewer is shown, preventing screenshots, screen recording, and recent-app previews.

The SDK only returns placement data. Cryptographic signing, identity verification, authorization,
and final document integrity checks must be handled by the backend.

## Project layout and tests

| Path | Contents |
|---|---|
| `psdk-core` | Core Android library |
| `psdk-pdfium` | Optional Pdfium engine |
| `sample` | Demo application |
| `parity/` | Fixtures shared with web SDK comparisons |

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

## License

[MIT](LICENSE)
