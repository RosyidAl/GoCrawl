# Pengujian

## Pengelompokan test

| Test | Area yang diverifikasi |
|---|---|
| `GarudaSearchEngineTest` | parsing hasil Portal Garuda dan pagination |
| `GarudaSubjectTest` | resolusi ID/nama/alias subjek |
| `JournalCatalogTest` | load, save, pencocokan, dan filter katalog |
| `SintaResolver` terkait | normalisasi nama/cache melalui test utilitas yang tersedia |
| `TextSanitizerTest` | entity HTML, mojibake, judul, dan DOI |
| `UrlFilterTest` | validasi dan kanonisasi URL OJS |
| `QualityGateTest` | penerimaan/rejeksi metadata artikel |
| `DeduplicationEngineTest` | deduplikasi berdasarkan identitas artikel |
| `CitationExporterTest` | BibTeX dan RIS |
| `OptionsTest` | parsing opsi CLI |
| `TestSupport` | fixture dan assertion bersama |
| `AllTests` | entry point untuk menjalankan seluruh test |

## Menjalankan

Jika Maven tersedia:

```text
mvn test
```

Script repository:

```text
run-tests.ps1
```

Test yang memerlukan jaringan atau HTML situs sebaiknya memakai fixture HTML dan dependency injection HTTP client jika tersedia. Jangan mengandalkan markup situs live untuk unit test deterministik.

## Kriteria penerimaan dasar

- HTML fixture yang valid menghasilkan `Article` dengan judul dan URL kanonikal.
- URL galley/PDF ditolak atau tidak dianggap sebagai URL artikel utama.
- Artikel dengan identitas URL/DOI/judul sama tidak muncul dua kali.
- Filter SINTA dan tahun tidak meloloskan artikel di luar batas.
- JSON hasil ekspor dapat dibaca kembali oleh `ArticleStorage.parseJson`.
- BibTeX dan RIS memuat judul, penulis, jurnal, tahun, DOI, dan URL ketika tersedia.
