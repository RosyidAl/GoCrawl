# GoCrawl

[![CI/CD Pipeline](https://github.com/RosyidAl/GoCrawl/actions/workflows/ci.yml/badge.svg)](https://github.com/RosyidAl/GoCrawl/actions/workflows/ci.yml)
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Node.js 22](https://img.shields.io/badge/Node.js-22+-339933?logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**GoCrawl** adalah solusi terpadu untuk penelusuran (*crawling*) dan pengumpulan metadata artikel ilmiah dari portal jurnal berbasis **Open Journal Systems (OJS 2 & 3)**. Dilengkapi dengan mesin backend Java 21 berkinerja tinggi serta antarmuka visual **Web Dashboard** modern dengan streaming proses waktu nyata (*real-time*).

---

## Daftar Isi
- [Fitur Utama](#fitur-utama)
- [Persyaratan Sistem](#persyaratan-sistem)
- [Panduan Instalasi & Penggunaan](#panduan-instalasi--penggunaan)
  - [Opsi A: Menggunakan Web Dashboard (Direkomendasikan)](#opsi-a-menggunakan-web-dashboard-direkomendasikan)
  - [Opsi B: Menggunakan Command Line Interface (CLI)](#opsi-b-menggunakan-command-line-interface-cli)
- [Konfigurasi Target Jurnal (`journal.txt`)](#konfigurasi-target-jurnal-journaltxt)
- [Format Output Data](#format-output-data)
- [Kompilasi dari Source Code (Build from Source)](#kompilasi-dari-source-code-build-from-source)
- [CI/CD & Otomasi Deployment](#cicd--otomasi-deployment)
- [Struktur Direktori Proyek](#struktur-direktori-proyek)
- [Lisensi](#lisensi)

---

## Fitur Utama

### 1. Mesin Ekstraksi & Kurasi Data (Java 21 Engine)
- **Cascading Metadata Extractor:** Menangkap metadata terlengkap secara bertingkat dari meta tags standar akademik (*Highwire Press*, *Dublin Core*) dengan fallback selektor DOM CSS OJS modern.
- **Pembersihan Duplikasi *Galley Viewer*:** Mengeliminasi duplikasi artifak pembaca PDF (berjudul *"View of..."*) secara otomatis melalui filter URL presisi.
- **Deduplikasi 3 Lapis (*Multi-Tier*):**
  1. *Canonical URL & Article ID*: Mencegah kunjungan berulang pada rute URL alternatif.
  2. *Normalized DOI*: Menormalisasi DOI dan membersihkan suffix galley (`.gXXXX`).
  3. *Semantic Fingerprint*: Deteksi kesamaan entitas melalui kombinasi `Judul + Penulis Utama + Tahun`.
- **Quality Gates:** Menyaring dan menolak entitas rusak (*ghost records* tanpa judul/identitas ilmiah).
- **Sanitasi UTF-8 & Mojibake:** Perbaikan otomatis entitas HTML dan decoding karakter multibita yang rusak.

### 2. Antarmuka Web Dashboard Modern
- **Tanpa Dependency Eksternal:** Backend Node.js murni (*zero external npm dependencies*).
- **Live Progress & Konsol Interaktif:** Pemantauan progres penelusuran, persentase, dan log konsol secara live melalui Server-Sent Events (SSE).
- **Eksplorasi Data:** Filter pencarian teks bebas, penyaringan berbasis jurnal & tahun publikasi, serta filter DOI aktif.
- **Sitasi Otomatis:** Generator format sitasi ilmiah instan dalam format **APA 7th Edition**, **BibTeX**, dan **RIS**.
- **Ekspor Data:** Unduh kumpulan artikel terpadu dalam format **CSV** atau **JSON** dengan satu klik.
- **Mobile Friendly:** Desain responsif dengan navigasi bawah (*bottom bar*) untuk kenyamanan penggunaan di smartphone atau tablet.

---

## Persyaratan Sistem

Pastikan perangkat Anda telah terpasang:
- **Java Runtime Environment (JRE) / JDK 21 LTS** atau yang lebih baru.
  - Cek versi: `java -version`
- **Node.js v18.0.0+** (disarankan Node.js 20 atau 22 LTS).
  - Cek versi: `node -v`
- **Git** (untuk menduplikasi repositori).

---

## Panduan Instalasi & Penggunaan

### 1. Clone Repositori
```bash
git clone https://github.com/RosyidAl/GoCrawl.git
cd GoCrawl
```

---

### Opsi A: Menggunakan Web Dashboard (Direkomendasikan)

Mode ini memberikan antarmuka grafis yang ramah pengguna, memungkinkan Anda mengatur kata kunci, memilih jurnal target, memantau crawling secara langsung, dan mengekspor data hasil temuan.

#### 1. Jalankan Server Web:
```bash
# Menggunakan npm:
npm start

# Atau langsung menggunakan Node.js:
node web/server.mjs
```

> **Pengguna Windows:** Anda juga dapat langsung mengklik dua kali file `start-web.bat` atau menjalankan `.\start-web.ps1`.

#### 2. Akses Aplikasi di Browser:
- **Komputer Lokal:** Buka [http://localhost:3000](http://localhost:3000)
- **Akses Jaringan / HP:** Buka `http://<IP-Komputer-Anda>:3000` (contoh: `http://192.168.1.10:3000`)

---

### Opsi B: Menggunakan Command Line Interface (CLI)

Anda dapat menjalankan mesin crawler secara mandiri (*standalone*) menggunakan file `gocrawl.jar` yang telah disertakan di root repositori.

#### Perintah Dasar:
```bash
java -jar gocrawl.jar --keyword "kata kunci"
```

#### Contoh dengan Filter Tahun dan Output Kustom:
```bash
# Menelusuri artikel dengan kata kunci pendidikan islam antara tahun 2020 hingga 2024:
java -jar gocrawl.jar --keyword "pendidikan islam" --from-year 2020 --to-year 2024 --output output
```

#### Multi-Keyword (Operator OR):
Pisahkan frasa kata kunci dengan tanda koma `,`:
```bash
java -jar gocrawl.jar --keyword "islamisasi, islamization, kurikulum" --from-year 2022
```

#### Catatan Khusus Pengguna Windows:
Beberapa server jurnal lokal di Indonesia menggunakan sertifikat SSL yang belum dikenali oleh keystore bawaan Java. Tambahkan parameter trust store Windows agar koneksi SSL berjalan mulus:
```powershell
java "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -jar gocrawl.jar --keyword "pendidikan" --from-year 2023
```

#### Ringkasan Parameter CLI:
| Parameter | Kebutuhan | Keterangan |
| :--- | :---: | :--- |
| `--keyword <string>` | **Wajib** | Frasa pencarian. Gunakan koma untuk logika pencarian alternatif (`OR`). |
| `--from-year <YYYY>` | Opsional | Batas awal tahun publikasi artikel. |
| `--to-year <YYYY>` | Opsional | Batas akhir tahun publikasi artikel. |
| `--output <path>` | Opsional | Folder penyimpanan output data (default: `output/`). |
| `--help` | Opsional | Menampilkan petunjuk pemakaian parameter. |

---

## Konfigurasi Target Jurnal (`journal.txt`)

Daftar URL basis jurnal disimpan dalam berkas [`journal.txt`](file:///journal.txt) di root direktori kerja (satu URL per baris). 

Contoh isi berkas `journal.txt`:
```text
https://jurnal.iainponorogo.ac.id/index.php/jusan
https://www.syekhnurjati.ac.id/jurnal/index.php/tamaddun/index
https://journal.unusia.ac.id/ISLAMNUSANTARA
https://www.mdpi.com/journal/religions/
https://ejournal.uinsaid.ac.id/al-isnad
```

> **Tips:** Anda juga dapat menambah, mengedit, atau menghapus daftar jurnal target secara langsung melalui menu **"Target Jurnal"** pada Web Dashboard.

---

## Format Output Data

Hasil penelusuran otomatis disimpan ke dalam folder `output/`:
- **`output/<nama_jurnal>/articles.csv`**: Format lembar kerja tabular (kompatibel dengan Excel, Google Sheets, SPSS).
- **`output/<nama_jurnal>/articles.json`**: Format data terstruktur JSON untuk integrasi aplikasi.

Field metadata yang disimpan:
- `title` : Judul artikel
- `authors` : Penulis (dipisahkan tanda koma)
- `year` : Tahun terbit
- `journal` : Nama jurnal ilmiah
- `url` : Tautan artikel asli
- `abstract`: Ringkasan / abstrak artikel
- `doi` : Nomor *Digital Object Identifier*
- `published`: Tanggal penerbitan resmi
- `keywords` : Kata kunci artikel

---

## Kompilasi dari Source Code (Build from Source)

Jika Anda memodifikasi kode sumber Java di `src/main/java/`, Anda dapat melakukan kompilasi ulang menjadi executable `gocrawl.jar`:

### Linux / macOS:
```bash
mkdir -p target/classes
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar" -d target/classes $(find src/main/java -name "*.java")
cd target/classes && jar -xf ../../lib/jsoup-1.18.3.jar && cd ../..
jar --create --file gocrawl.jar --main-class id.gocrawl.Main -C target/classes .
```

### Windows (PowerShell):
```powershell
New-Item -ItemType Directory -Force target/classes | Out-Null
$files = (Get-ChildItem -Recurse -Filter *.java src).FullName
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar" -d target/classes $files
Push-Location target/classes; jar -xf ../../lib/jsoup-1.18.3.jar; Pop-Location
jar --create --file gocrawl.jar --main-class id.gocrawl.Main -C target/classes .
```

---

## CI/CD & Otomasi Deployment

Proyek ini telah dikonfigurasi dengan pipeline [GitHub Actions](file:///.github/workflows/ci.yml):
1. **Automated Testing & Build**: Kompilasi Java 21, smoke test JAR, dan uji validitas sintaks Node.js pada setiap `push` dan `pull_request`.
2. **Auto Deploy ke VPS**: Setiap push ke branch `main` otomatis dideploy ke VPS tujuan pada direktori `~/projects/gocrawl` melalui koneksi VPN aman Tailscale dan direstart dengan PM2.
3. **Automated GitHub Release**: Setiap pembuatan git tag berawalan `v*` (contoh `v1.0.0`) otomatis membuat rilis publik di GitHub dengan melampirkan binary `gocrawl.jar` siap pakai.

---

## Struktur Direktori Proyek

```text
GoCrawl/
├── .github/workflows/ci.yml # Konfigurasi CI/CD Pipeline
├── docs/                    # Dokumentasi evaluasi arsitektur & analisis
├── lib/                     # Dependency JAR lokal (Jsoup 1.18.3)
├── src/main/java/id/gocrawl/# Source code Java Crawler Engine
│   ├── Article.java
│   ├── DeduplicationEngine.java
│   ├── Main.java
│   ├── MetadataExtractor.java
│   ├── QualityGate.java
│   ├── TextSanitizer.java
│   └── UrlFilter.java
├── web/                     # Web Dashboard (Node.js)
│   ├── public/              # Aset Frontend (HTML, CSS, JS)
│   └── server.mjs           # HTTP & SSE Server Backend
├── gocrawl.jar              # Standalone executable JAR siap pakai
├── journal.txt              # Daftar URL basis jurnal sasaran
├── package.json             # Manifest scripts Node.js
├── pom.xml                  # Konfigurasi Maven (opsional)
├── start-web.bat            # Launcher cepat Windows (Batch)
└── start-web.ps1            # Launcher cepat Windows (PowerShell)
```

---

## Lisensi

Didistribusikan di bawah lisensi [MIT](LICENSE). Silakan gunakan, pelajari, dan kembangkan secara bebas untuk keperluan riset maupun komersial.
