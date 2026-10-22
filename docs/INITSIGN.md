# ezSign /signing/initsign bridge

Checked against the read-only Flutter application:
- lib/utils/specimen_coordinates.dart: previewSpecimenToPage / resizePdfView.
- lib/views/document/authenticate_screen.dart: preview-to-PDF conversion.
- lib/views/document/otp_pin_screen.dart: _createSignature and initSign request.
- lib/utils/helpers.dart: backend specimen types.

SDK items use PDF point coordinates from the displayed page's top-left. Flutter stores a
center-relative preview position, then converts it to PDF points from the bottom-left.
The new exporter bypasses the preview representation and exports the equivalent PDF rectangle.
It does not multiply by zoom, screen density, bitmap size, or the item's scale.

For exact page dimensions W,H and SDK rectangle x,y,w,h:
    backend = (x, H-y-h, w, h)
    web = (x, y, w, h)
The item page is already 1-based. Do not add 1 again.
`docWidth/docHeight` in this exporter are the page point dimensions, not the item dimensions.
The Flutter application retains preview dimensions in these fields; its final PDF x/y/w/h
have already been converted and are the values compared in the parity tests.
`documentSha256` is local audit metadata and is omitted from the JSON request.
SDK `estamp` maps to Flutter/backend `emeterai`.

All backend specimen IDs must be bound explicitly by SDK item ID. The demo's label or generated
item ID is not a backend specimen ID. A demo image also is not automatically the same image as
the selected backend specimen: use the image returned for that specimen in the SDK preview.
This bridge covers direct single-document signature/E-Materai placement. Recipient placeholders,
approval IDs, seals, initial signatures and bulk signing still use their existing app flows.

Export takes the original loaded PDF, not a separately supplied Base64 string. Keep the returned
base64Pdf and signatureData together and do not overwrite either when merging the request.
Invalid page numbers, nonfinite or nonpositive rectangles, out-of-page rectangles, missing IDs,
unloaded/destroyed viewers, and empty item lists are rejected. Coordinates are not clamped.

Reported incident: the submitted PDF has MediaBox [0,0,198,255.12], but the result annotation
has Rect [409.36,58.96,492.16,99.56]. That appearance is outside the page. The screenshot's
SDK item (409.3586120605469,58.96342468261719,160,48) must be rejected for that page.
There is no defensible exact correction without the SDK's loaded document and page metrics.
The previous x=100,y=180 payload was only an arbitrary in-bounds test, not a recovered position.

Local tests verify conversion parity and source bytes. They do not call the signing service.
Final end-to-end acceptance requires submitting with valid backend specimen/OTP, reopening the
returned PDF and comparing appearance bounds, page CropBox/rotation, and the specimen image
against the captured viewer. Server-side image padding or fit behavior cannot be inferred from
coordinates alone. This local patch is not published and existing remote dependencies do not
pick it up until the application uses the patched SDK.

## Android

```kotlin
// Obtain exact displayed CropBox dimensions from PDF metadata for the SAME source document.
// Example for the incident PDF only: listOf(PageSize(198.0, 255.12), ...remaining pages).
// Do not use bitmap pixels. PdfRenderer/getPageSize rounds point dimensions to integers.
val exported = viewer.exportInitSignDocument(
    specimenIds = mapOf(sdkItem.id to selectedBackendSpecimen.id),
    exactPageSizes = exactPdfPageSizes,
)
val request = JSONObject(existingInitSignFields)
val fields = exported.toJson()
request.put("base64Pdf", fields.getString("base64Pdf"))
request.put("signatureData", fields.getJSONArray("signatureData"))
// POST request to /signing/initsign using the application's existing authenticated client.
```

Exact PDF metadata is REQUIRED by the Android API: the platform renderer exposes integer
page dimensions. To preserve the exact normalized preview placement, coordinates are scaled
by exactWidth/renderWidth and exactHeight/renderHeight. Only a discrepancy below 1 point per
dimension is allowed. Larger discrepancies are rejected as a document/geometry mismatch.
This matches Flutter converting its own preview dimensions to exact PDF page dimensions.
There is deliberately no fallback that silently labels rounded dimensions as exact.

The loaded file hash is captured during load and checked on export. Changing a FileSource on
disk or switching documents while export is suspended fails export. Export is suspend because
reading the source bytes runs on Dispatchers.IO; call from the viewer's main-thread lifecycle.
