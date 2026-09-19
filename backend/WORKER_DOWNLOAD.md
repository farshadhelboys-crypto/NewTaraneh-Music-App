# دانلود Worker Admin v2.3

## لینک مستقیم (کپی در Cloudflare)

**https://d.uguu.se/ZMFOmMvo.js**

پشتیبان: https://d.uguu.se/nehEpmns.js

## مراحل

1. روی لینک بالا کلیک کنید → کل متن را کپی کنید (Ctrl+A / Ctrl+C)
2. Cloudflare Dashboard → Workers → `newtaraneh-api` → Edit code
3. همه کد قبلی را پاک کنید و paste کنید
4. **Save and Deploy**
5. وب‌هوک تلگرام را دوباره ست کنید (ربات باید ادمین کانال باشد)

```
https://api.telegram.org/botYOUR_BOT_TOKEN/setWebhook?url=https://newtaraneh-api.farshadhelboys.workers.dev/webhook&allowed_updates=%5B%22channel_post%22%2C%22edited_channel_post%22%5D
```

6. تست: https://newtaraneh-api.farshadhelboys.workers.dev/
   باید `version: "2.3-admin"` ببینید

7. اگر آهنگ‌های قبلی حذف شده‌اند:
   https://newtaraneh-api.farshadhelboys.workers.dev/admin
   کلید: `newtaraneh_admin_2026` → **بازیابی همه**
