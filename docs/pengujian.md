# Pengujian

## Pengelompokan test

| Test | Area yang diverifikasi |
|---|---|
| `GarudaSearchEngineTest` | parsing hasil Portal Garuda dan pagination |
| `WorkspaceControllerTest` | persistence project, koleksi, dan deduplikasi workspace |
| `ArticleExportServiceTest` | ekspor CSV dan JSON dari tabel artikel |
| `GarudaSubjectTest` | resolusi ID/nama/alias subjek |
| `JournalCatalogTest` | load, save, pencocokan, dan filter katalog |
| `SintaResolver` terkait | normalisasi nama/cache melalui test utilitas yang tersedia |
| `TextSanitizerTest` | entity HTML, mojibake, judul, dan DOI |
| `CitationExporterTest` | BibTeX dan RIS |
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

- HTML fixture Portal Garuda dan OJS menghasilkan `Article` dengan metadata utama.
- Filter SINTA dan tahun tidak meloloskan artikel di luar batas.
- JSON hasil ekspor dapat dibaca kembali oleh `ArticleStorage.parseJson`.
- BibTeX dan RIS memuat judul, penulis, jurnal, tahun, DOI, dan URL ketika tersedia.
