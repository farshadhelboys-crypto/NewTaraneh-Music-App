# NewTaraneh Backend (Cloudflare Workers)

## نصب و دیپلوی

```bash
cd backend
npm install
```

### ۱. ساخت دیتابیس D1
```bash
npx wrangler d1 create newtaraneh-db
```
خروجی را کپی کنید و `database_id` را در `wrangler.toml` قرار دهید.

### ۲. ساخت جدول‌ها
```bash
npx wrangler d1 execute newtaraneh-db --file=./schema.sql
```

### ۳. تنظیم سکرت‌ها
```bash
npx wrangler secret put BOT_TOKEN
# توکن ربات را وارد کنید

npx wrangler secret put WEBHOOK_SECRET
# یک رشته تصادفی قوی وارد کنید (مثلاً از openssl rand -hex 32)
```

### ۴. دیپلوی
```bash
npx wrangler deploy
```

آدرس Worker شما چیزی شبیه این خواهد بود:
`https://newtaraneh-api.<your-subdomain>.workers.dev`

### ۵. تنظیم Webhook تلگرام
```bash
curl "https://api.telegram.org/bot<BOT_TOKEN>/setWebhook?url=https://newtaraneh-api.<subdomain>.workers.dev/webhook&secret_token=<WEBHOOK_SECRET>&allowed_updates=%5B%22channel_post%22%5D"
```

بعد از این، هر پست صوتی جدید در کانال به صورت خودکار به دیتابیس اضافه می‌شود.

## API Endpoints

| Method | Path | توضیح |
|--------|------|-------|
| GET | `/songs?page=1&limit=20&q=...` | لیست آهنگ‌ها |
| GET | `/songs/:id` | جزئیات یک آهنگ |
| GET | `/songs/:id/stream` | لینک موقت پخش |
| GET | `/songs/:id/download` | لینک موقت دانلود |
| GET | `/health` | وضعیت سرویس |

## نکته مهم
لینک‌های Telegram file حدود ۱ ساعت معتبر هستند. اپ اندروید باید هنگام پخش/دانلود دوباره درخواست بدهد.
