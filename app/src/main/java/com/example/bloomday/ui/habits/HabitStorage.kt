package com.example.bloomday.ui.habits

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HabitsStorage(private val context: Context) {

    private val PREFS = "BloomDayHabits"
    private val KEY_HABITS = "habits_json"
    private val KEY_COMPLETIONS = "habit_completions"
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = sdf.format(Date())

    // -------- Habits list ----------
    fun loadHabits(): MutableList<Habit> {
        val json = prefs.getString(KEY_HABITS, null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Habit(o.getLong("id"), o.getString("title"), o.optString("notes", "")))
        }
        return list
    }

    fun saveHabits(list: List<Habit>) {
        val arr = JSONArray()
        for (h in list) {
            val o = JSONObject()
            o.put("id", h.id)
            o.put("title", h.title)
            o.put("notes", h.notes)
            arr.put(o)
        }
        prefs.edit().putString(KEY_HABITS, arr.toString()).apply()
    }

    // -------- Completion tracking by date ----------
    private fun loadCompletionsObj(): JSONObject {
        val j = prefs.getString(KEY_COMPLETIONS, null) ?: return JSONObject()
        return JSONObject(j)
    }

    private fun saveCompletionsObj(obj: JSONObject) {
        prefs.edit().putString(KEY_COMPLETIONS, obj.toString()).apply()
    }

    fun getCompletedIdsForDate(date: String = today()): MutableSet<Long> {
        val obj = loadCompletionsObj()
        if (!obj.has(date)) return mutableSetOf()
        val arr = obj.getJSONArray(date)
        val set = mutableSetOf<Long>()
        for (i in 0 until arr.length()) set.add(arr.getLong(i))
        return set
    }

    fun setCompletedIdsForDate(date: String, ids: Collection<Long>) {
        val obj = loadCompletionsObj()
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        obj.put(date, arr)
        saveCompletionsObj(obj)
    }

    fun markCompleted(habitId: Long, date: String = today()) {
        val set = getCompletedIdsForDate(date)
        set.add(habitId)
        setCompletedIdsForDate(date, set)
    }

    fun markUncompleted(habitId: Long, date: String = today()) {
        val set = getCompletedIdsForDate(date)
        set.remove(habitId)
        setCompletedIdsForDate(date, set)
    }

    fun isCompleted(habitId: Long, date: String = today()): Boolean =
        getCompletedIdsForDate(date).contains(habitId)

    fun clearAllForDate(date: String) {
        val obj = loadCompletionsObj()
        if (obj.has(date)) obj.remove(date)
        saveCompletionsObj(obj)
    }
}
