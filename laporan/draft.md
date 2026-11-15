# Draft Kerangka Laporan Proyek: GoCrawl

Dokumen ini berisi poin-poin pokok rancangan laporan proyek GoCrawl yang dapat dikembangkan lebih lanjut.

---

## 1. Pendahuluan

### Latar Belakang
* **Pencarian referensi lokal masih terpisah-pisah:** Peneliti dan mahasiswa di Indonesia sering kesulitan mencari artikel ilmiah karena data tersebar di Portal Garuda dan ribuan web Open Journal Systems (OJS) milik masing-masing kampus.
* **Repot cek status akreditasi SINTA:** Portal Garuda tidak selalu menampilkan tingkatan akreditasi jurnal (SINTA 1 sampai S6) secara langsung. Pengguna harus membuka web SINTA secara manual satu per satu untuk mengecek reputasi jurnal.
* **Format sitasi tidak seragam:** Tidak semua portal jurnal menyediakan tombol ekspor sitasi yang rapi untuk aplikasi manajer referensi seperti Mendeley atau Zotero.
* **Kebutuhan alat bantu desktop yang praktis:** Dibutuhkan satu aplikasi yang bisa mencari artikel, memvalidasi peringkat SINTA otomatis, dan mengekspor daftar pustaka secara instan tanpa ribet.

### Ringkasan Masalah dan Solusi
| Masalah di Lapangan | Solusi di GoCrawl |
|---|---|
| Buka banyak tab browser (Garuda, SINTA, OJS kampus). | Semua pencarian dan data digabung dalam satu aplikasi desktop. |
| Peringkat SINTA tidak muncul di hasil pencarian Garuda. | Deteksi otomatis peringkat SINTA secara *on-demand* dengan cache lokal. |
| Repot salin sitasi manual ke skripsi/makalah. | Fitur ekspor satu klik ke format standar BibTeX (`.bib`) dan RIS (`.ris`). |
| Hasil pencarian sering bercampur file PDF rusak / metadata kosong. | Ada filter pembersih (*Quality Gate*) untuk menyaring artikel yang tidak valid. |

---

## 2. Pembahasan

### Fitur Utama Aplikasi
| Fitur | Fungsi Utama |
|---|---|
| **Pencarian Terpadu Garuda** | Mencari artikel berdasarkan kata kunci, rentang tahun, dan subjek bidang ilmu secara langsung dari Portal Garuda. |
| **Koleksi & Crawler OJS** | Menjelajahi langsung arsip edisi dan artikel dari web jurnal kampus berbasis OJS 3. |
| **Integrasi SINTA Otomatis** | Mencocokkan nama jurnal dengan peringkat akreditasi SINTA (S1–S6) serta menyimpan hasilnya di memori lokal agar hemat kuota. |
| **Penyaring Kualitas (Quality Gate)** | Menghapus artikel duplikat, membersihkan judul yang rusak (misal awalan "View of"), dan menolak artikel tanpa identitas jelas. |
| **Ekspor Sitasi Instan** | Mengekspor kumpulan artikel terpilih ke format BibTeX dan RIS yang siap dimasukkan ke Zotero/Mendeley. |
| **Manajemen Folder Koleksi** | Menyimpan artikel favorit ke dalam folder-folder proyek penelitian tersendiri di komputer. |
| **Antarmuka Desktop Simpel** | Tampilan GUI bersih (menggunakan FlatLaf modern) dengan tabel paginasi dan panel rincian artikel di sisi kanan. |

---

### Tantangan (Challenge)
* **Kestabilan server eksternal:** Portal Garuda dan SINTA milik pemerintah sering lambat, mengalami gangguan koneksi (*502/504*), atau membatasi akses jika ada terlalu banyak permintaan sekaligus (*HTTP 403 Forbidden*).
* **Variasi format OJS kampus:** Ribuan kampus menggunakan versi OJS yang berbeda-beda serta tata letak HTML yang tidak seragam, sehingga crawler harus dibuat fleksibel menangani variasi tersebut.
* **Masalah sertifikat SSL kampus:** Banyak web kampus menggunakan sertifikat keamanan (HTTPS) yang sudah kedaluwarsa atau tidak terdaftar resmi, sehingga koneksi rawan gagal jika tidak ditangani khusus.
* **Ketergantungan versi Java:** Aplikasi dibangun menggunakan Java 21, sehingga pengguna awam harus memastikan komputernya sudah memiliki Java versi yang sesuai.

---

### Peluang Pengembangan (Opportunity)
* **Integrasi AI / Ringkasan Abstrak:** Menambahkan fitur kecerdasan buatan untuk merangkum inti abstrak artikel atau merekomendasikan artikel yang relevan.
* **Pengunduh Berkas PDF Otomatis:** Menambahkan fitur unduh otomatis seluruh PDF artikel yang ditemukan hanya dengan sekali klik (*batch download*).
* **Penambahan Sumber Referensi Lain:** Membuka akses scraping ke portal nasional dan internasional lain seperti Moraref, Neliti, atau Google Scholar.
* **Pembuatan Installer Mandiri (`.exe`):** Membungkus aplikasi ke dalam bentuk paket instalasi mandiri agar pengguna tidak perlu memasang Java secara terpisah.
