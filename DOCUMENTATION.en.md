# PdfSignatureSDK Android Documentation

This document covers installation and day-to-day use of the Android SDK. The SDK provides a PDF
viewer where users can place signatures and E-Materai. Placement data can be sent to a backend in
the same format used by the web SDK.

## Features

- Open PDFs from a `File`, `Uri`, URL, byte array, or the app's `assets` folder.
- PDF rendering with pagination, thumbnails, pinch-zoom, and fit-to-screen.
- Add, move, resize, and remove signature or E-Materai items.
- Kotlin API, event listeners, `StateFlow`, and JSON output.
- Jetpack Compose API and classic XML/View integration.
- Theme, color, label, picker, item-lock, skeleton, and `FLAG_SECURE` options.
- Optional Pdfium module for password-protected PDFs on Android 14 and lower.

## Requirements

- Android 7.0 / API 24 or newer.
- `compileSdk` 36 or newer.
- Kotlin and AndroidX.
- `INTERNET` permission for PDF or image URLs.

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

Add the dependency to the application module:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<version>")

    // Optional: password-protected PDFs on Android 14 and lower.
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<version>")
}
```

Replace `<version>` with the tag or commit you want to use. For unreleased versions, JitPack can
also build a commit hash.

## Jetpack Compose

`rememberPdfSignatureViewer` keeps the viewer in a `ViewModel`, so the document and item positions
survive rotation and process restoration.

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

If the viewer is created without a file, load a document later:

```kotlin
val viewer = rememberPdfSignatureViewer()

LaunchedEffect(pdfUri) {
    viewer.loadDocument(PdfSource.UriSource(pdfUri))
}
```

## XML or classic View

Add the view to a layout:

```xml
<io.github.fvory.psdk.PdfSignatureView
    android:id="@+id/pdfSignatureView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Configure it from an Activity or Fragment:

```kotlin
val pdfView = findViewById<PdfSignatureView>(R.id.pdfSignatureView)
pdfView.setConfig(PsdkConfig(file = PdfSource.UriSource(uri)))

val viewer = pdfView.viewer
```

The host must be a `ComponentActivity` or one of its subclasses, such as `AppCompatActivity`.
With XML, rotation creates a new viewer; save `getSignatures()` yourself if item positions must
be restored.

## PDF sources

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

HTTPS is required for URLs by default. HTTP requires an explicit opt-in:

```kotlin
val config = PsdkConfig(allowInsecureHttp = true)
```

Use this only for a trusted legacy endpoint. PDF downloads are limited to 100 MiB, images to
10 MiB, and redirects are validated up to five times.

## Placing items from code

```kotlin
val signatureId = viewer.placeSignature(
    PlaceOptions(
        page = 1,
        x = 72.0,
        y = 680.0,
        label = "Director",
        image = PsdkImage.Url("https://example.com/signature.png"),
        width = 140.0,
        height = 60.0,
    )
)

val estampId = viewer.placeEStamp(
    PlaceOptions(page = 1, x = 420.0, y = 700.0)
)
```

`page` is one-based. `x` and `y` use the page's top-left corner as the origin, in PDF points
(1/72 inch). A4 is commonly 595 × 842 points. Missing `x`, `y`, `width`, or `height` values are
chosen by the SDK.

Read or remove items after placement:

```kotlin
viewer.removeSignature(signatureId)
val allItems = viewer.getSignatures()
val pageItems = viewer.getSignaturesByPage(1)
val json = allItems.toJsonString()
```

`SignatureItem` also provides `toMap()`, `toJson()`, and `yFromBottom(pageHeight)`. The SDK stores
`y` from the top, while some PDF libraries use bottom-origin coordinates.

## Signature and E-Materai pickers

When `signatureOptions` or `estampOptions` is empty, the button places the default item directly.
To show a selection list, provide `PickerOption` values:

```kotlin
val config = PsdkConfig(
    signatureOptions = listOf(
        PickerOption(
            id = "director",
            label = "Director",
            image = PsdkImage.Resource(R.drawable.signature_director),
        )
    ),
    estampOptions = listOf(
        PickerOption(
            id = "stamp-10000",
            label = "E-Materai 10,000",
            image = PsdkImage.Resource(R.drawable.materai_10000),
        )
    ),
)
```

Available image types are `PsdkImage.Resource`, `BitmapImage`, `Url`, and `Base64`.
`PsdkImage.of(value)` can classify a string as a URL or Base64 data.

## Events and state

Listeners run on the main thread and return a cancellable subscription:

```kotlin
val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
    val item = event.item
    sendToBackend(item.toJson())
}

// When the screen is finished:
subscription.cancel()
```

Available events include `DocumentLoaded`, `PageChanged`, `SignaturePlaced`, `EStampPlaced`,
`SignatureMoved`, `SignatureRemoved`, `SignaturesCleared`, and `PasswordRequested`.

In Compose, observe `viewer.state`:

```kotlin
val state by viewer.state.collectAsState()
Text("Page ${state.currentPage} of ${state.totalPages}")
```

Coroutine users can also collect `viewer.events`, a `SharedFlow<PsdkEvent>`.

## Navigation and zoom

```kotlin
viewer.nextPage()
viewer.prevPage()
viewer.goToPage(2)
viewer.setPaginationLocked(true)

viewer.setScale(1.5f)
viewer.fitToScreen()
viewer.fitToWidth()
```

`currentPage` starts at 1. `totalPages` is 0 until a document has loaded. Supported zoom is
0.25 to 4.0.

## Password-protected PDFs

The built-in password dialog is enabled by default. To handle the prompt in the host app:

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

Passwords are not included in events, logs, or saved state. The `psdk-pdfium` module is required
for password-protected PDFs on Android 14 and lower.

## Configuration

`PsdkConfig` controls the theme, colors, labels, font, initial scale, and interaction behavior:

```kotlin
val config = PsdkConfig(
    theme = PsdkThemeMode.SYSTEM,
    scale = 1.25f,
    disabled = false,
    disableDragging = true,
    disableResize = true,
    secureScreen = true,
    labels = PsdkLabels(
        signatureBtn = "Add signature",
        estampBtn = "Add E-Materai",
    ),
)
```

`secureScreen = true` sets `FLAG_SECURE` on the window to prevent screenshots, screen recording,
and the recent-apps preview while the viewer is shown.

## Lifecycle

Call `destroy()` when a manually created viewer is no longer needed. Viewers created by
`rememberPdfSignatureViewer` and `PdfSignatureView` follow the host lifecycle.

## Build and test from source

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

The demo app is in the `sample` module. Parity fixtures for comparison with the web SDK are in
the `parity/` folder.

## API status

This project is currently a preview. The public API may change before the 1.0.0 release.

