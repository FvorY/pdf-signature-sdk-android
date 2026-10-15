# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Sharp pages at high zoom (M4): when the whole-page bitmap hits its 4096 px cap, the visible part
  is rendered again at full resolution with `renderRegion` (PdfRenderer and Pdfium) once scrolling
  settles. Instrumented tests check that regions match whole-page renders on rotated and cropped pages.
- Optional `psdk-pdfium` engine (io.legere:pdfiumandroid, Apache-2.0) for password-protected PDFs
  where the platform renderer cannot open them. It runs on its own thread, waits for renders
  before closing, uses Pdfium's rotated page sizes, and keeps its entry point under R8.
- Geometry parity fixtures (M4): `parity/pdfs/geometry.pdf` with /Rotate 0/90/180/270, CropBox
  offsets and a MediaBox not at 0,0, plus scenarios limited with `"only"`. Page sizes and item
  positions match the web SDK on a device (PdfRenderer honours CropBox and /Rotate like pdf.js).
- Process-death restore (M4): `rememberPdfSignatureViewer` saves a small snapshot (document copy
  path, page, zoom, scroll, page lock, items) and item images as PNG files in the cache, so the
  viewer comes back as it was after Android kills the app in the background. Restored items send
  no `signaturePlaced` events; passwords are never saved.
- Password-protected PDFs (M4): `passwordRequested` event with `updatePassword` / `cancel`, a
  built-in password dialog (`ui.passwordDialog`), and `PsdkException.PasswordCancelled`.
  Android 15+ uses `PdfRenderer` with `LoadParams`; Android 12–14 use `PdfRendererPreV` when the
  device has the S extension 13 system update. Tested with AES-256 and RC4-128 files.

### Changed
- Whole-page bitmaps are capped at 4 MP / 3072 px (was 16 MP / 4096 px); sharpness above that
  comes from the visible-region detail. A page now takes at most 16 MB instead of 64 MB.
- A page is rendered only after it stays on screen for 60 ms, so pages flung past are skipped.
- Measured on a 1080p phone with a 300-page PDF: no crash or out-of-memory while scrolling to the
  end and zooming to 400%; after 10 s idle the app is back to about 155 MB PSS.
- Zooming with the buttons, menu or `setScale` centers the content horizontally. Before, a page
  narrower than the widest page drifted off screen at high zoom (blank view from about 325%).
- The page shadow and the disabled fade are drawn directly instead of through page-sized layers.
- Remote PDF and image loading is HTTPS-only by default, with opt-in legacy HTTP, validated
  redirects, sanitized URL errors and bounded download sizes. Request headers are only sent to
  the original origin, including after same-origin redirects.
- The 🔒 badge only shows while an encrypted document is open, and a cancelled password prompt
  returns to the normal empty state. Exception messages in the empty state are only shown with
  `logLevel = DEBUG`.
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
