# Dokumentasi Teknis GoCrawl

Repositori ini memuat dokumentasi arsitektur, evaluasi kendala data, dan peta jalan pengembangan untuk **GoCrawl**, sistem penelusuran dan ekstraksi metadata publikasi ilmiah dari portal jurnal berbasis Open Journal Systems (OJS).

---

## Daftar Dokumen

| Dokumen | Deskripsi | Status |
|---|---|---|
| [Dokumentasi Sistem](file:///C:/Users/H0me%20PC/Projects/GoCrawl/docs/DOKUMENTASI_SISTEM.md) | Spesifikasi teknis, arsitektur modul, diagram alur crawling, model data, dan panduan operasional sistem saat ini (P1). | Aktif |
| [Evaluasi Masalah Crawling](file:///C:/Users/H0me%20PC/Projects/GoCrawl/docs/EVALUASI_MASALAH_CRAWLING.md) | *Root Cause Analysis* (RCA) mendalam mengenai isu duplikasi data (*artifact viewer* OJS / "View of") dan fenomena *missing values* pada dataset output. | Selesai |
| [Analisis & Roadmap Pengembangan](file:///C:/Users/H0me%20PC/Projects/GoCrawl/docs/ANALISIS_DAN_ROADMAP_PENGEMBANGAN.md) | Desain solusi arsitektural: engine deduplikasi multi-lapis, strategi ekstraksi kaskade (Jsoup & DOM), *quality gates*, serta pentahapan implementasi (Fase 1 & 2 terimplementasi). | Fase 1 & 2 Aktif |

---

## Ringkasan Singkat Masalah & Arah Solusi

Berdasarkan pengujian terhadap target jurnal di `journal.txt` (`jusan`, `tamaddun`, `ISLAMNUSANTARA`), ditemukan dua anomali kritis:
1. **Duplikasi Galley Viewer:** Crawler menelusuri URL `/article/view/{articleId}/{galleyId}` sebagai entitas artikel mandiri. Hal ini menyebabkan kemunculan judul berawalan `"View of ..."` atau duplikasi artikel dengan DOI galley yang berbeda (`.gXXXX`).
2. **Missing Values Signifikan:** Halaman galley tidak menyajikan tag sitasi HTML sehingga kolom penulis, abstrak, dan DOI menjadi kosong. Selain itu, kolom `published` gagal terekstrak pada hampir seluruh data akibat seleksi key meta tag yang tidak lengkap pada parser saat ini.

Solusi teknis terstruktur, perbaikan filter URL, skema normalisasi, dan roadmap arsitektur telah dirinci di dalam folder ini untuk dijadikan dasar refaktor pada tahap pengembangan berikutnya.
