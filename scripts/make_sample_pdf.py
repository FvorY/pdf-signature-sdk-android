#!/usr/bin/env python3
"""Writes a small multi-page PDF for the sample app, using only the standard library.

Every page has a 50-point grid labelled in the SDK's coordinate system
(origin top-left, y down, PDF points), so positions can be checked by eye.

Usage: python3 scripts/make_sample_pdf.py <output.pdf>
"""
import sys

A4 = (595.28, 841.89)
LETTER = (612.0, 792.0)

# (title, media box width, height, /Rotate)
PAGES = [
    ("A4 portrait", *A4, 0),
    ("A4 portrait", *A4, 0),
    ("A4 landscape", A4[1], A4[0], 0),
    ("US Letter", *LETTER, 0),
    ("A4 with /Rotate 90", *A4, 90),
]


def content_stream(number, title, width, height):
    ops = ["0.85 G 0.5 w"]
    x = 50
    while x < width:
        ops.append(f"{x} 0 m {x} {height:.2f} l S")
        x += 50
    y = 50
    while y < height:
        ops.append(f"0 {height - y:.2f} m {width:.2f} {height - y:.2f} l S")
        y += 50
    ops.append("0 0 1 RG 1.5 w 20 20 %.2f %.2f re S" % (width - 40, height - 40))
    ops.append("0.4 g BT /F1 7 Tf")
    x = 100
    while x < width:
        ops.append(f"1 0 0 1 {x + 2} {height - 10:.2f} Tm (x {x}) Tj")
        x += 100
    y = 100
    while y < height:
        ops.append(f"1 0 0 1 4 {height - y - 9:.2f} Tm (y {y}) Tj")
        y += 100
    ops.append("ET")
    ops.append("0.06 0.09 0.16 rg BT /F1 28 Tf 1 0 0 1 60 %.2f Tm (Page %d) Tj ET" % (height - 90, number))
    ops.append("BT /F1 14 Tf 1 0 0 1 60 %.2f Tm (%s - %.2f x %.2f pt) Tj ET" % (height - 115, title, width, height))
    ops.append(
        "BT /F1 10 Tf 1 0 0 1 60 %.2f Tm (Grid labels use the SDK coordinates: origin top-left, y down.) Tj ET"
        % (height - 135)
    )
    return "\n".join(ops).encode("latin-1")


def build():
    objects = {}
    font_id = 3
    objects[font_id] = b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
    kids = []
    next_id = 4
    for number, (title, width, height, rotate) in enumerate(PAGES, start=1):
        page_id, stream_id = next_id, next_id + 1
        next_id += 2
        kids.append(page_id)
        data = content_stream(number, title, width, height)
        objects[stream_id] = b"<< /Length %d >>\nstream\n" % len(data) + data + b"\nendstream"
        page = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] " % (width, height)
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
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    with open(sys.argv[1], "wb") as f:
        f.write(build())
