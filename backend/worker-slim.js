function parseCaption(caption) {
  if (!caption) return { title: "آهنگ جدید", artist: "نیو ترانه", suggested: false };
  const lines = caption.split("\n").map(l => l.trim()).filter(Boolean);
  let title = lines[0] || "آهنگ جدید";
  let artist = "نیو ترانه";
  const artistMatch = caption.match(/(?:با صدای|خواننده|هنرمند|Artist|از)[:\s]*([^\n]+)/i);
  if (artistMatch) artist = artistMatch[1].trim();
  const titleMatch = caption.match(/(?:آهنگ|عنوان|Title)[:\s]*([^\n]+)/i);
  if (titleMatch) title = titleMatch[1].trim();
  title = title.replace(/^(دانلود آهنگ جدید|دانلود آهنگ|دانلود|✅|🎵|🎶|🆕|🔥)\s*/gi, "").replace(/\s*📥.*$/g, "").replace(/\s*@NewTaraneh.*$/gi, "").trim();
  if (title.length > 90) title = title.slice(0, 87) + "...";
  if (artist.length > 50) artist = artist.slice(0, 47) + "...";
  const suggested = /#\s*(پیشنهادی|ویژه|suggested|special|vip)/i.test(caption);
  return { title, artist, suggested };
}
async function ensureSchema(env) {
  try { await env.DB.prepare(`ALTER TABLE songs ADD COLUMN is_active INTEGER DEFAULT 1`).run(); } catch (e) {}
  try { await env.DB.prepare(`ALTER TABLE songs ADD COLUMN is_suggested INTEGER DEFAULT 0`).run(); } catch (e) {}
}
async function handleChannelPost(msg, env) {
  const audio = msg.audio;
  const doc = msg.document;
  const photo = msg.photo;
  const isAudioDoc = doc && (doc.mime_type?.startsWith("audio/") || (doc.file_name && doc.file_name.match(/\.(mp3|m4a|ogg|flac|wav)$/i)));
  if (!audio && !isAudioDoc) return;
  const file = audio || doc;
  const { title, artist, suggested } = parseCaption(msg.caption || msg.text);
  const finalTitle = (audio && audio.title) || title;
  const finalArtist = (audio && audio.performer) || artist;
  let thumbFileId = null;
  if (audio && audio.thumb) thumbFileId = audio.thumb.file_id;
  else if (doc && doc.thumb) thumbFileId = doc.thumb.file_id;
  else if (photo && photo.length > 0) thumbFileId = photo[photo.length - 1].file_id;
  const now = Math.floor(Date.now() / 1000);
  await env.DB.prepare(`INSERT OR REPLACE INTO songs (message_id, file_id, file_unique_id, title, artist, caption, duration, file_size, mime_type, thumbnail_file_id, created_at, updated_at, is_active, is_suggested) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)`)
    .bind(msg.message_id, file.file_id, file.file_unique_id || null, finalTitle, finalArtist, msg.caption || msg.text || null, (audio && audio.duration) || 0, file.file_size || 0, file.mime_type || "audio/mpeg", thumbFileId, msg.date || now, now, suggested ? 1 : 0).run();
}
async function getFileUrl(fileId, env) {
  if (!fileId) return null;
  const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getFile?file_id=${fileId}`);
  const data = await res.json();
  if (!data.ok || !data.result || !data.result.file_path) return null;
  return `https://api.telegram.org/file/bot${env.BOT_TOKEN}/${data.result.file_path}`;
}
async function proxyFile(fileId, env, contentType) {
  const fileUrl = await getFileUrl(fileId, env);
  if (!fileUrl) return new Response("File unavailable", { status: 502 });
  const upstream = await fetch(fileUrl);
  if (!upstream.ok) return new Response("Upstream error", { status: 502 });
  const headers = { "Content-Type": contentType || upstream.headers.get("Content-Type") || "application/octet-stream", "Cache-Control": "public, max-age=3600", "Access-Control-Allow-Origin": "*" };
  const len = upstream.headers.get("Content-Length");
  if (len) headers["Content-Length"] = len;
  return new Response(upstream.body, { status: 200, headers });
}
async function purgeMissing(env) {
  const { results } = await env.DB.prepare(`SELECT id, file_id FROM songs WHERE COALESCE(is_active,1) = 1`).all();
  let removed = 0;
  for (const s of (results || [])) {
    const url = await getFileUrl(s.file_id, env);
    if (!url) { await env.DB.prepare(`UPDATE songs SET is_active = 0 WHERE id = ?`).bind(s.id).run(); removed++; }
  }
  return removed;
}
function checkAdmin(request, url) {
  return (request.headers.get("X-Admin-Key") || url.searchParams.get("key") || "") === "newtaraneh_admin_2026";
}
export default {
  async scheduled(event, env, ctx) { await ensureSchema(env); ctx.waitUntil(purgeMissing(env)); },
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS", "Access-Control-Allow-Headers": "Content-Type, X-Admin-Key" };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    await ensureSchema(env);
    if (path === "/webhook" && request.method === "POST") {
      try { const update = await request.json(); if (update.channel_post) await handleChannelPost(update.channel_post, env); return new Response("OK"); }
      catch (e) { return new Response("Error", { status: 500 }); }
    }
    if (path === "/admin/purge" && request.method === "POST") {
      if (!checkAdmin(request, url)) return new Response("Forbidden", { status: 403, headers: cors });
      const removed = await purgeMissing(env);
      return new Response(JSON.stringify({ removed }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
    }
    if (path.match(/^\/songs\/\d+$/) && request.method === "DELETE") {
      if (!checkAdmin(request, url)) return new Response("Forbidden", { status: 403, headers: cors });
      const id = path.split("/")[2];
      await env.DB.prepare(`UPDATE songs SET is_active = 0, updated_at = ? WHERE id = ?`).bind(Math.floor(Date.now() / 1000), id).run();
      return new Response(JSON.stringify({ ok: true, id: Number(id) }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
    }
    if (path === "/songs" && request.method === "GET") {
      const page = Math.max(1, parseInt(url.searchParams.get("page") || "1"));
      const limit = Math.min(50, Math.max(1, parseInt(url.searchParams.get("limit") || "20")));
      const offset = (page - 1) * limit;
      const q = (url.searchParams.get("q") || "").trim();
      const suggested = url.searchParams.get("suggested") === "1";
      let query = `SELECT id, message_id, title, artist, duration, file_size, thumbnail_file_id, created_at, caption, COALESCE(is_suggested,0) as is_suggested FROM songs WHERE COALESCE(is_active,1) = 1`;
      const params = [];
      if (suggested) query += ` AND COALESCE(is_suggested,0) = 1`;
      if (q) { query += ` AND (title LIKE ? OR artist LIKE ? OR caption LIKE ?)`; const like = `%${q}%`; params.push(like, like, like); }
      query += ` ORDER BY created_at DESC LIMIT ? OFFSET ?`;
      params.push(limit, offset);
      const { results } = await env.DB.prepare(query).bind(...params).all();
      const origin = url.origin;
      const songs = (results || []).map(s => ({ ...s, cover_url: s.thumbnail_file_id ? `${origin}/songs/${s.id}/cover` : null }));
      return new Response(JSON.stringify({ page, limit, songs }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
    }
    if (path.match(/^\/songs\/\d+$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT * FROM songs WHERE id = ? AND COALESCE(is_active,1)=1`).bind(id).first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      return new Response(JSON.stringify({ ...song, stream_url: `${url.origin}/songs/${id}/stream`, cover_url: song.thumbnail_file_id ? `${url.origin}/songs/${id}/cover` : null }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
    }
    if (path.match(/^\/songs\/\d+\/cover$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT thumbnail_file_id FROM songs WHERE id = ?`).bind(id).first();
      if (!song || !song.thumbnail_file_id) return new Response("No cover", { status: 404, headers: cors });
      return proxyFile(song.thumbnail_file_id, env, "image/jpeg");
    }
    if (path.match(/^\/songs\/\d+\/stream$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT file_id, mime_type FROM songs WHERE id = ? AND COALESCE(is_active,1)=1`).bind(id).first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      const resp = await proxyFile(song.file_id, env, song.mime_type || "audio/mpeg");
      if (resp.status !== 200) { await env.DB.prepare(`UPDATE songs SET is_active = 0 WHERE id = ?`).bind(id).run(); return new Response("Removed", { status: 410, headers: cors }); }
      return resp;
    }
    if (path.match(/^\/songs\/\d+\/download$/) && request.method === "GET") {
      const id = path.split("/")[2];
      const song = await env.DB.prepare(`SELECT file_id, title, mime_type FROM songs WHERE id = ? AND COALESCE(is_active,1)=1`).bind(id).first();
      if (!song) return new Response("Not found", { status: 404, headers: cors });
      const resp = await proxyFile(song.file_id, env, song.mime_type || "audio/mpeg");
      if (resp.status !== 200) { await env.DB.prepare(`UPDATE songs SET is_active = 0 WHERE id = ?`).bind(id).run(); return new Response("Removed", { status: 410, headers: cors }); }
      const headers = new Headers(resp.headers);
      const safe = String(song.title || "song").replace(/[^\w\u0600-\u06FF\- ]+/g, "_").slice(0, 80);
      headers.set("Content-Disposition", 'attachment; filename="' + safe + '.mp3"');
      return new Response(resp.body, { status: 200, headers });
    }
    if (path === "/" || path === "/health") {
      try {
        const count = await env.DB.prepare(`SELECT COUNT(*) as c FROM songs WHERE COALESCE(is_active,1)=1`).first();
        return new Response(JSON.stringify({ status: "ok", app: "NewTaraneh Music API", songs: count ? count.c : 0, proxy: true, version: "2.1" }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
      } catch (e) {
        return new Response(JSON.stringify({ status: "ok", songs: 0 }), { headers: { ...cors, "Content-Type": "application/json; charset=utf-8" } });
      }
    }
    return new Response("Not Found", { status: 404, headers: cors });
  }
};
