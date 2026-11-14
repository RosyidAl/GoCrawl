# Operasional, CLI, dan data

## Mode CLI `Main`

| Mode | Opsi utama | Keluaran |
|---|---|---|
| Pencarian Portal Garuda | `--keyword`, `--area/--field`, `--min-sinta`, `--from-year`, `--to-year`, `--limit`, `--output` | `articles.csv`, `articles.json` |
| Panen katalog | `--harvest-catalog`, `--area`, `--all`, `--limit`, `--output` | `journals_master.json` |
| Daftar subjek | `--list-fields` | daftar 40 subjek di terminal |
| Direct OJS | `--direct-ojs`, `journal.txt`, opsi filter | folder per jurnal berisi artikel |

Jika tidak ada argumen, aplikasi membuka GUI. `--help` menampilkan penggunaan dan opsi.

## Berkas data

| Lokasi | Isi |
|---|---|
| `data/journals_master.json` | registry jurnal yang dipakai GUI dan filter SINTA |
| `data/sinta_cache.json` | cache lookup SINTA positif maupun negatif |
| `journal.txt` | daftar URL jurnal untuk direct OJS, satu URL per baris; baris `#` diabaikan |
| `output/` | artikel hasil pencarian/crawl dan ekspor |
| `projects/` | folder proyek yang dibuat dari sidebar GUI |

## Format keluaran

`ArticleStorage.saveJson` menulis array JSON dengan field `title`, `authors`, `year`, `journal`, `url`, `abstract`, `doi`, `published`, dan `keywords`. CSV CLI memuat kolom metadata utama. `CitationExporter` menghasilkan BibTeX dan RIS untuk Mendeley, Zotero, dan EndNote.

## Dependency dan build

- Java 21.
- Jsoup 1.18.3 untuk parsing HTML.
- FlatLaf 3.8 untuk tema Swing.
- `pom.xml` mendefinisikan dependency Maven.
- `build-jar.ps1` mengompilasi sumber, menggabungkan dependency lokal ke class output, dan membuat `gocrawl.jar`.
