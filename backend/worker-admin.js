/**
 * NewTaraneh Worker Admin v2.3
 *
 * فایل کامل (~20KB) را از یکی از لینک‌های زیر دانلود کنید،
 * سپس در Cloudflare Workers → Edit code → Paste → Deploy.
 *
 * دانلود مستقیم:
 *   https://d.uguu.se/ZMFOmMvo.js
 *   https://d.uguu.se/nehEpmns.js
 *
 * بعد از Deploy وب‌هوک را ست کنید:
 *   https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://newtaraneh-api.farshadhelboys.workers.dev/webhook&allowed_updates=%5B%22channel_post%22%2C%22edited_channel_post%22%5D
 *
 * پنل ادمین: https://newtaraneh-api.farshadhelboys.workers.dev/admin
 * کلید: newtaraneh_admin_2026
 *
 * این فایل placeholder است — کد کامل را از لینک بالا بگیرید.
 */
export default {
  async fetch() {
    return new Response(
      JSON.stringify({
        error: "Deploy full worker from https://d.uguu.se/ZMFOmMvo.js",
        version: "placeholder",
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  },
};
