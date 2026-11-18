# Modul GUI desktop

Semua kelas GUI berada di `id.gocrawl.gui` dan memakai Swing dengan FlatLaf.

## `GoCrawlGui`

Jendela utama dan coordinator wiring aplikasi. Tanggung jawabnya:

- memuat `JournalCatalog`;
- membangun layout, menu, sidebar, dan field pencarian;
- menghubungkan callback antar-panel dan controller;
- meneruskan artikel terpilih ke `DetailInspectorPanel`;
- membuka dialog pengelolaan sumber crawl.

Logika pencarian, workspace, dan ekspor tidak disimpan di jendela utama.

## Controller dan service GUI

| Kelas | Tanggung jawab |
|---|---|
| `SearchController` | State query, worker asynchronous, cache pagination, filter, serta dispatch Garuda/OJS |
| `WorkspaceController` | Project, favorit, koleksi, persistence JSON, dan operasi filesystem |
| `ArticleExportService` | Penulisan CSV/JSON; BibTeX dan RIS didelegasikan ke `CitationExporter` |

Advanced Search dibuat sebagai overlay pada `JLayeredPane`, sehingga panel menempel di bawah field pencarian tanpa menggeser tabel dan inspector. Field lanjutannya mencakup judul jurnal/buku, tahun, penulis, afiliasi, akreditasi, dan rentang tahun.

## `ArticleTablePanel`

Menampilkan hasil artikel dalam `JTable`. Fitur utama:

- penyaringan lokal real-time;
- pemilihan artikel;
- pagination dan pilihan ukuran halaman;
- status progres;
- dialog ekspor dan callback ke `ArticleExportService`/`CitationExporter`.

## `DetailInspectorPanel`

Menampilkan metadata artikel terpilih dalam panel kanan bergulir. Informasi meliputi asal/penerbit, jurnal, subjek, SINTA, penulis, tahun, DOI, URL OJS, dan abstrak. Tersedia aksi membuka halaman web artikel.

## Dialog pendukung

| Kelas | Fungsi |
|---|---|
| `CrawlSourcesDialog` | Menguji, menambah, mengaktifkan, dan menghapus sumber crawl |

## Alur event pencarian

```text
keyword / Enter / Mulai Crawl
          |
          v
GoCrawlGui -> SearchController
          |
          v
SwingWorker -> GarudaSearchEngine / OjsCrawler
          |
          v
ArticleTablePanel.setArticles
          |
          v
klik baris -> DetailInspectorPanel.setArticle
```
