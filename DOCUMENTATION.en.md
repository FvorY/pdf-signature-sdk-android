# PdfSignatureSDK for Android — Guide

The SDK shows a PDF and lets users place **signatures** and **E-Materai** on it. It returns the
positions in PDF points, in the same JSON format as the web SDK
[`pdf-signature-esign-sdk`](https://www.npmjs.com/package/pdf-signature-esign-sdk) 1.1.2, so one
backend can complete the signing for web, Android and iOS.

The iOS SDK's [guide](https://github.com/FvorY/pdf-signature-sdk-ios/blob/main/DOCUMENTATION.en.md)
has the same sections. Bahasa Indonesia: [DOCUMENTATION.id.md](DOCUMENTATION.id.md).

> **About the code samples.** Everything you see is SDK API except values that stand for your own
> data: `uri`, `file`, `bytes`, `context` and resource names such as `R.drawable.sig_director`.
> The code is the same in the English and Indonesian guides; only comments and texts in quotes differ.

1. [Requirements and installation](#1-requirements-and-installation)
2. [Showing the viewer](#2-showing-the-viewer)
3. [PDF sources](#3-pdf-sources)
4. [Placing items](#4-placing-items)
5. [Editing on screen](#5-editing-on-screen)
6. [Reading the result](#6-reading-the-result)
7. [Events and state](#7-events-and-state)
8. [Pages and zoom](#8-pages-and-zoom)
9. [Password-protected PDFs](#9-password-protected-pdfs)
10. [Built-in UI](#10-built-in-ui)
11. [Theme, colors, font and labels](#11-theme-colors-font-and-labels)
12. [Upload button](#12-upload-button)
13. [Loading overlay](#13-loading-overlay)
14. [Page images](#14-page-images)
15. [Screen protection](#15-screen-protection)
16. [Errors](#16-errors)
17. [Lifecycle and restoring state](#17-lifecycle-and-restoring-state)
18. [All options](#18-all-options)
19. [Platform differences](#19-platform-differences)
20. [Building from source](#20-building-from-source)

## 1. Requirements and installation

| | Minimum | Notes |
|---|---|---|
| Android | minSdk 24 (7.0) | |
| compileSdk | 35 | |
| Android Gradle Plugin | 8.6 | 8.9.1+ recommended. On 8.6–8.8 add `lint { disable += "NullSafeMutableLiveData" }`: lint there crashes on a check shipped with AndroidX Lifecycle 2.10. |
| Kotlin | 2.1 | `psdk-pdfium` needs Kotlin 2.2 (its Pdfium library is built with Kotlin 2.3). |
| JVM target | 1.8 | Apps compiled for 1.8, 11 or 17 all work. |

These combinations are built in CI on every change (`compat/`).

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

and the SDK to the app module:

```kotlin
dependencies {
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-core:<version>")
    // Optional, see §9: password-protected PDFs on devices whose system cannot open them
    implementation("com.github.FvorY.pdf-signature-sdk-android:psdk-pdfium:<version>")
}
```

`<version>` is a tag or a commit hash (the SDK is in preview, there is no 1.0.0 tag yet).
Remote URLs need `<uses-permission android:name="android.permission.INTERNET" />` in the app.

`psdk-pdfium` adds about 18 MB of native libraries for four ABIs to a universal APK. Publish an
Android App Bundle or use ABI splits so each device downloads only its own (about 4–5 MB).

## 2. Showing the viewer

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

`rememberPdfSignatureViewer(config, key)` keeps the viewer across rotation and restores it after
Android ends the app in the background (§17). `config` is only used the first time; change it later
with `viewer.updateConfig`. Use different `key`s for several viewers on one screen.

To open a document later, create the viewer without `file`:

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

The host must be a `ComponentActivity` (for example `AppCompatActivity`). The view follows the
host's lifecycle. With XML, rotation creates a new viewer; keep `getSignatures()` yourself if the
positions must survive it.

## 3. PDF sources

```kotlin
PdfSource.FileSource(file)
PdfSource.UriSource(uri)                       // content:// from the file picker
PdfSource.UrlSource("https://example.com/a.pdf", headers = mapOf("Authorization" to "Bearer …"))
PdfSource.BytesSource(bytes)
PdfSource.AssetSource("documents/example.pdf")  // app assets
```

Remote PDFs and images must use HTTPS. `PsdkConfig(allowInsecureHttp = true)` allows HTTP for a
trusted legacy server only. Downloads follow at most five validated redirects, send headers only to
the original origin, and are limited to 100 MiB (PDF) and 10 MiB (image).

## 4. Placing items

```kotlin
val signatureId = viewer.placeSignature(
    PlaceOptions(
        page = 1, x = 72.0, y = 680.0, label = "Director",
        image = PsdkImage.Url("https://example.com/signature.png"), width = 140.0,
    )
)
val stampId = viewer.placeEStamp(PlaceOptions(page = 1, x = 420.0, y = 700.0))
```

| `PlaceOptions` | Meaning | Default |
|---|---|---|
| `page` | Page, starting at 1 | current page |
| `x`, `y` | Top-left corner in PDF points | centered, unless both are given |
| `width`, `height` | Size in PDF points | 160 × 48 (signature), 80 × 80 (E-Materai) |
| `label` | Signature text | `"Signature"`; E-Materai is always `"E-Materai"` |
| `image` | `PsdkImage.Url`, `Base64`, `BitmapImage`, `Resource`, or `PsdkImage.of(string)` | none |
| `disableDrag`, `disableResize`, `disableDragging`, `locked` | Locks for this item (§5) | `false` |

Without `height`, an item with an image takes the image's aspect ratio once the image has loaded
(a `SignatureMoved` event reports the new size), like the web SDK.

**Coordinates**: PDF points (1/72 inch), origin at the page's top-left corner as displayed (after
/Rotate), Y down. A4 is about 595 × 842. `getPageSize(page)` returns each page's size.

The add buttons and pickers of the built-in UI are described in §10.

### Starting position from the add buttons

By default the add buttons and pickers (and `openSignatureModal()` / `openEStampModal()`) put a new
item in the center of the page, and the user drags it. Set `defaultPosition` to place it at a fixed
position instead, with no dragging needed:

```kotlin
PsdkConfig(defaultPosition = PlacementPosition(x = 10.0, y = 20.0)) // top-left corner, PDF points
// or later: viewer.updateConfig { it.copy(defaultPosition = PlacementPosition(10.0, 20.0)) }
```

- `x`, `y` use the same coordinates as `PlaceOptions` (top-left origin, Y down, PDF points).
- The item is kept inside the page: a position beyond the page edge is pulled back so the whole item fits.
- It applies to the current page, to signatures and E-Materai, and to items chosen from a picker.
- `null` (default) keeps the centered placement.
- `placeSignature` / `placeEStamp` are not affected: their own `x`, `y` always win.

### Ask for the position first

With `askPositionOnAdd` on, tapping an add button (and choosing a role or option, if there is a picker)
opens a built-in dialog before the item is placed. The dialog has an **X** and a **Y** field (PDF points,
top-left origin) and starts empty:

```kotlin
PsdkConfig(askPositionOnAdd = true)
// or later: viewer.updateConfig { it.copy(askPositionOnAdd = true) }
```

- Both fields filled: the item is placed at that position, kept inside the page.
- Both empty: the item is placed at `defaultPosition`, or at the center of the page when that is not set.
  Use this when the user does not want to type coordinates.
- Only one filled, or text that is not a number: "Place" stays disabled and the dialog explains why.
- Cancel (or tapping outside): nothing is added.
- Off by default: the item is placed right away. `placeSignature` / `placeEStamp` never show the dialog.
- Texts: `PsdkLabels.positionModalTitle`, `positionModalDesc`, `positionX`, `positionY`, `positionPageSize`, `positionInvalid` and `placeBtn`.

## 5. Editing on screen

- Drag an item to move it; drag the handle on its bottom-right corner to resize it (at least
  20 pt); tap the button on its top-right corner to delete it. Items stay inside their page.
- Locks follow the web SDK: `disableDrag` stops moving, `disableResize` stops resizing,
  `disableDragging` and `locked` stop both. The delete button works even on locked items; only
  `disabled` turns it off. The same locks exist for all items in `PsdkConfig`.
- `estampKeepAspectRatio = true` keeps an E-Materai's width/height ratio while resizing.
- `ui.haptics` gives a short vibration when an item starts moving or is deleted.
- TalkBack reads each item ("Signature, page 1") and offers move, bigger/smaller and delete actions.

## 6. Reading the result

```kotlin
val items: List<SignatureItem> = viewer.getSignatures()
val onPage2 = viewer.getSignaturesByPage(2)
val json: String = items.toJsonString()   // identical to JSON.stringify in the web SDK
val array: JSONArray = items.toJsonArray()
```

```json
{"id":"signature-1760600000000-k3x9a","type":"signature","x":72,"y":680,"page":1,"label":"Director",
 "width":140,"height":48,"disableDrag":false,"disableDragging":false,"disableResize":false,"locked":false}
```

`SignatureItem` also has `toMap()`, `toJson()` and `yFromBottom(pageHeight)` for PDF libraries
that measure from the bottom-left corner. `removeSignature(id)` and `clearSignatures()` remove items.

## 7. Events and state

```kotlin
val subscription = viewer.on<PsdkEvent.SignaturePlaced> { event ->
    Log.d("psdk", event.item.toJsonString()) // send this to your backend
}
viewer.once<PsdkEvent.DocumentLoaded> { println("${it.totalPages} pages") }
subscription.cancel() // or viewer.off(subscription)
```

| Event | Data | When |
|---|---|---|
| `DocumentLoaded` | `totalPages` | a document opened |
| `PageChanged` | `page`, `total`, `source` (`SCROLL` or `API`) | the current page changed |
| `SignaturePlaced` / `EStampPlaced` | `item` | an item was placed |
| `SignatureMoved` | `item` | an item was moved or resized (also after an image sets its height) |
| `SignatureRemoved` | `id` | an item was removed |
| `SignaturesCleared` | – | all items were removed, also when a new document opens |
| `PasswordRequested` | `isIncorrect`, `updatePassword()`, `cancel()` | a password is needed (§9) |

Listeners run on the main thread. With coroutines, collect `viewer.events` (`SharedFlow`) or
`viewer.state` (`StateFlow<PsdkState>`: `isLoading`, `isLoaded`, `currentPage`, `totalPages`,
`scale`, `isPaginationLocked`, `isPasswordProtected`, `pageSizes`, `signatures`, `error`).

## 8. Pages and zoom

```kotlin
viewer.nextPage(); viewer.prevPage(); viewer.goToPage(3) // false when not possible
viewer.setPaginationLocked(true) // blocks buttons, page input, thumbnails and goToPage
viewer.setScale(1.5f)            // 0.25–4.0 (suspend)
viewer.fitToScreen(); viewer.fitToWidth()
```

Without `PsdkConfig(scale = …)` the widest page fits the screen width. Users can pinch and
double-tap to zoom (`ui.viewer.zoom = false` turns that off). `lockScrollWhenPaginationLocked =
true` also blocks scrolling while pagination is locked (off by default, like the web SDK).

## 9. Password-protected PDFs

`loadDocument` waits until a password opens the document, and throws
`PsdkException.PasswordCancelled` when the prompt is cancelled. The built-in dialog
(`ui.passwordDialog`, on by default) asks for the password; the `PasswordRequested` event is sent
either way, so you can show your own dialog instead:

```kotlin
val viewer = rememberPdfSignatureViewer(PsdkConfig(ui = PsdkUiConfig(passwordDialog = false)))
viewer.on<PsdkEvent.PasswordRequested> { event ->
    // Show your own dialog (event.isIncorrect is true after a wrong password), then answer it:
    event.updatePassword("test123") // the text the user typed
    // event.cancel()               // or the user gave up
}
```

Passwords are never logged, sent in events or saved. Android 15+ opens encrypted PDFs itself;
Android 12–14 need the S extension 13 system update; other devices need `psdk-pdfium`.
`state.isPasswordProtected` (and the 🔒 badge) stay on while an encrypted document is open.

## 10. Built-in UI

The viewer has its own UI around the pages, the same on Android and iOS:

| Part | Phone (< 600 dp wide) | Tablet (≥ 600 dp) |
|---|---|---|
| Top bar | brand and a ⋮ menu (zoom, fit, theme, go to page) | brand, upload, add signature, E-Materai, clear, pages, zoom, theme |
| Bottom bar | upload, add signature, E-Materai, clear (scrolls sideways when labels are long) | – |
| Page indicator | floating "‹ 1 / 5 ›"; tap the number to go to a page | in the top bar |
| Thumbnails | strip above the bottom bar (hidden below 560 dp height) | sidebar |
| Status line | "Ready · 2 signatures", 🔒 badge, zoom (hidden below 420 dp height) | same |
| Empty state | title, text, upload button, and the error when loading failed | same |

`ui.layout` forces `PHONE` or `TABLET`. Every part can be turned off:

```kotlin
PsdkConfig(
    ui = PsdkUiConfig(
        topbar = TopBarConfig(upload = false, themeToggle = false, brandText = "My App"),
        sidebar = SidebarConfig(thumbnails = false),
        statusbar = false,
    ),
)
```

With every `topbar` option off and no brand, logo or custom content, there is no top bar.
`topbar.customComponent` adds your own composable; `topbar.logo` and `topbar.style` (background,
content color, height, padding) style the bar. `disabled = true` turns off the toolbar buttons and
all item editing.

**Pickers.** The add buttons and `openSignatureModal()` / `openEStampModal()` place a default item,
or show a list when options are given:

```kotlin
PsdkConfig(
    signatureOptions = listOf(
        PickerOption("director", "Director", PsdkImage.Resource(R.drawable.sig_director), group = "Internal"),
        PickerOption("client", "Client", group = "External"),
    ),
    estampOptions = listOf(PickerOption("stamp-10000", "E-Materai 10,000")),
    groupByCategory = true, // headings per group; options without one go under labels.defaultGroup
)
```

## 11. Theme, colors, font and labels

```kotlin
PsdkConfig(
    theme = PsdkThemeMode.SYSTEM, // LIGHT (default), DARK or SYSTEM
    lightColors = PsdkColors.light().copy(accent = Color(0xFF0057B8)),
    darkColors = PsdkColors.dark(),
    fontFamily = FontFamily(Font(R.font.inter)),
    labels = PsdkLabels.indonesian().copy(uploadBtn = "Pilih PDF"),
)
```

- `PsdkColors.light()` / `dark()` are the web colors adjusted for WCAG AA contrast;
  `webLight()` / `webDark()` are the web colors unchanged. There are 18 tokens (`background`,
  `surface`, `accent`, `danger`, `textPrimary`, …). Pages are always white.
- The theme button switches between light and dark without sending an event.
- `PsdkLabels` holds every text (English by default, `PsdkLabels.indonesian()` for Bahasa
  Indonesia), including TalkBack texts. Item labels in the result stay `"Signature"` and
  `"E-Materai"` in every language.

## 12. Upload button

The upload button (`topbar.upload`) opens the system file picker for PDFs. `onUpload` decides what
happens with the chosen file:

```kotlin
PsdkConfig(onUpload = { picked ->
    // Example: only accept files up to 20 MB (`context` is your Context)
    val uri = (picked as? PdfSource.UriSource)?.uri
    val size = uri?.let { context.contentResolver.openAssetFileDescriptor(it, "r")?.use { d -> d.length } }
    if (size != null && size in 0..20_000_000) UploadDecision.Proceed else UploadDecision.Cancel
    // or open another document instead: UploadDecision.Replace(PdfSource.AssetSource("other.pdf"))
})
```

## 13. Loading overlay

```kotlin
viewer.showSkeleton(true, SkeletonOptions(backgroundOpacity = 0.3f, blurRadius = 8.dp))
// … send to the backend …
viewer.showSkeleton(false)
```

`SkeletonOptions.content` replaces the spinner with your own composable. Blur needs Android 12+.

## 14. Page images

```kotlin
val page: Bitmap = viewer.renderPage(page = 1, scale = 2f)                // pixels per PDF point
val withItems: Bitmap = viewer.renderPageWithOverlay(page = 1, scale = 2f) // items drawn on top
```

Useful for previews or a confirmation screen. Images follow /Rotate like the viewer.

## 15. Screen protection

`secureScreen = true` sets `FLAG_SECURE` on the window while the viewer is shown: screenshots and
screen recordings are black and the recent-apps preview is hidden. The flag is removed again only
if the SDK added it.

## 16. Errors

`loadDocument`, `placeSignature` / `placeEStamp`, `getPageSize` and `renderPage` throw
`PsdkException`:

| Exception | Meaning |
|---|---|
| `InvalidSource` | file missing or unreadable, or not allowed (e.g. HTTP without `allowInsecureHttp`) |
| `Network` | download failed (with the status code when there is one) |
| `NotAPdf` / `Corrupted` | not a PDF / a damaged PDF |
| `PasswordCancelled` | the password prompt was cancelled |
| `PasswordNotSupported` | encrypted PDF on a device that cannot open it without `psdk-pdfium` |
| `InvalidPage` / `NotLoaded` / `Destroyed` | wrong page number / no document / viewer destroyed |

After a failed load the empty state shows a short message; with `logLevel = PsdkLogLevel.DEBUG` it
also shows the exception, and the SDK logs details to Logcat.

## 17. Lifecycle and restoring state

- Viewers from `rememberPdfSignatureViewer` and `PdfSignatureView` follow the host's lifecycle.
  Call `destroy()` only for a viewer you created yourself.
- `rememberPdfSignatureViewer` survives rotation. After Android ends the app in the background, it
  reopens the document at the same page and zoom with the same items (restored items send no
  `SignaturePlaced` events; passwords are never saved).

## 18. All options

| `PsdkConfig` | Default | See |
|---|---|---|
| `file` | `null` | §3 |
| `allowInsecureHttp` | `false` | §3 |
| `scale` | `null` (fit width) | §8 |
| `theme`, `lightColors`, `darkColors`, `fontFamily` | `LIGHT`, AA colors, system font | §11 |
| `disabled`, `disableDragging`, `disableDrag`, `disableResize` | `false` | §5 |
| `estampKeepAspectRatio` | `false` | §5 |
| `signatureOptions`, `estampOptions`, `groupByCategory` | empty, `false` | §10 |
| `defaultPosition` | `null` (centered) | §4 |
| `askPositionOnAdd` | `false` | §4 |
| `onUpload` | `null` (open the picked file) | §12 |
| `lockScrollWhenPaginationLocked` | `false` | §8 |
| `secureScreen` | `false` | §15 |
| `labels` | English | §11 |
| `ui` | everything on | `topbar`, `sidebar.thumbnails`, `viewer.zoom`, `statusbar`, `passwordDialog`, `layout`, `haptics` (§10) |
| `logLevel` | `ERROR` | `NONE`, `ERROR`, `DEBUG` (§16) |

Change options at runtime with `viewer.updateConfig { it.copy(disabled = true) }`.

## 19. Platform differences

| | Android | iOS |
|---|---|---|
| UI toolkit | Compose and XML/View | UIKit (`PdfSignatureView`) and SwiftUI (`PdfSignatureViewer`) |
| State | `viewer.state` (`StateFlow`) | `PdfSignatureController` (`@Published`) |
| Events | `on<T>`, `events` (`SharedFlow`) | `on(.type)`, `eventsPublisher` (Combine), delegate |
| Screen protection | screenshots and recordings blocked | covered while recording or mirroring; iOS cannot block screenshots |
| Encrypted PDFs | Android 15+ built in, 12–14 with a system update, otherwise `psdk-pdfium` | always built in (PDFKit) |
| Restore after the app is ended | automatic | not needed for rotation; keep `getSignatures()` yourself across launches |
| Custom toolbar content | `customComponent` (composable) | `customView` (`UIView`) |
| Font | `fontFamily` | `typography` (font name and base size) |

Everything else (coordinates, JSON, events, options, built-in UI) is the same.

## 20. Building from source

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

`sample/` is the demo app, `parity/` holds the web SDK comparison fixtures and `compat/` the app
that checks compatibility with older build tools. The SDK is a preview; the public API may still
change before 1.0.0.
