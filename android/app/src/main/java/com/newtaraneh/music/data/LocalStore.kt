package com.newtaraneh.music.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

private const val PREFS = "newtaraneh_prefs"
const val KEY_FAVS = "favorites"
const val KEY_HISTORY = "history"

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
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
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
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    prefs.edit().putString(key, arr.toString()).apply()
}
