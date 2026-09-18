# 🎵 NewTaraneh Music App

اپلیکیشن رسمی اندروید کانال **نیو ترانه** (`@NewTaraneh`)

پخش آنلاین + دانلود + رابط کاربری شیک نئون آبی (سبک آهنگیفای)

**API زنده:** https://newtaraneh-api.farshadhelboys.workers.dev

## ویژگی‌های فعلی
- دریافت خودکار آهنگ‌های جدید از کانال تلگرام
- نمایش کاور + عنوان + خواننده
- پخش آنلاین و دانلود مستقیم
- جستجو
- تم دارک نئون آبی
- دکمه پشتیبانی و تبلیغات
- بک‌اند Cloudflare Workers + D1 (رایگان)
- GitHub Actions برای بیلد APK

## ساختار پروژه

```
NewTaraneh-Music-App/
├── android/                 # اپ اندروید (Kotlin + Jetpack Compose)
├── backend/                 # Cloudflare Worker
├── .github/workflows/       # بیلد خودکار APK
└── README.md
```

## چگونه APK بگیری؟

### روش ۱: GitHub Actions (پیشنهادی)
1. برو به ریپو: https://github.com/farshadhelboys-crypto/NewTaraneh-Music-App
2. تب **Actions** را باز کن
3. روی **Build NewTaraneh APK** کلیک کن
4. **Run workflow** بزن
5. بعد از اتمام بیلد، فایل APK را از بخش Artifacts دانلود کن

### روش ۲: Android Studio
1. پروژه را Clone کن
2. پوشه `android` را با Android Studio باز کن
3. یک بار Sync و Build بگیر (تا gradlew ساخته شود)
4. بعد از آن Actions هم کامل کار می‌کند

## وضعیت بک‌اند
- Worker: فعال
- دیتابیس: فعال
- ربات: متصل به کانال
- هر پست صوتی جدید کانال خودکار اضافه می‌شود

## لینک‌های رسمی
- کانال: https://t.me/NewTaraneh
- پشتیبانی: https://t.me/NewTaranehAdmin
- تبلیغات: https://t.me/NewTaranehAds
- وبلاگ: https://NewTaraneh.Blogfa.Com
- یوتیوب: https://youtube.com/@NewTaraneh

---
ساخته شده با ❤️ برای نیو ترانه
