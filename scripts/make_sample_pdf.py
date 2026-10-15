#!/usr/bin/env python3
"""Writes the multi-page test PDFs, using only the standard library.

Every page has a 50-point grid labelled in the SDK's coordinate system (origin at the top-left of
the visible box, y down, PDF points), so positions can be checked by eye.

Usage:
  python3 scripts/make_sample_pdf.py [--set sample|geometry] <output.pdf>

Sets:
  sample    A4, A4 landscape, US Letter and a /Rotate 90 page (sample app, parity)
  geometry  /Rotate 0/90/180/270, a CropBox inside a larger MediaBox, CropBox with /Rotate 90 and a
            MediaBox that does not start at 0,0 (parity of page sizes and coordinates)
"""
import sys

A4 = (0, 0, 595.28, 841.89)
A4_LANDSCAPE = (0, 0, 841.89, 595.28)
LETTER = (0, 0, 612.0, 792.0)
BIG = (0, 0, 700.0, 1000.0)
CROP = (50.0, 100.0, 650.0, 900.0)  # 600 x 800 inside BIG

# Each page: (title, media box, crop box or None, /Rotate)
SETS = {
    "sample": [
        ("A4 portrait", A4, None, 0),
        ("A4 portrait", A4, None, 0),
        ("A4 landscape", A4_LANDSCAPE, None, 0),
        ("US Letter", LETTER, None, 0),
        ("A4 with /Rotate 90", A4, None, 90),
    ],
    "geometry": [
        ("A4 /Rotate 0", A4, None, 0),
        ("A4 /Rotate 90", A4, None, 90),
        ("A4 /Rotate 180", A4, None, 180),
        ("A4 /Rotate 270", A4, None, 270),
        ("CropBox 600x800 inside MediaBox 700x1000", BIG, CROP, 0),
        ("CropBox 600x800 + /Rotate 90", BIG, CROP, 90),
        ("MediaBox [100 200 712 992] (Letter, not at 0,0)", (100.0, 200.0, 712.0, 992.0), None, 0),
    ],
}


def fmt(value):
    return f"{value:.2f}".rstrip("0").rstrip(".")


def content_stream(number, title, media, crop):
    mx0, my0, mx1, my1 = media
    vx0, vy0, vx1, vy1 = crop or media
    width, height = vx1 - vx0, vy1 - vy0
    ops = []
    if crop:
        # Red frame on the MediaBox edge: hidden when the CropBox is honoured
        ops.append(f"1 0 0 RG 6 w {fmt(mx0 + 3)} {fmt(my0 + 3)} {fmt(mx1 - mx0 - 6)} {fmt(my1 - my0 - 6)} re S")
    ops.append("0.85 G 0.5 w")
    x = 50
    while x < width:
        ops.append(f"{fmt(vx0 + x)} {fmt(vy0)} m {fmt(vx0 + x)} {fmt(vy1)} l S")
        x += 50
    y = 50
    while y < height:
        ops.append(f"{fmt(vx0)} {fmt(vy1 - y)} m {fmt(vx1)} {fmt(vy1 - y)} l S")
        y += 50
    ops.append(f"0 0 1 RG 1.5 w {fmt(vx0 + 20)} {fmt(vy0 + 20)} {fmt(width - 40)} {fmt(height - 40)} re S")
    ops.append("0.4 g BT /F1 7 Tf")
    x = 100
    while x < width:
        ops.append(f"1 0 0 1 {fmt(vx0 + x + 2)} {fmt(vy1 - 10)} Tm (x {x}) Tj")
        x += 100
    y = 100
    while y < height:
        ops.append(f"1 0 0 1 {fmt(vx0 + 4)} {fmt(vy1 - y - 9)} Tm (y {y}) Tj")
        y += 100
    ops.append("ET")
    ops.append(f"0.06 0.09 0.16 rg BT /F1 28 Tf 1 0 0 1 {fmt(vx0 + 60)} {fmt(vy1 - 90)} Tm (Page {number}) Tj ET")
    ops.append(f"BT /F1 12 Tf 1 0 0 1 {fmt(vx0 + 60)} {fmt(vy1 - 115)} Tm ({title}) Tj ET")
    ops.append(
        f"BT /F1 10 Tf 1 0 0 1 {fmt(vx0 + 60)} {fmt(vy1 - 135)} Tm "
        f"(Visible box {fmt(width)} x {fmt(height)} pt. Grid labels: origin top-left, y down.) Tj ET"
    )
    return "\n".join(ops).encode("latin-1")


def build(pages):
    objects = {}
    font_id = 3
    objects[font_id] = b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
    kids = []
    next_id = 4
    for number, (title, media, crop, rotate) in enumerate(pages, start=1):
        page_id, stream_id = next_id, next_id + 1
        next_id += 2
        kids.append(page_id)
        data = content_stream(number, title, media, crop)
        objects[stream_id] = b"<< /Length %d >>\nstream\n" % len(data) + data + b"\nendstream"
        page = "<< /Type /Page /Parent 2 0 R /MediaBox [%s] " % " ".join(fmt(v) for v in media)
        if crop:
            page += "/CropBox [%s] " % " ".join(fmt(v) for v in crop)
        if rotate:
            page += "/Rotate %d " % rotate
        page += "/Resources << /Font << /F1 %d 0 R >> >> /Contents %d 0 R >>" % (font_id, stream_id)
        objects[page_id] = page.encode("latin-1")
    objects[1] = b"<< /Type /Catalog /Pages 2 0 R >>"
    objects[2] = ("<< /Type /Pages /Kids [%s] /Count %d >>" % (" ".join(f"{k} 0 R" for k in kids), len(kids))).encode()

    out = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = {}
    for obj_id in sorted(objects):
        offsets[obj_id] = len(out)
        out += b"%d 0 obj\n" % obj_id + objects[obj_id] + b"\nendobj\n"
    xref = len(out)
    count = max(objects) + 1
    out += b"xref\n0 %d\n0000000000 65535 f \n" % count
    for obj_id in range(1, count):
        out += b"%010d 00000 n \n" % offsets[obj_id]
    out += b"trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (count, xref)
    return bytes(out)


if __name__ == "__main__":
    args = sys.argv[1:]
    name = "sample"
    if len(args) == 3 and args[0] == "--set":
        name, args = args[1], args[2:]
    if len(args) != 1 or name not in SETS:
        sys.exit(__doc__)
    with open(args[0], "wb") as f:
        f.write(build(SETS[name]))
