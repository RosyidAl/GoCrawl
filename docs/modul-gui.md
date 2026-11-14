# Modul GUI desktop

Semua kelas GUI berada di `id.gocrawl.gui` dan memakai Swing dengan FlatLaf.

## `GoCrawlGui`

Jendela utama aplikasi. Tanggung jawabnya:

- memuat `JournalCatalog`;
- menyediakan keyword, pencarian lanjutan, SINTA, dan rentang tahun;
- menjalankan pencarian melalui `SwingWorker` agar UI tetap responsif;
- mengelola cache halaman dan pagination;
- meneruskan artikel terpilih ke `DetailInspectorPanel`;
- mengelola proyek/folder workspace;
- membuka katalog jurnal dan dialog tambah jurnal.

Advanced Search dibuat sebagai overlay pada `JLayeredPane`, sehingga panel menempel di bawah field pencarian tanpa menggeser tabel dan inspector. Field lanjutannya mencakup judul jurnal/buku, tahun, penulis, afiliasi, akreditasi, dan rentang tahun.

## `ArticleTablePanel`

Menampilkan hasil artikel dalam `JTable`. Fitur utama:

- penyaringan lokal real-time;
- pemilihan artikel;
- pagination dan pilihan ukuran halaman;
- status progres;
- ekspor CSV, JSON, BibTeX, dan RIS.

## `DetailInspectorPanel`

Menampilkan metadata artikel terpilih dalam panel kanan bergulir. Informasi meliputi asal/penerbit, jurnal, subjek, SINTA, penulis, tahun, DOI, URL OJS, dan abstrak. Tersedia aksi membuka halaman web dan mengunduh PDF bila tautan tersedia.

## Dialog pendukung

| Kelas | Fungsi |
|---|---|
| `AddJournalDialog` | Menguji dan menambahkan jurnal sumber ke katalog |
| `IndexedJournalsDialog` | Melihat, mencari, menghapus, dan mengelola jurnal terindeks |

## Alur event pencarian

```text
keyword / Enter / Mulai Crawl
          |
          v
GoCrawlGui.handleCrawlToggle
          |
          v
SwingWorker -> GarudaSearchEngine.searchPage
          |
          v
ArticleTablePanel.setArticles
          |
          v
klik baris -> DetailInspectorPanel.setArticle
```
