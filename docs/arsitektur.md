# Arsitektur dan alur data

## Lapisan aplikasi

```text
GUI
   |
   +--> SearchController ----------> GarudaSearchEngine / OjsCrawler
   |          |                     (worker, cache, pagination)
   |          +--> MetadataExtractor / TextSanitizer
   |          +--> JournalCatalog --> SintaResolver --> data/sinta_cache.json
   |
   +--> WorkspaceController --------> ArticleStorage --> projects/ dan output/
   |
   +--> ArticleExportService / CitationExporter --> file ekspor
```

## Alur pencarian

1. Pengguna memasukkan keyword dan filter pada GUI.
2. `SearchController.Request` menyimpan keyword, SINTA minimum, rentang tahun, dan ukuran halaman.
3. Controller memilih sumber Garuda/OJS, menjalankan worker, lalu mengelola cache pagination.
4. Mesin crawler membentuk request dan memparse HTML menjadi `Article`.
5. Artikel disaring berdasarkan rentang tahun, subjek, SINTA, dan deduplikasi.
6. Hasil dikirim ke tabel dan artikel terpilih diteruskan ke inspector.
7. Workspace dan service ekspor menangani persistence secara terpisah dari UI.

## Alur katalog jurnal

1. GUI memuat `data/journals_master.json` melalui `JournalCatalog`.
2. `JournalCatalog` mencocokkan jurnal berdasarkan ID, nama, URL, ISSN, subjek, dan penerbit.
3. Saat pencarian, katalog digunakan untuk asal/penerbit, subjek, dan resolusi SINTA.

## Batasan desain saat ini

- Parser bergantung pada selector HTML Portal Garuda, SINTA, dan OJS. Perubahan markup situs dapat memerlukan penyesuaian selector.
- Cache SINTA bersifat lokal dan persisten; nilai lama dapat bertahan sampai cache diperbarui atau dihapus.
- Parser JSON `ArticleStorage` dan `JournalCatalog` dibuat khusus untuk skema internal, bukan parser JSON umum.
- HTTP client memakai timeout singkat dan beberapa bagian crawler menangani kegagalan per halaman agar proses dapat dilanjutkan.
