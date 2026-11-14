# Dokumentasi GoCrawl

Dokumentasi ini menjelaskan struktur, alur data, modul Java, antarmuka desktop, mode CLI, penyimpanan, dan pengujian GoCrawl.

## Daftar dokumen

- [Arsitektur dan alur data](arsitektur.md)
- [Modul domain dan utilitas](modul-domain.md)
- [Modul pencarian, crawling, dan metadata](modul-pipeline.md)
- [Modul GUI desktop](modul-gui.md)
- [Penyimpanan, ekspor, CLI, dan data](operasional.md)
- [Pengujian](pengujian.md)

## Gambaran singkat

GoCrawl adalah aplikasi Java 21 untuk mencari artikel ilmiah melalui Portal Garuda, mengaitkan hasilnya dengan katalog jurnal dan peringkat SINTA, lalu menampilkan, menyaring, menyimpan, dan mengekspor metadata artikel.

Implementasi memiliki dua antarmuka:

1. GUI Swing pada `id.gocrawl.gui.GoCrawlGui`.
2. CLI pada `id.gocrawl.Main`.

Sumber eksternal utama adalah Portal Garuda, halaman OJS jurnal, dan SINTA. Parser HTML memakai Jsoup; tema GUI memakai FlatLaf.

## Struktur repository

```text
src/main/java/id/gocrawl/       modul domain, crawling, parser, CLI
src/main/java/id/gocrawl/gui/   modul Swing GUI
src/test/java/id/gocrawl/       pengujian unit dan integrasi ringan
data/                           katalog jurnal dan cache SINTA
output/                         keluaran pencarian atau crawl
docs/                           dokumentasi proyek
lib/                            dependency lokal untuk build manual
```

## Menjalankan

GUI default:

```text
java -jar gocrawl.jar
```

CLI pencarian:

```text
java -jar gocrawl.jar --keyword "pendidikan karakter" --from-year 2021 --to-year 2024
```

Gunakan `--help` untuk daftar opsi lengkap.
