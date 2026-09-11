# KadaiKu Brunei — Android Prototype v0.1

Prototype UI untuk marketplace small-business Brunei.

## Screens
- Home
- Search
- Cart
- Profile
- Product cards
- Seller/Delivery entry points

## Cara buka
Gunakan Android Studio, pilih **Open** dan pilih folder projek ini.
Kemudian Run pada Android Emulator atau telefon Android.

Ini masih prototype UI; payment, database, live delivery dan login belum disambungkan.

## Build APK dengan GitHub Actions
Project ini sudah disertakan workflow `.github/workflows/build-android.yml`.
Selepas project dimasukkan ke GitHub, buka tab **Actions** > **Build Android APK** > **Run workflow**.
Apabila build siap, download artifact **KadaiKuBrunei-debug-apk**. Di dalamnya ada `app-debug.apk` untuk testing pada Android.
