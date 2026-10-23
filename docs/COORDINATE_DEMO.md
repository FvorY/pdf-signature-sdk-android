# Demo Android: koordinat SDK ke Postman

APK: sample/build/outputs/apk/debug/sample-debug.apk (hasil `./gradlew :sample:assembleDebug`)
Nama aplikasi: ezSign Coordinate Demo
Package: io.github.fvory.psdk.coordinates (terpisah dari PSDK Sample)
Minimum: Android 7.0 / API 24

1. Instal APK lalu pilih **1. Buka PDF**. Demo ini membuka PDF tanpa password.
2. Isi **specimenId backend**. Pilih **Gambar TTD** milik specimenId yang sama untuk membandingkan bentuknya.
   Tanpa gambar pilihan, preview memakai gambar demo; backend tetap memakai spesimen dari specimenId.
3. Opsional: isi **X awal** dan **Y awal** (pojok kiri atas, PDF point) agar TTD langsung muncul di posisi itu
   tanpa digeser; kosongkan keduanya untuk menaruh TTD di tengah halaman. Posisi di luar halaman ditarik masuk.
   Lalu pilih **2. Tambah TTD**. Tutup panel Postman bila ingin area PDF lebih luas. Geser/resize TTD pada PDF.
   Setiap item terikat ke specimenId yang diisi saat item dibuat. Mengubah field tidak mengganti ID item lama.
4. Buka panel kembali, periksa koordinat SDK (kiri atas) dan payload (kiri bawah), lalu pilih
   **3. Simpan JSON Postman**. File berisi request lengkap, termasuk base64Pdf asli, signatureData, nama PDF,
   reason, location, serta pilihan signing default dari payload sebelumnya. OTP boleh diisi di aplikasi
   atau diedit di Postman. Jangan mengganti base64Pdf dengan PDF lain.
5. Pindahkan file JSON ke komputer. Di Postman:
   - POST https://gtm.myesign.id/api/signing/initsign
   - Authorization: Bearer Token, isi token yang valid.
   - Body > raw > JSON, tempel SELURUH isi file JSON. File ini adalah body request, bukan Postman Collection.
   - Pastikan OTP, specimenId, reason, location, putQR dan paidAllSign sesuai pengujian akunmu, lalu Send.
6. Simpan/decode hasil server menjadi PDF. Pilih **Buka PDF hasil** di demo untuk melihat hasil server.
   Preview hasil memakai viewer terpisah, sehingga dokumen asli dan posisi TTD tetap tersimpan.

**Salin signatureData** hanya menyalin array koordinat tanpa base64Pdf. Untuk request lengkap gunakan Simpan JSON.
Ukuran halaman diambil dari CropBox/MediaBox dan rotasi PDF yang sama melalui Pdfium, termasuk pecahan point.
Android renderer bisa membulatkan ukuran; exporter menyesuaikan selisih kurang dari 1 point ke ukuran PDF asli.
Posisi di luar halaman ditolak sebelum ekspor. JSON dibuat dari snapshot posisi saat tombol Simpan ditekan.

Validasi 22 Oktober 2026 (v4):
- Build APK debug, unit test core dan demo, ktlint sample, serta 6 tes instrumented di emulator Android 15 lulus.
- Fitur posisi awal (`PsdkConfig.defaultPosition`) diuji di tes instrumented dan tes iOS (33 tes lulus).
- Signing di server dilakukan manual oleh pengguna; keselarasan hasil server belum diklaim terverifikasi.

Build ulang:
JAVA_HOME=/path/to/jdk \
ANDROID_HOME=/path/to/android/sdk \
./gradlew :sample:assembleDebug :sample:testDebugUnitTest :psdk-core:testDebugUnitTest :sample:ktlintCheck

## Versi 2: validasi body Postman

Tombol **Cek JSON Postman** membuka JSON request lengkap, membaca PDF dari base64Pdf di file itu,
lalu memeriksa ukuran halaman dan batas kotak TTD terhadap PDF yang benar-benar dikirim.
Ekspor JSON juga menjalankan pemeriksaan ulang ini. Pemeriksaan tidak mengirim request ke server.

Kasus terbaru: docWidth=595 dan docHeight=841, tetapi base64Pdf berisi 16 halaman 198 x 255.12.
Hasil server mempunyai annotation Rect [480.85,719.31,595,841] yang seluruhnya di luar halaman.
Fixture regresi menolak payload tersebut. Payload koreksi mempertahankan posisi relatif terhadap
frame deklarasi 595 x 841 dengan sx=198/595 dan sy=255.12/841. Ini merupakan koreksi proporsional;
posisi SDK persisnya tetap harus diambil dari ekspor dokumen yang sama.

## Versi 3: paket uji satu snapshot

Ekspor paket uji ZIP + Postman menyimpan source.pdf (byte PDF dari viewer), request.json,
audit.json (hash sumber, item SDK mentah, ukuran halaman SDK dan metadata), serta
postman_collection.json. Tampilan menampilkan nama file dan SHA-256 dari file yang dipilih.
Ekspor diblokir bila hash viewer berbeda dari file pilihan. Koleksi Postman mempertahankan
PDF dan signatureData dari snapshot yang sama. Import koleksi, isi variabel accessToken dan
otp lalu kirim. Paket ini membantu membedakan PDF yang berbeda dari kesalahan pembacaan
metadata. Penyebab laporan ukuran 595 x 841 vs base64Pdf 198 x 255.12 belum dipastikan
melalui pengujian di perangkat.
