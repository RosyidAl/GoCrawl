# Evaluasi Kritis Kualitas Data dan Masalah Crawling GoCrawl

Dokumen ini menyajikan hasil *Root Cause Analysis* (RCA) teknis terhadap dua kendala utama yang teridentifikasi pada hasil penelusuran GoCrawl P1:
1. **Duplikasi Data Crawling:** Artikel yang sama tercatat lebih dari sekali dengan judul terdistorsi berawalan `"View of (nama artikel)"`.
2. **Missing Values pada Atribut Dataset:** Tingginya persentase kolom kosong (`published`, `authors`, `abstract`, `doi`, `journal`).

Evaluasi ini disusun berdasarkan pengujian empiris pada folder `output/` terhadap tiga jurnal target di `journal.txt` (`jusan`, `tamaddun`, `ISLAMNUSANTARA`).

---

## 1. Analisis Masalah 1: Duplikasi Crawling dan Prefix "View of..."

### 1.1 Bukti Empiris dari Dataset Output

Berdasarkan pemeriksaan file `articles.csv` di setiap folder output, ditemukan pola anomali berikut:

#### Kasus A: Portal JUSAN (`output/jusan/articles.csv`)
- **Baris 2 (Record Asli):**
  - **Judul:** `"MASJID BAITURROHIM RINGINWOK: SEJARAH, TRADISI, DAN KONTRIBUSINYA..."`
  - **URL:** `https://jurnal.iainponorogo.ac.id/index.php/jusan/article/view/12693`
  - **Penulis & Abstrak:** Terisi lengkap.
- **Baris 5 (Record Duplikat Galley):**
  - **Judul:** `"View of MASJID BAITURROHIM RINGINWOK: SEJARAH, TRADISI, DAN KONTRIBUSINYA..."`
  - **URL:** `https://jurnal.iainponorogo.ac.id/index.php/jusan/article/view/12693/4504`
  - **Penulis, Tahun, Abstrak, DOI:** Bernilai kosong (`""`).

#### Kasus B: Portal ISLAMNUSANTARA (`output/ISLAMNUSANTARA/articles.csv`)
- **Baris 1:** `"View of Makhdūmūn and Easternmost Dawah in Early Modern Philippines..."` &rarr; URL: `.../article/view/1735/798` (Semua kolom lain kosong).
- **Baris 2:** `"Makhdūmūn and Easternmost Dawah in Early Modern Philippines"` &rarr; URL: `.../article/view/1735` (Data metadata lengkap).

#### Kasus C: Portal Tamaddun (`output/tamaddun/articles.csv`)
- **Baris 1:** URL `.../article/view/21442/pdf_106` &rarr; DOI: `10.24235/tamaddun.v13i2.21442.g7209`
- **Baris 2:** URL `.../article/view/21442` &rarr; DOI: `10.24235/tamaddun.v13i2.21442`
- **Dampak:** Judul identik, penulis identik, namun terdaftar dua kali karena halaman galley PDF OJS menyematkan DOI galley terpisah (akhiran `.g7209`).

---

### 1.2 Analisis Arsitektur OJS (Open Journal Systems)

Pada arsitektur OJS versi 2.x maupun 3.x, terdapat pemisahan rute URL:
1. **Article Landing Page (Abstract Page):**
   - Pola: `/article/view/{articleId}` (contoh: `/article/view/12693`).
   - Fungsi: Menampilkan abstrak artikel, daftar penulis, afiliasi, DOI, dan tag metadata akademik (`<meta name="citation_*">`).
2. **Galley Viewer / PDF Wrapper Page:**
   - Pola: `/article/view/{articleId}/{galleyId}` (contoh: `/article/view/12693/4504` atau `/article/view/21442/pdf_106`).
   - Fungsi: Wadah penampil dokumen PDF / HTML.
   - Karakteristik HTML: Tag `<title>` OJS secara baku di-*generate* menggunakan template lokalisasi:
     - Bahasa Inggris: `View of {Article Title} | {Journal Title}`
     - Bahasa Indonesia: `Tampilan {Judul Artikel} | {Nama Jurnal}`
   - Karakteristik Metadata: Mayoritas tema OJS **tidak menyertakan** tag sitasi Highwire Press (`citation_*`) pada halaman viewer ini karena halaman tersebut dianggap sebagai artefak media pembaca, bukan halaman landing metadata.

---

### 1.3 Cacat Desain pada Kode Sumber ([`Main.java`](file:///C:/Users/H0me%20PC/Projects/GoCrawl/src/main/java/id/gocrawl/Main.java))

Terjadinya duplikasi ini berakar pada 3 (tiga) titik kegagalan di dalam `Main.java`:

#### Titik 1: Filter Tautan Terlalu Permisif ([Line 205](file:///C:/Users/H0me%20PC/Projects/GoCrawl/src/main/java/id/gocrawl/Main.java#L205))
```java
// KODE SAAT INI:
if (article != null && article.getPath() != null && article.getPath().contains("/article/view/")) {
    URI candidate = article;
    articleTasks.add(articlePool.submit(() -> crawlArticle(candidate)));
}
```
**Kelemahan:** Logika hanya memeriksa keberadaan substring `"/article/view/"`. Tautan ke tombol *"PDF"* pada daftar isi edisi mengarah ke `/article/view/{id}/{galleyId}`. Karena substring cocok, URL viewer dimasukkan ke antrean ekstraksi bersama dengan URL landing page.

#### Titik 2: Fallback Tag `<title>` yang Tidak Terfilter ([Line 217-220](file:///C:/Users/H0me%20PC/Projects/GoCrawl/src/main/java/id/gocrawl/Main.java#L217-L220))
```java
// KODE SAAT INI:
String title = p.get("citation_title");
if (title.isBlank()) title = p.get("dc.title");
if (title.isBlank()) title = p.title; // <--- JATUH KE HTML <title>
if (title.isBlank()) return;
```
**Kelemahan:** Pada halaman viewer galley, tag `citation_title` dan `dc.title` tidak tersedia. Kode secara otomatis mengambil `p.title` yang berisi `"View of [Judul Artikel]"`.

#### Titik 3: Kunci Deduplikasi Hanya Berdasarkan URL Mentah ([Line 229 & 234](file:///C:/Users/H0me%20PC/Projects/GoCrawl/src/main/java/id/gocrawl/Main.java#L229-L235))
```java
// KODE SAAT INI:
String canonical = first(p.get("citation_public_url"), uri.toString());
...
synchronized (articles) {
    if (!articles.containsKey(canonical)) {
        articles.put(canonical, item);
        sink.accept(item);
    }
}
```
**Kelemahan:** Karena `citation_public_url` tidak ada di halaman viewer, `canonical` jatuh ke `uri.toString()` (`.../article/view/12693/4504`). Peta `articles` menganggap ini adalah entitas baru yang berbeda dari `.../article/view/12693`, sehingga record diduplikasi dan ditulis ke output.

---

## 2. Analisis Masalah 2: Missing Values pada Kolom Metadata

Berdasarkan audit hasil crawl, berikut rincian missing value dan penyebab teknisnya:

```
+------------------+-----------------------+--------------------------------------------------------+
| Kolom            | Persentase Missing    | Penyebab Utama                                         |
+------------------+-----------------------+--------------------------------------------------------+
| published        | ~98% (Kritis)         | Logika kode mengabaikan meta tag 'citation_date'       |
| authors          | ~35% (Baris duplikat) | Galley viewer tidak merender tag sitasi                |
| abstract         | ~35% (Baris duplikat) | Galley viewer tidak memiliki deskripsi abstrak         |
| doi              | ~40% (Galley & Non)   | Halaman galley kosong; beberapa artikel lama tanpa DOI |
| journal          | ~35% (Distorsi host)  | Galley viewer jatuh ke fallback host domain            |
+------------------+-----------------------+--------------------------------------------------------+
```

### 2.1 Anomali Kolom `published`: Cacat Logika Pengecekan Key

Pada [`Main.java` baris 223 dan 224](file:///C:/Users/H0me%20PC/Projects/GoCrawl/src/main/java/id/gocrawl/Main.java#L223-L224):
```java
// Ekstraksi tanggal lengkap (published):
String published = first(p.get("citation_publication_date"), p.get("dc.date"));

// Ekstraksi tahun (year):
String year = yearFrom(first(p.get("citation_date"), published, p.get("citation_year")));
```

**Temuan Bug:**
- OJS versi 3.x secara *de facto* menyematkan tanggal terbit pada meta tag:
  `<meta name="citation_date" content="2026/01/15">`
- Ekstraksi `year` berhasil mendapatkan tahun karena baris 224 membaca `citation_date`.
- Namun, variabel `published` pada baris 223 **tidak memeriksa `citation_date`** sama sekali; variabel ini hanya memeriksa `citation_publication_date` dan `dc.date`.
- Akibatnya, pada ribuan artikel yang valid, kolom `year` terisi `2026` namun kolom `published` bernilai kosong `""`.

### 2.2 Anomali Kolom `authors`, `abstract`, dan `doi`

- **Record Galley:** Menyumbang 100% kekosongan pada entitas duplikat yang lolos filter.
- **Record Artikel Asli:**
  - Sebagian OJS menggunakan variasi nama meta tag Dublin Core seperti `dc.date.issued`, `dc.contributor.author`, `dc.identifier.doi` atau `prism.doi`.
  - Struktur parser `Page` saat ini hanya mencari key persis (`dc.creator`, `dc.identifier`, `citation_doi`). Semua tag dengan kualifikasi dot-notation OJS (`DC.Identifier.DOI`) tidak tertangkap.

### 2.3 Keterbatasan Parser HTML Swing (`ParserDelegator`)

Komponen `javax.swing.text.html.parser.ParserDelegator` dirancang pada era Java 1.1/1.2 untuk komponen GUI desktop desktop HTML 3.2. Penggunaannya pada web modern menimbulkan keterbatasan teknis:
1. **Ketidakmampuan Mengurai Tag HTML5 Self-Closing & Malformed DOM:** Halaman OJS modern yang memuat script modern, SVG, atau tag unclosed dapat menyebabkan parser Swing menghentikan parsing callback sebelum mencapai tag `<meta>` di bagian bawah `<head>`.
2. **Tidak Ada Dukungan CSS Selector / DOM Query:** Sistem tidak memiliki cara untuk mengambil konten dari body HTML (seperti elemen `.item.abstract` atau `.author-name`) jika tag `<meta>` tidak diaktifkan oleh pengelola jurnal.

### 2.4 Anomali Encoding dan Karakter Rusak (*Mojibake*)

Pada dataset `ISLAMNUSANTARA` dan `tamaddun` ditemukan anomali karakter:
- `MakhdÅ«mÅ«n` &rarr; seharusnya `Makhdūmūn`
- `Cheng Hoâ€™s` &rarr; seharusnya `Cheng Ho’s`
- `Ã¢â‚¬Å“SnouckismÃ¢â‚¬Â` &rarr; seharusnya `“Snouckism”`

**Penyebab Teknis:**
1. Pemanggilan `HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)` secara statis tanpa memeriksa charset pada header HTTP `Content-Type: text/html; charset=ISO-8859-1` atau `<meta charset="...">`.
2. Jika server web OJS mengembalikan data dalam Latin-1 atau teks yang telah mengalami double-encoding UTF-8 dari basis data MySQL asal, pembacaan paksa UTF-8 menghasilkan byte mojibake.

---

## 3. Matriks Evaluasi Masalah

| Parameter Evaluasi | Perilaku Saat Ini | Dampak Kualitas Data | Solusi yang Diperlukan |
|---|---|---|---|
| **Filter URL Artikel** | Substring `contains("/article/view/")` | Mengindeks halaman galley viewer PDF/HTML | Regex ketat: `/article/view/(\d+)$` |
| **Kunci Deduplikasi** | URL string mentah (`articles.containsKey`) | Gagal mencegah duplikasi URL galley dan variasi query | Normalisasi URL + DOI hash + Fingerprint Judul |
| **Pengecekan Tag Tanggal** | Hanya `citation_publication_date` & `dc.date` | Kolom `published` kosong pada 98% artikel | Tambahkan `citation_date`, `dc.date.issued`, `prism.publicationDate` |
| **Data Integrity Gate** | Menerima record hanya bermodal judul | *Ghost records* (record tanpa penulis & tanpa abstrak) lolos ke output | Tolak artikel jika `authors.isBlank() && abstract.isBlank()` |
| **Engine Parser HTML** | `ParserDelegator` (Swing legacy) | Rentan malformed HTML5, tidak ada fallback DOM selector | Migrasi ke parser HTML modern (misal: Jsoup) |
| **Penanganan Encoding** | Hardcoded `UTF-8` | Karakter mojibake pada nama tokoh & kutipan | Dekode dinamis berdasarkan header respons & sanitasi string |
