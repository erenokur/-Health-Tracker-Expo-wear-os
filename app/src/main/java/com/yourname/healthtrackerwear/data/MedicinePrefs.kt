package com.yourname.healthtrackerwear.data

import android.content.Context
import org.json.JSONArray

/**
 * Stores and retrieves the medication list that the paired phone pushes to
 * the watch via the Wearable Data Layer. The list is persisted across
 * launches so the last known list is always available even when the phone
 * is not currently connected.
 */
object MedicinePrefs {
    private const val PREFS_NAME = "health_tracker_wear_prefs"
    private const val KEY_MED_LIST = "medicine_list"

    /** Returns the cached medication name list, or an empty list if never synced. */
    fun getList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MED_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Overwrites the cached medication list with the new names. */
    fun saveList(context: Context, names: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_MED_LIST, JSONArray(names).toString())
            .apply()
    }
}

