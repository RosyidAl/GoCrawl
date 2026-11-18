# Dokumentasi GoCrawl

Dokumentasi ini menjelaskan struktur, alur data, modul Java, antarmuka desktop, penyimpanan, dan pengujian GoCrawl.

## Daftar dokumen

- [Arsitektur dan alur data](arsitektur.md)
- [Modul domain dan utilitas](modul-domain.md)
- [Modul pencarian, crawling, dan metadata](modul-pipeline.md)
- [Modul GUI desktop](modul-gui.md)
- [Penyimpanan, ekspor, dan data](operasional.md)
- [Pengujian](pengujian.md)

## Gambaran singkat

GoCrawl adalah aplikasi Java 21 untuk mencari artikel ilmiah melalui Portal Garuda, mengaitkan hasilnya dengan katalog jurnal dan peringkat SINTA, lalu menampilkan, menyaring, menyimpan, dan mengekspor metadata artikel.

Implementasi memiliki satu antarmuka utama:

1. GUI Swing pada `id.gocrawl.gui.GoCrawlGui`.

Sumber eksternal utama adalah Portal Garuda, halaman OJS jurnal, dan SINTA. Parser HTML memakai Jsoup; tema GUI memakai FlatLaf.

## Struktur repository

```text
src/main/java/id/gocrawl/       modul domain, crawling, dan parser
src/main/java/id/gocrawl/gui/   modul Swing GUI
src/test/java/id/gocrawl/       pengujian unit dan integrasi ringan
data/                           katalog jurnal dan cache SINTA
output/                         keluaran pencarian atau crawl
docs/                           dokumentasi proyek
lib/                            dependency lokal untuk build manual
```

## Menjalankan

GUI Desktop (Windows):
- Klik dua kali `buka-gocrawl.bat` (langsung membuka GUI) atau `run-gui.bat`.
- Atau via terminal:
  ```text
  java -jar gocrawl.jar
  ```
- Launcher lama `java -jar gocrawl.jar --gui` juga tetap didukung.

## Berbagi ke Teman / Distribusi Pengguna

Untuk membagikan GoCrawl ke pengguna akhir tanpa perlu menyertakan source code, cukup sertakan:
- `gocrawl.jar`
- `buka-gocrawl.bat`
- Folder `data/` (`crawl_sources.txt`, `journals_master.json`, `sinta_cache.json`)

*Prasyarat*: Komputer pengguna harus sudah terpasang **Java 21** atau versi lebih baru.
