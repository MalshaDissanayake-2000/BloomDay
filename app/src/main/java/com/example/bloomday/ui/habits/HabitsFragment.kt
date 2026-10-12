package com.example.bloomday.ui.habits

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class HabitsFragment : Fragment(), HabitsAdapter.Listener {

    private lateinit var storage: HabitsStorage
    private lateinit var adapter: HabitsAdapter
    private lateinit var rv: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardEmpty: View
    private lateinit var btnCreateFirst: Button
    private lateinit var etSearch: EditText
    private lateinit var chipAll: Chip
    private lateinit var chipToday: Chip
    private lateinit var chipMissed: Chip

    private var allHabits = mutableListOf<Habit>()
    private var shownHabits = mutableListOf<Habit>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)

        storage = HabitsStorage(requireContext())

        // find views (IDs must match your layout)
        rv = view.findViewById(R.id.rvHabits)
        fab = view.findViewById(R.id.fabAddHabit)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        progressBar = view.findViewById(R.id.habitsProgressBar)
        cardEmpty = view.findViewById(R.id.cardEmpty)
        btnCreateFirst = view.findViewById(R.id.btnCreateFirst)
        etSearch = view.findViewById(R.id.etSearch)
        chipAll = view.findViewById(R.id.chipAll)
        chipToday = view.findViewById(R.id.chipToday)
        chipMissed = view.findViewById(R.id.chipMissed)

        // Recycler
        rv.layoutManager = LinearLayoutManager(requireContext())
        allHabits = storage.loadHabits()
        shownHabits = allHabits.toMutableList()
        adapter = HabitsAdapter(shownHabits, this, storage)
        rv.adapter = adapter

        // UI actions
        fab.setOnClickListener { showAddEditDialog(null) }
        btnCreateFirst.setOnClickListener { showAddEditDialog(null) }

        // search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // chips
        chipAll.setOnClickListener { selectChipAndFilter(chipAll) }
        chipToday.setOnClickListener { selectChipAndFilter(chipToday) }
        chipMissed.setOnClickListener { selectChipAndFilter(chipMissed) }

        // initial UI
        chipAll.isChecked = true
        updateUi()

        return view
    }

    private fun selectChipAndFilter(selected: Chip) {
        // mark only this one checked (Choice behavior)
        chipAll.isChecked = selected == chipAll
        chipToday.isChecked = selected == chipToday
        chipMissed.isChecked = selected == chipMissed
        applyFilters()
    }

    private fun applyFilters() {
        // reload master list from storage so we always show latest
        allHabits = storage.loadHabits()
        val query = etSearch.text.toString().lowercase(Locale.getDefault()).trim()

        // start from all
        var filtered = allHabits.asSequence()

        // apply search
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.title.lowercase(Locale.getDefault()).contains(query) || it.notes.lowercase(Locale.getDefault()).contains(query) }
        }

        // apply chip filter
        when {
            chipToday.isChecked -> {
                val today = storage.today()
                // show habits that are not completed? or show only those that appear for today - we will include all but mark
                filtered = filtered.filter { /* optionally apply today's criteria; for simplicity show all */ true }
            }
            chipMissed.isChecked -> {
                // naive "missed" filter: show habits that exist but have no completion for today
                val todaySet = storage.getCompletedIdsForDate(storage.today())
                filtered = filtered.filter { habit -> !todaySet.contains(habit.id) }
            }
            else -> { /* chipAll -> nothing extra */ }
        }

        shownHabits.clear()
        shownHabits.addAll(filtered.sortedBy { it.title }.toList())
        adapter.setItems(shownHabits)
        updateUi()
    }

    private fun updateUi() {
        // progress calculation based on stored completions for today
        val total = allHabits.size
         val completedSet = storage.getCompletedIdsForDate(storage.today())
        val done = completedSet.count { id -> allHabits.any { it.id == id } } // only count existing habits
        val percent = if (total == 0) 0 else (done * 100 / total)

        progressBar.progress = percent
        tvProgressPercent.text = if (total == 0) "No habits yet" else "$percent% completed today ($done / $total)"

        // empty state toggles
        cardEmpty.isVisible = allHabits.isEmpty()
        rv.isVisible = allHabits.isNotEmpty()
    }

    private fun showAddEditDialog(editHabit: Habit?) {
        val isEdit = editHabit != null
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEdit) "Edit Habit" else "Add Habit")
        val v = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val etTitle = v.findViewById<EditText>(R.id.etHabitTitle)
        val etNotes = v.findViewById<EditText>(R.id.etHabitNotes)
        if (isEdit) {
            etTitle.setText(editHabit!!.title)
            etNotes.setText(editHabit.notes)
        }
        builder.setView(v)
        builder.setPositiveButton(if (isEdit) "Save" else "Add") { dialog, _ ->
            val title = etTitle.text.toString().trim()
            val notes = etNotes.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "Enter a name"
                return@setPositiveButton
            }
            if (isEdit) {
                editHabit!!.title = title
                editHabit.notes = notes
                storage.saveHabits(allHabits)
            } else {
                val h = Habit(System.currentTimeMillis(), title, notes)
                allHabits.add(0, h)
                storage.saveHabits(allHabits)
            }
            applyFilters()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // Listener callbacks from adapter
    override fun onEdit(habit: Habit) {
        showAddEditDialog(habit)
    }

    override fun onDelete(habit: Habit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete habit")
            .setMessage("Delete \"${habit.title}\" ?")
            .setPositiveButton("Delete") { _, _ ->
                allHabits.removeAll { it.id == habit.id }

                // remove completions for removed habit across dates
                try {
                    val obj = storage.run {
                        val privateMethod = this::class.java.getDeclaredMethod("loadCompletionsObj")
                        privateMethod.isAccessible = true
                        privateMethod.invoke(this) as org.json.JSONObject
                    }
                    val keys = obj.keys().asSequence().toList()
                    for (key in keys) {
                        val arr = obj.getJSONArray(key)
                        val newArr = org.json.JSONArray()
                        for (i in 0 until arr.length()) {
                            val id = arr.getLong(i)
                            if (id != habit.id) newArr.put(id)
                        }
                        obj.put(key, newArr)
                    }
                    storage.run {
                        val privateSave = this::class.java.getDeclaredMethod("saveCompletionsObj", org.json.JSONObject::class.java)
                        privateSave.isAccessible = true
                        privateSave.invoke(this, obj)
                    }
                } catch (e: Exception) {
                    // ignore reflection issues — not critical
                }

                storage.saveHabits(allHabits)
                applyFilters()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onToggleCompletion(habit: Habit, completed: Boolean) {
        // the adapter should already update storage when user toggles; just refresh progress
        updateUi()
    }
}
