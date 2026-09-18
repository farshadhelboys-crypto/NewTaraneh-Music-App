/**
 * NewTaraneh Music Backend - Proxy mode
 * Audio & covers stream through Cloudflare so users in Iran need no VPN.
 */

export interface Env {
  DB: D1Database;
  BOT_TOKEN: string;
  WEBHOOK_SECRET?: string;
}

function parseCaption(caption?: string): { title: string; artist: string } {
  if (!caption) return { title: "آهنگ جدید", artist: "نیو ترانه" };
  const lines = caption.split("\n").map((l) => l.trim()).filter(Boolean);
  let title = lines[0] || "آهنگ جدید";
  let artist = "نیو ترانه";
  const artistMatch = caption.match(/(?:با صدای|خواننده|هنرمند|Artist|از)[:\s]*([^\n]+)/i);
  if (artistMatch) artist = artistMatch[1].trim();
  const titleMatch = caption.match(/(?:آهنگ|عنوان|Title)[:\s]*([^\n]+)/i);
  if (titleMatch) title = titleMatch[1].trim();
  title = title
    .replace(/^(دانلود آهنگ جدید|دانلود آهنگ|دانلود|✅|🎵|🎶|🆕)\s*/gi, "")
    .replace(/\s*📥.*$/g, "")
    .replace(/\s*@NewTaraneh.*$/gi, "")
    .trim();
  if (title.length > 90) title = title.slice(0, 87) + "...";
  if (artist.length > 50) artist = artist.slice(0, 47) + "...";
  return { title, artist };
}

async function handleChannelPost(msg: any, env: Env): Promise<void> {
  const audio = msg.audio;
  const doc = msg.document;
  const photo = msg.photo;
  const isAudioDoc =
    doc &&
    (doc.mime_type?.startsWith("audio/") ||
      (doc.file_name && /\.(mp3|m4a|ogg|flac|wav)$/i.test(doc.file_name)));
  if (!audio && !isAudioDoc) return;

  const file = audio || doc;
  const { title, artist } = parseCaption(msg.caption || msg.text);
  const finalTitle = audio?.title || title;
  const finalArtist = audio?.performer || artist;

  let thumbFileId: string | null = null;
  if (audio?.thumb) thumbFileId = audio.thumb.file_id;
  else if (doc?.thumb) thumbFileId = doc.thumb.file_id;
  else if (photo?.length) thumbFileId = photo[photo.length - 1].file_id;

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
      thumbFileId,
      msg.date || now,
      now
    )
    .run();
}

async function getFileUrl(fileId: string, env: Env): Promise<string | null> {
  const res = await fetch(
    `https://api.telegram.org/bot${env.BOT_TOKEN}/getFile?file_id=${fileId}`
  );
  const data: any = await res.json();
  if (!data.ok || !data.result?.file_path) return null;
  return `https://api.telegram.org/file/bot${env.BOT_TOKEN}/${data.result.file_path}`;
}

/** Stream Telegram file body through this Worker (no client-side Telegram access needed). */
async function proxyFile(
  fileId: string,
  env: Env,
  contentType?: string
): Promise<Response> {
  const fileUrl = await getFileUrl(fileId, env);
  if (!fileUrl) return new Response("File unavailable", { status: 502 });
  const upstream = await fetch(fileUrl);
  if (!upstream.ok) return new Response("Upstream error", { status: 502 });
  const headers: Record<string, string> = {
    "Content-Type":
      contentType ||
      upstream.headers.get("Content-Type") ||
      "application/octet-stream",
    "Cache-Control": "public, max-age=3600",
    "Access-Control-Allow-Origin": "*",
  };
  const len = upstream.headers.get("Content-Length");
  if (len) headers["Content-Length"] = len;
  return new Response(upstream.body, { status: 200, headers });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });

    if (path === "/webhook" && request.method === "POST") {
      try {
        const update: any = await request.json();
        if (update.channel_post) await handleChannelPost(update.channel_post, env);
        return new Response("OK");
      } catch {
        return new Response("Error", { status: 500 });
      }
    }

    if (path === "/songs" && request.method === "GET") {
      const page = Math.max(1, parseInt(url.searchParams.get("page") || "1"));
      const limit = Math.min(50, Math.max(1, parseInt(url.searchParams.get("limit") || "20")));
      const offset = (page - 1) * limit;
      const q = (url.searchParams.get("q") || "").trim();
      let query =
        `SELECT id, message_id, title, artist, duration, file_size, thumbnail_file_id, created_at, caption FROM songs`;
      const params: any[] = [];
      if (q) {
        query += ` WHERE title LIKE ? OR artist LIKE ? OR caption LIKE ?`;
        const like = `%${q}%`;
        params.push(like, like, like);
      }
      query += ` ORDER BY created_at DESC LIMIT ? OFFSET ?`;
      params.push(limit, offset);
      const { results } = await env.DB.prepare(query).bind(...params).all();
      const origin = url.origin;
      const songs = (results || []).map((s: any) => ({
        ...s,
        cover_url: s.thumbnail_file_id ? `${origin}/songs/${s.id}/cover` : null,
      }));
      return new Response(JSON.stringify({ page, limit, songs }), {
        headers: { ...cors, "Content-Type": "application/json" },
      });
    }

    if (path.match(/^\/songs\/\d+$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song: any = await env.DB.prepare(`SELECT * FROM songs WHERE id = ?`).bind(id).first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      return new Response(
        JSON.stringify({
          ...song,
          stream_url: `${url.origin}/songs/${id}/stream`,
          cover_url: song.thumbnail_file_id ? `${url.origin}/songs/${id}/cover` : null,
        }),
        { headers: { ...cors, "Content-Type": "application/json" } }
      );
    }

    if (path.match(/^\/songs\/\d+\/cover$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song: any = await env.DB
        .prepare(`SELECT thumbnail_file_id FROM songs WHERE id = ?`)
        .bind(id)
        .first();
      if (!song?.thumbnail_file_id) return new Response("No cover", { status: 404, headers: cors });
      return proxyFile(song.thumbnail_file_id, env, "image/jpeg");
    }

    if (path.match(/^\/songs\/\d+\/stream$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song: any = await env.DB
        .prepare(`SELECT file_id, mime_type FROM songs WHERE id = ?`)
        .bind(id)
        .first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      return proxyFile(song.file_id, env, song.mime_type || "audio/mpeg");
    }

    if (path.match(/^\/songs\/\d+\/download$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song: any = await env.DB
        .prepare(`SELECT file_id, title, mime_type FROM songs WHERE id = ?`)
        .bind(id)
        .first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      const resp = await proxyFile(song.file_id, env, song.mime_type || "audio/mpeg");
      if (resp.status !== 200) return resp;
      const headers = new Headers(resp.headers);
      const safe = String(song.title || "song").replace(/[^\w\u0600-\u06FF\- ]+/g, "_").slice(0, 80);
      headers.set("Content-Disposition", `attachment; filename="${safe}.mp3"`);
      return new Response(resp.body, { status: 200, headers });
    }

    if (path === "/" || path === "/health") {
      try {
        const count: any = await env.DB.prepare(`SELECT COUNT(*) as c FROM songs`).first();
        return new Response(
          JSON.stringify({
            status: "ok",
            app: "NewTaraneh Music API",
            songs: count?.c || 0,
            proxy: true,
          }),
          { headers: { ...cors, "Content-Type": "application/json" } }
        );
      } catch {
        return new Response(JSON.stringify({ status: "ok", songs: 0 }), {
          headers: { ...cors, "Content-Type": "application/json" },
        });
      }
    }

    return new Response("Not Found", { status: 404, headers: cors });
  },
};
