package com.example.bloomday.ui.mood

import android.content.Context
import com.example.bloomday.ui.mood.Mood
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MoodStorage(private val context: Context) {

    private val prefsName = "BloomDayPrefs"
    private val key = "moods_json"
    private val prefs by lazy { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun loadMoods(): MutableList<Mood> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<Mood>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getLong("id")
            val emoji = obj.optString("emoji", "🙂")
            val note = obj.optString("note", "")
            val ts = obj.optLong("timestamp", System.currentTimeMillis())
            list.add(Mood(id, emoji, note, ts))
        }
        // sort newest first
        list.sortByDescending { it.timestamp }
        return list
    }

    fun saveMoods(moods: List<Mood>) {
        val arr = JSONArray()
        for (m in moods) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("emoji", m.emoji)
            obj.put("note", m.note)
            obj.put("timestamp", m.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun timestampToString(ts: Long): String = dateTimeFormat.format(Date(ts))
}
