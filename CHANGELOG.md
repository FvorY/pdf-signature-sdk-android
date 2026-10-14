# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Viewer UI (M3): phone layout (top bar with ⋮ menu, bottom bar, floating page indicator, thumbnail
  strip) and tablet layout (web-style toolbar, thumbnail sidebar), switching at 600 dp.
- Signature and E-Materai pickers (`signatureOptions`, `estampOptions`, `groupByCategory`),
  `openSignatureModal`, `openEStampModal`.
- Light, dark and system themes with `PsdkColors` mapped from the web SDK CSS variables, `fontFamily`.
- Upload button with the system file picker and `onUpload` (`UploadDecision`).
- `updateConfig`, `showSkeleton`, status bar, "Clear All", 🔒 badge for encrypted PDFs, go-to-page dialog.
- `ui.topbar.*`, `ui.sidebar.thumbnails`, `ui.statusbar`, `ui.layout` options.
- `PdfSignatureView` for XML layouts.
- Signatures and E-Materai (M2): `placeSignature`, `placeEStamp`, `removeSignature`, `clearSignatures`,
  `getSignatures`, `getSignaturesByPage`, `renderPageWithOverlay`; events `signaturePlaced`,
  `eStampPlaced`, `signatureMoved`, `signatureRemoved`.
- Drag, resize and delete on the page with the web SDK's lock rules (`disabled`, `disableDragging`,
  `disableDrag`, `disableResize`, per-item `locked`/`disable*`), plus `estampKeepAspectRatio`.
- Item images from URL, Base64, bitmap or drawable; the height follows the image like the web SDK.
- `SignatureItem.toJsonString()` / `toJsonArray()` with the web SDK's JSON format.
- Web parity fixtures and a headless-Chrome runner for the real web SDK (`parity/`).
- `PdfSignatureViewer` (M1): `loadDocument` from file, `Uri`, URL (with headers), bytes or asset;
  `nextPage`/`prevPage`/`goToPage`, `setPaginationLocked`, `setScale`, `fitToScreen`, `fitToWidth`,
  `getPageSize`, `renderPage`, `on`/`once`/`off`, `events` flow, `state` flow, `destroy`.
- Events `documentLoaded`, `pageChanged`, `signaturesCleared` with the web SDK's order and rules.
- Compose UI: lazy page list, horizontal scroll for wide pages, pinch-to-zoom, double-tap zoom,
  empty state. `rememberPdfSignatureViewer` keeps the viewer across rotation.
- `lockScrollWhenPaginationLocked` option: also blocks scrolling while pagination is locked (off by default, like the web SDK).
- A short last page (e.g. landscape) becomes the active page when the list is scrolled to the end.
- The page list keeps its scroll position across rotation.
- `PsdkLabels` with English defaults and an Indonesian preset.
- Sample app with a generated multi-page test PDF (`scripts/make_sample_pdf.py`).
- Gradle project with `psdk-core`, optional `psdk-pdfium` and `sample` modules
  (minSdk 24, compileSdk 36, AGP 9, Kotlin 2.4, Compose).
- Maven publication for JitPack (`jitpack.yml`).
- ktlint, unit test setup.
- GitHub Actions: lint, unit tests, build, local publish, tag release.
