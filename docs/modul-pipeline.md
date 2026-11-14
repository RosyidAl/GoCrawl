# Modul pencarian, crawling, dan metadata

## `GarudaSearchEngine`

Mesin pencarian Portal Garuda pada endpoint `/documents`.

### Input

`SearchOptions` berisi:

- `keyword`
- `subjectScopeId`
- `minSintaRank`
- `fromYear` dan `toYear`
- `page`

### Output

`PageResult` berisi daftar `Article`, halaman aktif, total halaman, total record, dan flag `hasMore`.

### Tahapan

1. Validasi keyword.
2. Pembentukan URL dan query parameter tahun/halaman.
3. HTTP GET dengan timeout dan user agent browser.
4. Parsing pagination.
5. Parsing item artikel: judul, penulis, jurnal, tahun, abstrak, DOI, URL, dan metadata lain.
6. Penyaringan tahun, SINTA melalui `JournalCatalog`, dan subjek.
7. Deduplikasi lokal dalam satu halaman.

`parseDocumentPage` dapat digunakan langsung pada HTML fixture tanpa melakukan HTTP, sehingga cocok untuk pengujian parser.

## `GarudaHarvester`

Pemanen katalog jurnal dari direktori `/area/index/<subjectId>` Portal Garuda.

`HarvestOptions` menentukan subjek, mode seluruh subjek, batas per subjek, dan file keluaran. Harvester mengambil baris jurnal, membuka halaman detail, mengekstrak penerbit, ISSN/eISSN, OJS, OAI, core subject, dan multi-subject, lalu menyimpan progres katalog setelah setiap subjek.

## `MetadataExtractor`

Ekstraktor fallback untuk halaman OJS. Urutan ekstraksi memprioritaskan meta tag akademik Highwire/Dublin Core/OpenGraph, lalu selector DOM OJS modern, lalu fallback judul dokumen atau nama jurnal yang diberikan.

Field yang diekstrak:

- judul dan penulis
- tanggal/tahun publikasi
- abstrak
- DOI
- nama jurnal
- URL kanonikal
- kata kunci

## `SintaResolver`

Resolver menggunakan nama jurnal yang dinormalisasi sebagai cache key. Urutannya:

1. Cari di memory cache.
2. Jika belum ada, query halaman SINTA.
3. Cocokkan nama jurnal dan ekstrak peringkat, penerbit, URL OJS, serta ID Garuda.
4. Simpan hasil atau marker negatif ke `data/sinta_cache.json`.

## `Main.OjsCrawler`

Mode direct OJS membaca URL dari `journal.txt`, mengambil halaman dan tautan artikel, memvalidasi URL melalui `UrlFilter`, mengambil HTML artikel, mengekstrak metadata, menjalankan quality gate/deduplikasi, lalu menulis JSON/CSV per jurnal.
