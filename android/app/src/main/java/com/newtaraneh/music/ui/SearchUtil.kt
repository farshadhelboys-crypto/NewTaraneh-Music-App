package com.newtaraneh.music.ui

object SearchUtil {
    private val map = mapOf(
        'ا' to "a", 'آ' to "a", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ث' to "s",
        'ج' to "j", 'چ' to "ch", 'ح' to "h", 'خ' to "kh", 'د' to "d", 'ذ' to "z",
        'ر' to "r", 'ز' to "z", 'ژ' to "zh", 'س' to "s", 'ش' to "sh", 'ص' to "s",
        'ض' to "z", 'ط' to "t", 'ظ' to "z", 'ع' to "e", 'غ' to "gh", 'ف' to "f",
        'ق' to "gh", 'ک' to "k", 'ك' to "k", 'گ' to "g", 'ل' to "l", 'م' to "m",
        'ن' to "n", 'و' to "o", 'ه' to "h", 'ی' to "i", 'ي' to "i", 'ئ' to "i",
        'ء' to "", 'ة' to "h", 'ؤ' to "o"
    )

    fun toLatin(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(map[ch] ?: ch)
        }
        return sb.toString().lowercase().replace(Regex("\\s+"), " ").trim()
    }

    fun searchQueries(input: String): List<String> {
        val q = input.trim()
        if (q.isEmpty()) return emptyList()
        val latin = toLatin(q)
        return listOf(q, latin).distinct().filter { it.isNotBlank() }
    }
}
