# Arsitektur dan alur data

## Lapisan aplikasi

```text
GUI
   |
   +--> GarudaSearchEngine --------> Portal Garuda documents
   |          |                     (HTML hasil pencarian)
   |          +--> MetadataExtractor / TextSanitizer
   |          +--> JournalCatalog --> SintaResolver --> data/sinta_cache.json
   |
   +--> ArticleStorage / CitationExporter --> output/
```

## Alur pencarian

1. Pengguna memasukkan keyword dan filter pada GUI.
2. `GarudaSearchEngine.SearchOptions` menyimpan keyword, subjek, SINTA minimum, rentang tahun, dan halaman.
3. Mesin membentuk URL Portal Garuda, mengirim HTTP request, lalu menerima HTML.
4. HTML diparse menjadi `Article` melalui selector Portal Garuda.
5. Artikel disaring berdasarkan rentang tahun, subjek, dan SINTA bila katalog tersedia.
6. GUI melakukan pagination, cache halaman, deduplikasi hasil, dan mengirim artikel terpilih ke inspector.
7. Hasil dapat diekspor atau disimpan sebagai JSON/CSV.

## Alur katalog jurnal

1. GUI memuat `data/journals_master.json` melalui `JournalCatalog`.
2. `JournalCatalog` mencocokkan jurnal berdasarkan ID, nama, URL, ISSN, subjek, dan penerbit.
3. Saat pencarian, katalog digunakan untuk asal/penerbit, subjek, dan resolusi SINTA.

## Batasan desain saat ini

- Parser bergantung pada selector HTML Portal Garuda, SINTA, dan OJS. Perubahan markup situs dapat memerlukan penyesuaian selector.
- Cache SINTA bersifat lokal dan persisten; nilai lama dapat bertahan sampai cache diperbarui atau dihapus.
- Parser JSON `ArticleStorage` dan `JournalCatalog` dibuat khusus untuk skema internal, bukan parser JSON umum.
- HTTP client memakai timeout singkat dan beberapa bagian crawler menangani kegagalan per halaman agar proses dapat dilanjutkan.
