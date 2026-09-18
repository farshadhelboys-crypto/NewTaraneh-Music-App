/**
 * NewTaraneh Music Backend
 * Cloudflare Worker + D1 + Telegram Bot Webhook
 */

export interface Env {
  DB: D1Database;
  BOT_TOKEN: string;
  WEBHOOK_SECRET?: string;
}

interface TelegramUpdate {
  update_id: number;
  channel_post?: TelegramMessage;
  message?: TelegramMessage;
}

interface TelegramMessage {
  message_id: number;
  date: number;
  chat: { id: number; username?: string; type: string };
  caption?: string;
  text?: string;
  audio?: {
    file_id: string;
    file_unique_id: string;
    duration: number;
    file_size?: number;
    mime_type?: string;
    title?: string;
    performer?: string;
    thumb?: { file_id: string };
  };
  document?: {
    file_id: string;
    file_unique_id: string;
    file_name?: string;
    mime_type?: string;
    file_size?: number;
    thumb?: { file_id: string };
  };
}

function parseCaption(caption: string | undefined): { title: string; artist: string } {
  if (!caption) return { title: "آهنگ جدید", artist: "نیو ترانه" };

  const lines = caption.split("\n").map(l => l.trim()).filter(Boolean);
  let title = lines[0] || "آهنگ جدید";
  let artist = "نیو ترانه";

  // Common patterns in Persian music captions
  const artistMatch = caption.match(/(?:با صدای|خواننده|هنرمند|Artist)[:\s]*([^\n]+)/i);
  if (artistMatch) artist = artistMatch[1].trim();

  const titleMatch = caption.match(/(?:آهنگ|عنوان|Title)[:\s]*([^\n]+)/i);
  if (titleMatch) title = titleMatch[1].trim();

  // Clean title from common prefixes
  title = title
    .replace(/^(دانلود آهنگ جدید|دانلود|✅|🎵|🎶)\s*/gi, "")
    .replace(/\s*📥.*$/g, "")
    .trim();

  if (title.length > 80) title = title.slice(0, 77) + "...";
  if (artist.length > 50) artist = artist.slice(0, 47) + "...";

  return { title, artist };
}

async function handleChannelPost(msg: TelegramMessage, env: Env): Promise<void> {
  // Only process audio or audio documents
  const audio = msg.audio;
  const doc = msg.document;
  const isAudioDoc = doc && (doc.mime_type?.startsWith("audio/") || doc.file_name?.match(/\.(mp3|m4a|ogg|flac|wav)$/i));

  if (!audio && !isAudioDoc) return;

  const file = audio || doc!;
  const { title, artist } = parseCaption(msg.caption || msg.text);

  // Prefer Telegram's own title/performer if available
  const finalTitle = audio?.title || title;
  const finalArtist = audio?.performer || artist;

  const now = Math.floor(Date.now() / 1000);

  await env.DB.prepare(
    `INSERT OR REPLACE INTO songs 
     (message_id, file_id, file_unique_id, title, artist, caption, duration, file_size, mime_type, thumbnail_file_id, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      msg.message_id,
      file.file_id,
      file.file_unique_id || null,
      finalTitle,
      finalArtist,
      msg.caption || msg.text || null,
      audio?.duration || 0,
      file.file_size || 0,
      file.mime_type || "audio/mpeg",
      (audio?.thumb || doc?.thumb)?.file_id || null,
      msg.date || now,
      now
    )
    .run();
}

async function getFileUrl(fileId: string, env: Env): Promise<string | null> {
  const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getFile?file_id=${fileId}`);
  const data = await res.json() as any;
  if (!data.ok || !data.result?.file_path) return null;
  return `https://api.telegram.org/file/bot${env.BOT_TOKEN}/${data.result.file_path}`;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;

    // CORS
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, X-Telegram-Bot-Api-Secret-Token",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // ========== Webhook from Telegram ==========
    if (path === "/webhook" && request.method === "POST") {
      // Optional secret check
      if (env.WEBHOOK_SECRET) {
        const secret = request.headers.get("X-Telegram-Bot-Api-Secret-Token");
        if (secret !== env.WEBHOOK_SECRET) {
          return new Response("Unauthorized", { status: 401 });
        }
      }

      try {
        const update = await request.json() as TelegramUpdate;
        if (update.channel_post) {
          await handleChannelPost(update.channel_post, env);
        }
        return new Response("OK");
      } catch (e) {
        console.error("Webhook error:", e);
        return new Response("Error", { status: 500 });
      }
    }

    // ========== Public API ==========

    // GET /songs?page=1&limit=20&q=search
    if (path === "/songs" && request.method === "GET") {
      const page = Math.max(1, parseInt(url.searchParams.get("page") || "1"));
      const limit = Math.min(50, Math.max(1, parseInt(url.searchParams.get("limit") || "20")));
      const offset = (page - 1) * limit;
      const q = url.searchParams.get("q")?.trim();

      let query = `SELECT id, message_id, title, artist, duration, file_size, thumbnail_file_id, created_at 
                  FROM songs`;
      const params: any[] = [];

      if (q) {
        query += ` WHERE title LIKE ? OR artist LIKE ? OR caption LIKE ?`;
        const like = `%${q}%`;
        params.push(like, like, like);
      }

      query += ` ORDER BY created_at DESC LIMIT ? OFFSET ?`;
      params.push(limit, offset);

      const { results } = await env.DB.prepare(query).bind(...params).all();

      return new Response(JSON.stringify({ page, limit, songs: results }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // GET /songs/:id
    if (path.match(/^\/songs\/\d+$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT * FROM songs WHERE id = ?`).bind(id).first();
      if (!song) return new Response("Not found", { status: 404, headers: corsHeaders });
      return new Response(JSON.stringify(song), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // GET /songs/:id/cover → temporary Telegram thumbnail URL
    if (path.match(/^\/songs\/\d+\/cover$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(
        `SELECT thumbnail_file_id FROM songs WHERE id = ?`
      ).bind(id).first() as any;

      if (!song) return new Response("Not found", { status: 404, headers: corsHeaders });
      if (!song.thumbnail_file_id) {
        return new Response("No cover", { status: 404, headers: corsHeaders });
      }

      const fileUrl = await getFileUrl(song.thumbnail_file_id, env);
      if (!fileUrl) {
        return new Response("Cover unavailable", { status: 502, headers: corsHeaders });
      }

      return Response.redirect(fileUrl, 302);
    }

    // GET /songs/:id/stream  → temporary Telegram file URL
    if (path.match(/^\/songs\/\d+\/stream$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT file_id FROM songs WHERE id = ?`).bind(id).first() as any;
      if (!song) return new Response("Not found", { status: 404, headers: corsHeaders });

      const fileUrl = await getFileUrl(song.file_id, env);
      if (!fileUrl) return new Response("File unavailable", { status: 502, headers: corsHeaders });

      // Redirect to Telegram CDN (valid ~1 hour)
      return Response.redirect(fileUrl, 302);
    }

    // GET /songs/:id/download  (same as stream for now)
    if (path.match(/^\/songs\/\d+\/download$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT file_id, title FROM songs WHERE id = ?`).bind(id).first() as any;
      if (!song) return new Response("Not found", { status: 404, headers: corsHeaders });

      const fileUrl = await getFileUrl(song.file_id, env);
      if (!fileUrl) return new Response("File unavailable", { status: 502, headers: corsHeaders });

      return Response.redirect(fileUrl, 302);
    }

    // Health
    if (path === "/" || path === "/health") {
      const count = await env.DB.prepare(`SELECT COUNT(*) as c FROM songs`).first() as any;
      return new Response(JSON.stringify({
        status: "ok",
        app: "NewTaraneh Music API",
        songs: count?.c || 0,
      }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    return new Response("Not Found", { status: 404, headers: corsHeaders });
  },
};
