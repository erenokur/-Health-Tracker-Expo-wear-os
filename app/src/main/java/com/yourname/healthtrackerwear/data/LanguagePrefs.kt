package com.yourname.healthtrackerwear.data

import android.content.Context

object LanguagePrefs {
    private const val PREFS_NAME = "health_tracker_wear_prefs"
    private const val KEY_LANG = "language"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "tr") ?: "tr"
    }

    fun set(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang).apply()
    }
}
