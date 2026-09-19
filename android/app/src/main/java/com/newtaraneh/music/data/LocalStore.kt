package com.newtaraneh.music.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS = "newtaraneh_prefs"
const val KEY_FAVS = "favorites"
const val KEY_HISTORY = "history"
const val KEY_CACHE = "songs_cache_json"
const val KEY_CACHE_TS = "songs_cache_ts"

fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

fun loadIdSet(prefs: SharedPreferences, key: String): Set<Long> {
    val raw = prefs.getString(key, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getLong(it) }.toSet()
    }.getOrDefault(emptySet())
}

fun saveIdSet(prefs: SharedPreferences, key: String, ids: Set<Long>) {
    val arr = JSONArray(); ids.forEach { arr.put(it) }
    prefs.edit().putString(key, arr.toString()).apply()
}

fun loadIdList(prefs: SharedPreferences, key: String): List<Long> {
    val raw = prefs.getString(key, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getLong(it) }
    }.getOrDefault(emptyList())
}

fun saveIdList(prefs: SharedPreferences, key: String, ids: List<Long>) {
    val arr = JSONArray(); ids.forEach { arr.put(it) }
    prefs.edit().putString(key, arr.toString()).apply()
}

fun saveSongsCache(prefs: SharedPreferences, songs: List<SongDto>) {
    val arr = JSONArray()
    songs.forEach { s ->
        arr.put(JSONObject().apply {
            put("id", s.id)
            put("message_id", s.message_id)
            put("title", s.title)
            put("artist", s.artist)
            put("duration", s.duration ?: 0)
            put("cover_url", s.cover_url)
            put("caption", s.caption)
            put("is_suggested", s.is_suggested ?: 0)
            put("thumbnail_file_id", s.thumbnail_file_id)
            put("created_at", s.created_at ?: 0)
        })
    }
    prefs.edit().putString(KEY_CACHE, arr.toString()).putLong(KEY_CACHE_TS, System.currentTimeMillis()).apply()
}

fun loadSongsCache(prefs: SharedPreferences, maxAgeMs: Long = 30 * 60 * 1000): List<SongDto>? {
    val ts = prefs.getLong(KEY_CACHE_TS, 0)
    if (ts == 0L || System.currentTimeMillis() - ts > maxAgeMs) return null
    val raw = prefs.getString(KEY_CACHE, null) ?: return null
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SongDto(
                id = o.getLong("id"),
                message_id = o.optLong("message_id"),
                title = o.optString("title", null),
                artist = o.optString("artist", null),
                duration = o.optInt("duration"),
                cover_url = o.optString("cover_url", null).takeIf { it.isNotBlank() },
                caption = o.optString("caption", null).takeIf { it.isNotBlank() },
                is_suggested = o.optInt("is_suggested"),
                thumbnail_file_id = o.optString("thumbnail_file_id", null).takeIf { it.isNotBlank() },
                created_at = o.optLong("created_at")
            )
        }
    }.getOrNull()
}
