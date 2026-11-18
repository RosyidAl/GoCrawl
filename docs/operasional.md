# Operasional, GUI, dan data

## Launcher desktop

`id.gocrawl.Main` hanya bertugas membuka GUI Swing. Jalankan `java -jar gocrawl.jar`
atau gunakan `--gui` sebagai alias eksplisit.

## Berkas data

| Lokasi | Isi |
|---|---|
| `data/journals_master.json` | registry jurnal yang dipakai GUI dan filter SINTA |
| `data/sinta_cache.json` | cache lookup SINTA positif maupun negatif |
| `projects/` | folder proyek yang dibuat dari sidebar GUI |

## Format keluaran

`ArticleStorage.saveJson` menulis array JSON dengan field `title`, `authors`, `year`, `journal`, `url`, `abstract`, `doi`, `published`, dan `keywords`. GUI menyediakan ekspor CSV, JSON, BibTeX, dan RIS melalui tabel artikel. `CitationExporter` menghasilkan BibTeX dan RIS untuk Mendeley, Zotero, dan EndNote.

## Dependency dan build

- Java 21.
- Jsoup 1.18.3 untuk parsing HTML.
- FlatLaf 3.8 untuk tema Swing.
- `pom.xml` mendefinisikan dependency Maven.
- `build-jar.ps1` mengompilasi sumber, menggabungkan dependency lokal ke class output, dan membuat `gocrawl.jar`.
