# 💰 Aplikasi Simple Expense — Pemrograman Mobile

Proyek ini merupakan implementasi aplikasi **Pencatat Pengeluaran (Expense Tracker) sederhana** menggunakan **Kotlin** di Android Studio. Aplikasi ini dirancang untuk membantu pengguna **mencatat, memantau, dan mengelola pengeluaran** sehari-hari.

Aplikasi ini menampilkan **daftar riwayat pengeluaran**, memungkinkan pengguna **menambah transaksi baru**, **melihat total pengeluaran**, serta memberikan antarmuka yang sederhana dan mudah digunakan.

---

## 🚀 Fitur Utama

- Menampilkan daftar **pengeluaran** dalam format list.
- Menambahkan data pengeluaran baru (Judul & Nominal) melalui input form.
- Menghapus riwayat pengeluaran jika terjadi kesalahan input.
- Menggunakan **RecyclerView** dan **Custom Adapter** untuk performa list yang optimal.
- Desain antarmuka (UI) yang bersih dan responsif.

---

## 🧠 Teknologi yang Digunakan

- **Android Studio** — IDE pengembangan.
- **Kotlin** — bahasa pemrograman utama.
- **XML Layout** — membangun UI aplikasi.
- **RecyclerView** — menampilkan list riwayat pengeluaran.
- **Adapter & ViewHolder** — pengatur data dan tampilan item.
- **ViewBinding** (opsional) — mempermudah akses komponen UI.

---

## 📦 Struktur Berkas Utama

| File | Fungsi |
|------|--------|
| `Expense.kt` | Data class penyimpanan struktur data pengeluaran (nama, nominal, tanggal). |
| `ExpenseAdapter.kt` | Adapter RecyclerView untuk menampilkan daftar pengeluaran. |
| `MainActivity.kt` | Logika utama untuk menampilkan list & menangani input data. |
| `activity_main.xml` | Layout halaman utama berisi form input dan RecyclerView. |
| `item_expense.xml` | Layout tampilan untuk setiap item transaksi pengeluaran. |

---

## ⚙️ Dependensi Utama (build.gradle)

- RecyclerView
- Material Design Components
- Kotlin Standard Library

Semua dependensi telah disiapkan untuk memastikan aplikasi berjalan dengan baik.

---

## 👤 Identitas Pengembang

- **Nama:** Ihza Ananda Rachman
- **NIM:** 2300018284
- **Slot:** Kamis, 08:30–10:00
- **Praktikum:** Pemrograman Mobile

---

## 📄 Ringkasan Repositori

| **Kategori** | **Detail** |
|--------------|------------|
| **Nama Repositori** | simpleexpense |
| **Pemilik** | Ihzaanandar |
| **Bahasa Utama** | Kotlin (100%) |
| **Deskripsi** | Aplikasi pencatat pengeluaran sederhana untuk mata kuliah Pemrograman Mobile. |
| **Struktur Proyek** | Menggunakan struktur standar Android: `.idea`, `app`, `gradle`, `build.gradle.kts`, `settings.gradle.kts`. |

---

## 📄 Lisensi

Proyek ini dibuat untuk keperluan **Mata Kuliah Pemrograman Mobile** dan bersifat akademik. Dilarang memperbanyak atau menggunakan kode tanpa izin dari pemilik repositori.
