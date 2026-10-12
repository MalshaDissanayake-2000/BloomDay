package com.example.bloomday.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bloomday.R
import com.example.bloomday.ui.calendar.HydrationEntry
import com.example.bloomday.ui.calendar.HydrationStorage
import com.example.bloomday.ui.habits.HabitsStorage
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var habitsStorage: HabitsStorage
    private lateinit var hydrationStorage: HydrationStorage

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvHabitsSummary: TextView
    private lateinit var pbHabits: ProgressBar
    private lateinit var tvHydrationSummary: TextView
    private lateinit var pbHydration: ProgressBar
    private lateinit var btnOpenHabits: Button
    private lateinit var btnAddHydrationQuick: Button
    private lateinit var btnOpenCalendar: Button

    private lateinit var llCompletedHabitsList: LinearLayout

    private val dailyHydrationTarget = 8

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        habitsStorage = HabitsStorage(requireContext())
        hydrationStorage = HydrationStorage(requireContext())

        tvGreeting = root.findViewById(R.id.tvGreeting)
        tvDate = root.findViewById(R.id.tvDate)
        tvHabitsSummary = root.findViewById(R.id.tvHabitsSummary)
        pbHabits = root.findViewById(R.id.pbHabits)
        tvHydrationSummary = root.findViewById(R.id.tvHydrationSummary)
        pbHydration = root.findViewById(R.id.pbHydration)
        btnOpenHabits = root.findViewById(R.id.btnOpenHabits)
        btnAddHydrationQuick = root.findViewById(R.id.btnAddHydrationQuick)
        btnOpenCalendar = root.findViewById(R.id.btnOpenCalendar)
        llCompletedHabitsList = root.findViewById(R.id.llCompletedHabitsList)

        tvGreeting.text = greetingText()
        tvDate.text = dateString()

        btnOpenHabits.setOnClickListener { navigateToHabits() }
        btnOpenCalendar.setOnClickListener { navigateToCalendar() }
        btnAddHydrationQuick.setOnClickListener { addQuickHydration() }

        root.findViewById<Button>(R.id.btnShortcutHabits).setOnClickListener { navigateToHabits() }
        root.findViewById<Button>(R.id.btnShortcutCalendar).setOnClickListener { navigateToCalendar() }
        root.findViewById<Button>(R.id.btnShortcutHydration).setOnClickListener { addQuickHydration() }

        refreshAll()

        return root
    }

    private fun greetingText(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning 👋"
            in 12..16 -> "Good afternoon 👋"
            in 17..21 -> "Good evening 👋"
            else -> "Hello 👋"
        }
    }

    private fun dateString(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun refreshAll() {
        refreshHabitsCard()
        refreshHydrationCard()
        refreshCompletedHabits()
    }

    private fun refreshHabitsCard() {
        val habits = habitsStorage.loadHabits()
        if (habits.isEmpty()) {
            tvHabitsSummary.text = "No habits yet"
            pbHabits.progress = 0
            return
        }
        val total = habits.size
        val completedIds = habitsStorage.getCompletedIdsForDate(habitsStorage.today())
        val done = completedIds.count { id -> habits.any { it.id == id } }
        val percent = (done * 100 / total)
        tvHabitsSummary.text = "$done / $total complete"
        pbHabits.progress = percent
    }

    private fun refreshHydrationCard() {
        val entries = hydrationStorage.loadEntries()
        val today = hydrationStorage.todayString()
        val todayTotal = entries.filter { it.date == today }.sumOf { it.amount }
        val percent = ((todayTotal.toDouble() / dailyHydrationTarget) * 100).coerceAtMost(100.0).toInt()
        tvHydrationSummary.text = "$todayTotal / $dailyHydrationTarget glasses"
        pbHydration.progress = percent
    }

    private fun refreshCompletedHabits() {
        llCompletedHabitsList.removeAllViews()

        val habits = habitsStorage.loadHabits()
        val completedIds = habitsStorage.getCompletedIdsForDate(habitsStorage.today())
        val completed = habits.filter { completedIds.contains(it.id) }

        if (completed.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "No habits completed yet"
            tv.setTextColor(resources.getColor(R.color.black))
            tv.textSize = 14f
            llCompletedHabitsList.addView(tv)
            return
        }

        completed.forEach { habit ->
            val tv = TextView(requireContext())
            tv.text = "• ${habit.title}"
            tv.setTextColor(resources.getColor(R.color.black))
            tv.textSize = 16f
            tv.setPadding(8, 4, 0, 4)
            llCompletedHabitsList.addView(tv)
        }
    }

    private fun addQuickHydration() {
        val entry = HydrationEntry(
            id = System.currentTimeMillis(),
            date = hydrationStorage.todayString(),
            amount = 1,
            notes = "Quick add",
            timestamp = System.currentTimeMillis()
        )
        val entries = hydrationStorage.loadEntries()
        entries.add(0, entry)
        hydrationStorage.saveEntries(entries)

        refreshHydrationCard()
        Toast.makeText(requireContext(), "Added 1 glass", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHabits() = findNavController().navigate(R.id.habitsFragment)
    private fun navigateToCalendar() = findNavController().navigate(R.id.calendarFragment)

    override fun onResume() {
        super.onResume()
        refreshAll()
    }
}
