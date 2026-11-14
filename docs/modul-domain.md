# Modul domain dan utilitas

Semua kelas pada bagian ini berada di package `id.gocrawl`.

| Modul | Tanggung jawab | API penting |
|---|---|---|
| `Article` | Model immutable metadata artikel | record berisi title, authors, year, journal, URL, abstrak, DOI, tanggal terbit, keywords |
| `Journal` | Model immutable katalog jurnal | record metadata jurnal, subjek, SINTA, OJS/OAI, ISSN |
| `GarudaSubject` | Taksonomi 40 subjek resmi Portal Garuda dan alias Indonesia | `resolveSubjectId`, `getSubjectName`, `getDisplayLabel`, `OFFICIAL_SUBJECTS` |
| `JournalCatalog` | Registry jurnal, pencocokan URL/nama, pemuatan dan penyimpanan JSON | `load`, `save`, `register`, `resolveOrigin`, `resolveSintaRank`, `matchesSinta` |
| `SintaResolver` | Lookup peringkat SINTA on-demand dan cache persisten | `getInstance`, `resolveRank`, `resolveInfo`, `cleanJournalName`, `normalizeKey` |
| `TextSanitizer` | Normalisasi HTML entity, whitespace, mojibake, judul, dan DOI | `clean`, `cleanTitle`, `cleanDoi`, `repairMojibake` |
| `UrlFilter` | Validasi URL artikel OJS dan kanonisasi URL | `isValidArticleUrl`, `extractArticleId`, `canonicalizeArticleUrl` |
| `QualityGate` | Pemeriksaan kualitas metadata artikel sebelum diterima | validasi judul, penulis, tahun, jurnal, URL, DOI, dan kelengkapan minimum |
| `DeduplicationEngine` | Menghapus duplikat artikel berdasarkan URL, DOI, dan judul | operasi deduplikasi pada koleksi artikel |
| `ArticleStorage` | Membaca/menulis artikel lokal JSON dan memindai workspace | `scanWorkspace`, `parseJson`, `saveJson` |
| `CitationExporter` | Ekspor artikel ke BibTeX dan RIS | `toBibTeX`, `toRIS`, `exportBibTeX`, `exportRIS` |

## Model `Article`

`Article` adalah data transfer object yang dipakai oleh crawler, GUI, penyimpanan, dan ekspor. Nilai kosong biasanya direpresentasikan sebagai string kosong atau `-`, bergantung pada tahap ekstraksi.

## Model `Journal` dan `JournalCatalog`

`Journal.create` membuat ID stabil berbentuk `garuda-<garudaId>`. Katalog memakai beberapa strategi pencocokan: ID Garuda, URL OJS/OAI, ISSN, nama jurnal yang dinormalisasi, dan sebagian nama jurnal. Katalog juga menyimpan `coreSubject` serta subjek spesifik.

## Normalisasi dan validasi

Pipeline memakai `TextSanitizer` sebelum deduplikasi dan quality gate. `UrlFilter` mencegah URL galley/PDF diperlakukan sebagai halaman artikel utama. `QualityGate` dan `DeduplicationEngine` digunakan untuk mencegah keluaran yang tidak layak atau berulang.
