# GoCrawl (Fase 2)

Crawler metadata jurnal Open Journal Systems (OJS) berbasis Java 21 LTS dengan *Cascading Metadata Extractor*, *Multi-Tier Deduplication Engine*, dan *Data Quality Gates*.

Dokumentasi arsitektur dan evaluasi teknis lengkap tersedia pada folder [`docs/`](file:///C:/Users/H0me%20PC/Projects/GoCrawl/docs/README.md).

---

## Fitur Utama (Fase 1 & Fase 2)

- **URL Filtering Presisi:** Menyaring otomatis tautan galley viewer (`/article/view/{id}/{galleyId}`) dan download link sehingga mengeliminasi duplikasi data bertajuk *"View of ..."*.
- **Cascading Metadata Extractor (Jsoup):** Ekstraksi metadata bertingkat dari kamus meta tags Highwire Press & Dublin Core terlengkap dengan fallback CSS selector DOM OJS modern.
- **Perbaikan Missing Values:** Memperbaiki penangkapan field `published`, `authors`, `abstract`, `doi`, dan `journal`.
- **Deduplikasi 3 Lapis:** Mencegah duplikasi data berdasarkan URL Kanonikal & ID Artikel, DOI ternormalisasi (pembersihan suffix galley `.gXXXX`), serta sidik jari semantik (`Judul + Penulis + Tahun`).
- **Data Quality Gates:** Menolak entitas sampah (*ghost records* tanpa identitas akademik) sebelum disimpan.
- **Pembersihan Teks & Mojibake:** Sanitasi entitas HTML, normalisasi spasi ganda, dan pemulihan otomatis string UTF-8 yang mengalami kerusakan decoding.

---

## Cara Kompilasi dan Menjalankan

### Persyaratan
- JDK 21 LTS atau lebih baru.
- Pustaka `lib/jsoup-1.18.3.jar` (telah disertakan di repositori).

### 1. Build Standalone Executable JAR
Jalankan perintah PowerShell berikut di direktori proyek:

```powershell
$javaFiles = (Get-ChildItem -Recurse -Filter *.java src).FullName
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar" -d target/classes $javaFiles
cd target/classes; jar -xf ../../lib/jsoup-1.18.3.jar; cd ../..
jar --create --file gocrawl.jar --main-class id.gocrawl.Main -C target/classes .
```

### 2. Menjalankan Pencarian
Pastikan file `journal.txt` berada di direktori kerja (berisi URL jurnal, satu per baris). Contoh eksekusi:

```powershell
java "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -jar gocrawl.jar --keyword "pendidikan islam" --from-year 2020 --to-year 2025 --output output
```

- Parameter `--keyword` (wajib): Frasa kata kunci yang dicocokkan dengan logika `AND` pada judul dan abstrak.
- Parameter `--from-year` dan `--to-year` (opsional): Filter rentang tahun publikasi.
- Parameter `--output` (opsional, default: `output/`): Direktori penyimpanan hasil (`articles.csv` dan `articles.json` per jurnal).
- Opsi `-Djavax.net.ssl.trustStoreType=Windows-ROOT` disarankan di Windows untuk memastikan handshaking TLS berjalan lancar dengan trust store Windows.

---

## 3. Web Dashboard Antarmuka Pengguna

Tersedia antarmuka web modern, responsif, dan ramah seluler (*mobile-friendly*) untuk mengoperasikan crawler secara visual, memantau live log streaming, dan mengeksplorasi artikel ilmiah.

### Menjalankan Server Web:
```powershell
# Menggunakan npm:
npm start

# Atau langsung menggunakan Node.js:
node web/server.mjs

# Atau melalui script peluncur cepat di Windows:
.\start-web.bat   # CMD / Double-Click
.\start-web.ps1   # PowerShell
```

Akses aplikasi pada peramban web:
- **Lokal:** [http://localhost:3000](http://localhost:3000)
- **Perangkat Bergerak (Mobile/Jaringan):** `http://<IP-Komputer>:3000`

### Keunggulan Antarmuka Web:
- **Kontrol & Live Stream SSE:** Eksekusi crawling instan dengan streaming konsol real-time dan bilah progres.
- **Eksplorasi Artikel Interaktif:** Pencarian teks bebas, penyaringan jurnal & tahun, filter DOI, serta generator sitasi (APA 7th, BibTeX, RIS).
- **Manajemen Jurnal Sasaran:** Tambah dan hapus URL OJS pada `journal.txt` langsung dari antarmuka.
- **Ekspor 1-Klik:** Unduh seluruh kumpulan data terpadu dalam format CSV atau JSON.
- **Bebas Emoticon & Ikon Vektor Orisinal:** Seluruh elemen visual dirancang menggunakan ikon SVG vektor murni berpresisi tinggi.
- **Desain Reflow Seluler:** Kompatibel dengan layar smartphone (bilah navigasi bawah, target sentuh >= 44px, dan tampilan kartu responsif).
