# Quriva

Aplikasi Android open source untuk membaca Al-Qur'an dengan antarmuka hitam-putih yang modern, teks Utsmani, terjemahan Bahasa Indonesia (Kemenag RI), dan dukungan offline untuk teks inti.

## Fitur

**Membaca**
- 114 surah dengan nama Arab, nama Latin, arti, jumlah ayat, dan tempat turun
- Teks Arab Utsmani + terjemahan Bahasa Indonesia per ayat (dibundel offline)
- Opsi tampilan: tajwid berwarna, kata-per-kata, dan transliterasi Latin (diambil dari Quran.com saat dibutuhkan)
- Mode fokus (sembunyikan terjemahan), tema sistem/terang/gelap, dan pengaturan ukuran teks
- Kartu “Terakhir dibaca”, penyimpanan posisi bacaan otomatis, dan ayat hari ini di beranda
- Navigasi 30 Juz lengkap dengan rentang surah dan ayat

**Audio murottal**
- Pemutaran per surah **per ayat** dari Quran.com (12 qari)
- **Sorot & lompat ayat** serta opsi gulir otomatis mengikuti ayat yang diputar
- Kontrol putar/jeda, sebelumnya/berikutnya, posisi audio, kecepatan putar, mode ulang (mati/surah/ayat), dan timer tidur
- **Latar belakang seperti pemutar musik**: tetap berjalan saat aplikasi ditinggalkan/layar mati dengan notifikasi media dan kontrol lock screen/headset/Bluetooth
- **Lanjutkan audio** (posisi terakhir diingat) dan **unduh audio per surah** untuk diputar offline
- Mini player di layar baca

**Tafsir, terjemahan & pemahaman**
- Tafsir per ayat online dari Quran.com: Ibn Kathir (Inggris), Ma'arif al-Qur'an, Tazkirul Quran, Ibn Kathir/Al-Qurtubi/As-Sa'di/Al-Baghawi/Muyassar (Arab)
- Teks inti + terjemahan Kemenag RI tersedia offline; terjemahan/tafsir tambahan dimuat online
- Catatan: tafsir berbahasa Indonesia belum tersedia di API Quran.com

**Pencarian & koleksi**
- Pencarian surah (nama, arti, nomor, Arab) dan filter Makkiyah/Madaniyah
- Pencarian global offline ke seluruh 6.236 ayat (Arab/terjemahan/referensi seperti `2:255`) dengan sorotan hasil
- **Bookmark** ayat tersimpan di perangkat (dengan pratinjau teks) serta **catatan pribadi** per ayat

**Progres & kebiasaan**
- Pelacak progres bacaan harian, estimasi khatam, target khatam, dan statistik
- Pengingat baca harian (preferensi di aplikasi)

**Berbagi**
- Salin/bagikan ayat (teks dengan tautan quran.com)
- Bagikan sebagai **kartu ayat** (gambar)

## Sumber data

- **Teks Arab Utsmani & terjemahan Indonesia**: [Quran.com](https://quran.com) API v4
  - Teks: `quran/verses/uthmani`
  - Terjemahan: `quran/translations/33` (Kementerian Agama RI)
  - Metadata surah: `chapters?language=id`
  - Pembagian juz: `juzs`
- Data teks inti dibundel sebagai aset lokal:
  - `app/src/main/assets/surahs.json`
  - `app/src/main/assets/juzs.json`
  - `app/src/main/assets/quran/001.json` ... `114.json`
- **Audio murottal**: `chapter_recitations` dan `recitations/.../by_chapter` (streaming/unduhan, memerlukan internet untuk akses awal)
- **Tafsir**: `tafsirs/{id}/by_ayah/{surah}:{ayah}` (online)
- **Kata-per-kata, transliterasi, tajwid**: `verses/by_chapter` (words) dan `quran/verses/uthmani_tajweed` (online, dimuat sesuai kebutuhan)
- **Font**: [Amiri](https://github.com/aliftype/amiri) (SIL Open Font License), tersedia di `app/src/main/assets/licenses/amiri-OFL.txt`.

## Arsitektur singkat

- Kotlin + Jetpack Compose + Material 3
- MVVM ringan dengan `AndroidViewModel` dan `StateFlow`
- Data per surah dimuat dari aset secara lazy; indeks pencarian global dinormalisasi sekali di memori
- Navigasi: AndroidX Navigation Compose
- Audio: AndroidX Media3 ExoPlayer + `MediaSessionService` untuk pemutaran latar belakang/per-ayat
- Build release memakai R8 dan resource shrinking
- Preferensi, bookmark, catatan, progres, dan posisi baca/audio: SharedPreferences/JSON lokal
- Teks inti sepenuhnya offline; audio, tafsir, kata-per-kata, tajwid, dan terjemahan tambahan memerlukan internet

Izin jaringan dipakai untuk audio, tafsir, dan fitur studi online. Izin foreground media playback, wake lock, dan notifikasi mendukung pemutaran murottal di latar belakang. Aplikasi tidak meminta akses ke kontak, lokasi, kamera, atau penyimpanan pengguna.

## Build

Prasyarat: JDK 17+, Android SDK dengan platform 36.

```bash
ANDROID_HOME=/path/to/android-sdk ./gradlew assembleDebug
ANDROID_HOME=/path/to/android-sdk ./gradlew assembleRelease
```

APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`; APK release yang
telah dioptimalkan R8 ada di `app/build/outputs/apk/release/app-release.apk`.

## Lisensi

Kode aplikasi: MIT (lihat `LICENSE`).

Konten Al-Qur'an, terjemahan, audio, tafsir, dan font memiliki lisensi masing-masing seperti dijelaskan di bagian sumber data dan di dalam aplikasi.
