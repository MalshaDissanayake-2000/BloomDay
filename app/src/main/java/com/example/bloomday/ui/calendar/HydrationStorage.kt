package com.example.bloomday.ui.calendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HydrationStorage(private val context: Context) {

    private val PREFS = "BloomDayHydration"
    private val KEY_ENTRIES = "hydration_entries_json"
    private val KEY_REMINDER_ENABLED = "hydration_reminder_enabled"
    private val KEY_REMINDER_INTERVAL = "hydration_reminder_interval_minutes"
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun todayString(): String = dateFmt.format(Date())

    fun loadEntries(): MutableList<HydrationEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<HydrationEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optLong("id", System.currentTimeMillis())
            val date = obj.optString("date", todayString())
            val amount = obj.optInt("amount", 1)
            val notes = obj.optString("notes", "")
            val ts = obj.optLong("timestamp", System.currentTimeMillis())
            list.add(HydrationEntry(id, date, amount, notes, ts))
        }
        list.sortByDescending { it.timestamp }
        return list
    }

    fun saveEntries(entries: List<HydrationEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("date", e.date)
            obj.put("amount", e.amount)
            obj.put("notes", e.notes)
            obj.put("timestamp", e.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    // Reminder prefs
    fun getReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
    fun setReminderEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()

    fun getReminderIntervalMinutes(): Int = prefs.getInt(KEY_REMINDER_INTERVAL, 60)
    fun setReminderIntervalMinutes(mins: Int) = prefs.edit().putInt(KEY_REMINDER_INTERVAL, mins).apply()
}
