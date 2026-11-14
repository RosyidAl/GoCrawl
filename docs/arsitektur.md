# Arsitektur dan alur data

## Lapisan aplikasi

```text
GUI / CLI
   |
   +--> GarudaSearchEngine --------> Portal Garuda documents
   |          |                     (HTML hasil pencarian)
   |          +--> MetadataExtractor / TextSanitizer
   |          +--> JournalCatalog --> SintaResolver --> data/sinta_cache.json
   |
   +--> GarudaHarvester ------------> Portal Garuda area
   |          +--> JournalCatalog --> data/journals_master.json
   |
   +--> ArticleStorage / CitationExporter --> output/
```

## Alur pencarian

1. Pengguna memasukkan keyword dan filter pada GUI, atau `Main` membaca opsi CLI.
2. `GarudaSearchEngine.SearchOptions` menyimpan keyword, subjek, SINTA minimum, rentang tahun, dan halaman.
3. Mesin membentuk URL Portal Garuda, mengirim HTTP request, lalu menerima HTML.
4. HTML diparse menjadi `Article` melalui selector Portal Garuda.
5. Artikel disaring berdasarkan rentang tahun, subjek, dan SINTA bila katalog tersedia.
6. GUI melakukan pagination, cache halaman, deduplikasi hasil, dan mengirim artikel terpilih ke inspector.
7. Hasil dapat diekspor atau disimpan sebagai JSON/CSV.

## Alur katalog jurnal

1. `GarudaHarvester` mengambil halaman direktori berdasarkan ID subjek.
2. Baris jurnal dan halaman detail diparse menjadi `Journal`.
3. `JournalCatalog` mendaftarkan jurnal berdasarkan ID, nama, URL, ISSN, subjek, dan penerbit.
4. Katalog disimpan ke JSON melalui `JournalCatalog.save`.
5. Saat pencarian, katalog digunakan untuk asal/penerbit, subjek, dan resolusi SINTA.

## Batasan desain saat ini

- Parser bergantung pada selector HTML Portal Garuda, SINTA, dan OJS. Perubahan markup situs dapat memerlukan penyesuaian selector.
- Cache SINTA bersifat lokal dan persisten; nilai lama dapat bertahan sampai cache diperbarui atau dihapus.
- Parser JSON `ArticleStorage` dan `JournalCatalog` dibuat khusus untuk skema internal, bukan parser JSON umum.
- HTTP client memakai timeout singkat dan beberapa bagian crawler menangani kegagalan per halaman agar proses dapat dilanjutkan.
