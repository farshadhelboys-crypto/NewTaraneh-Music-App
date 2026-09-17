# 🎵 NewTaraneh Music App

اپلیکیشن رسمی اندروید کانال **نیو ترانه** (`@NewTaraneh`)

پخش آنلاین + دانلود + رابط کاربری شیک نئون آبی

## ویژگی‌ها
- پخش آنلاین با Media3 / ExoPlayer
- دانلود موزیک با نوتیفیکیشن
- جستجو و لیست جدیدترین‌ها
- تم دارک نئون مطابق برند
- دکمه پشتیبانی → `@NewTaranehAdmin`
- دکمه تبلیغات → `@NewTaranehAds`
- بک‌اند رایگان روی Cloudflare Workers + D1
- بیلد خودکار با GitHub Actions

## ساختار پروژه

```
NewTaraneh-Music-App/
├── android/                 # پروژه کامل اندروید (Kotlin + Compose)
├── backend/                 # Cloudflare Worker (TypeScript)
├── assets/                  # لوگو و تصاویر برند
├── .github/workflows/       # بیلد خودکار APK
└── README.md
```

## راه‌اندازی سریع

### ۱. بک‌اند (Cloudflare)

1. یک حساب Cloudflare بسازید (رایگان)
2. `wrangler` را نصب کنید:
   ```bash
   npm install -g wrangler
   wrangler login
   ```
3. به پوشه `backend` بروید و دستورات را اجرا کنید (جزئیات در `backend/README.md`)

### ۲. ربات تلگرام

ربات فعلی: `@appbotfornewtaranehbot`  
توکن را در **GitHub Secrets** و **Cloudflare Secrets** ذخیره کنید (هرگز در کد نگذارید).

### ۳. اپ اندروید

پروژه در پوشه `android` آماده است. با Android Studio باز کنید و بیلد بگیرید، یا از GitHub Actions استفاده کنید.

## لینک‌های رسمی
- کانال: https://t.me/NewTaraneh
- پشتیبانی: https://t.me/NewTaranehAdmin
- تبلیغات: https://t.me/NewTaranehAds
- وبلاگ: https://NewTaraneh.Blogfa.Com
- یوتیوب: https://youtube.com/@NewTaraneh

---
ساخته شده با ❤️ برای نیو ترانه
