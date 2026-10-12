package com.example.bloomday.ui.calendar

import android.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R
import com.example.bloomday.notifications.HydrationReceiver
import java.util.*

class CalendarFragment : Fragment(), HydrationAdapter.HydrationListener {

    private lateinit var storage: HydrationStorage
    private lateinit var adapter: HydrationAdapter
    private lateinit var rv: RecyclerView
    private lateinit var btnAdd: Button
    private lateinit var switchReminder: Switch
    private lateinit var etInterval: EditText
    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar

    private var allEntries = mutableListOf<HydrationEntry>()
    private var shownEntries = mutableListOf<HydrationEntry>()
    private var selectedDateStr: String = ""

    private val dailyTarget = 8
    private val REQUEST_CODE_PI = 1111
    private val TEST_MODE = false // set true for quick testing (seconds instead of minutes)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        storage = HydrationStorage(requireContext())

        rv = view.findViewById(R.id.rvHydration)
        btnAdd = view.findViewById(R.id.btnAddHydration)
        switchReminder = view.findViewById(R.id.switchReminder)
        etInterval = view.findViewById(R.id.etReminderInterval)
        calendarView = view.findViewById(R.id.calendarView)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        progressBar = view.findViewById(R.id.progressHydration)

        rv.layoutManager = LinearLayoutManager(requireContext())
        allEntries = storage.loadEntries()
        shownEntries.clear()
        adapter = HydrationAdapter(shownEntries, this)
        rv.adapter = adapter

        selectedDateStr = storage.todayString()
        tvSelectedDate.text = selectedDateStr
        calendarView.date = java.util.Calendar.getInstance().timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val m = month + 1
            selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, m, dayOfMonth)
            tvSelectedDate.text = selectedDateStr
            refreshShownEntries()
        }

        // setup reminder UI
        setupReminderUi()

        btnAdd.setOnClickListener { showAddDialog() }

        // initial refresh
        refreshShownEntries()

        HydrationReceiver.createChannelIfNeeded(requireContext())
        return view
    }

    private fun setupReminderUi() {
        etInterval.setText(storage.getReminderIntervalMinutes().toString())
        switchReminder.isChecked = storage.getReminderEnabled()

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val ok = scheduleReminderWithValidation()
                if (!ok) {
                    switchReminder.isChecked = false
                } else {
                    storage.setReminderEnabled(true)
                }
            } else {
                cancelReminder()
                storage.setReminderEnabled(false)
            }
        }
    }

    private fun refreshShownEntries() {
        allEntries = storage.loadEntries()
        val list = allEntries.filter { it.date == selectedDateStr }.sortedByDescending { it.timestamp }
        shownEntries.clear()
        shownEntries.addAll(list)
        adapter.setItems(shownEntries)
        updateProgress()
    }

    private fun updateProgress() {
        val total = shownEntries.sumOf { it.amount }
        val percent = ((total.toDouble() / dailyTarget) * 100).coerceAtMost(100.0).toInt()
        progressBar.progress = percent
        tvProgressPercent.text = "$total / $dailyTarget glasses • $percent%"
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Hydration Entry")
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_hydration, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        etAmount.setText("1")
        builder.setView(dialogView)
        builder.setPositiveButton("Add") { dialog, _ ->
            val amount = etAmount.text.toString().toIntOrNull() ?: 1
            val note = etNote.text.toString().trim()
            val entry = HydrationEntry(System.currentTimeMillis(), selectedDateStr, amount, note, System.currentTimeMillis())
            allEntries.add(0, entry)
            storage.saveEntries(allEntries)
            refreshShownEntries()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDelete(entry: HydrationEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Remove this hydration entry?")
            .setPositiveButton("Delete") { _, _ ->
                allEntries.removeAll { it.id == entry.id }
                storage.saveEntries(allEntries)
                refreshShownEntries()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleReminderWithValidation(): Boolean {
        val raw = etInterval.text.toString().trim()
        val mins = raw.toIntOrNull()
        if (mins == null || mins <= 0) {
            Toast.makeText(requireContext(), "Enter a positive number of minutes", Toast.LENGTH_SHORT).show()
            return false
        }
        storage.setReminderIntervalMinutes(mins)

        val intervalMillis = if (TEST_MODE) {
            // test mode: interpret value as seconds for quick testing
            (mins * 1000L)
        } else {
            (mins * 60_000L).coerceAtLeast(15 * 60_000L)
        }

        val intent = Intent(requireContext(), HydrationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(requireContext(), REQUEST_CODE_PI, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.cancel(pi)
            if (TEST_MODE) {
                am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, pi)
            } else {
                val triggerAt = System.currentTimeMillis() + intervalMillis
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMillis, pi)
            }
            HydrationReceiver.createChannelIfNeeded(requireContext())
            Toast.makeText(requireContext(), "Hydration reminders scheduled every $mins ${if (TEST_MODE) "seconds" else "minutes"}", Toast.LENGTH_SHORT).show()
            return true
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), "Failed to schedule reminders: ${ex.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun cancelReminder() {
        val intent = Intent(requireContext(), HydrationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(requireContext(), REQUEST_CODE_PI, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
        storage.setReminderEnabled(false)
        Toast.makeText(requireContext(), "Hydration reminders canceled", Toast.LENGTH_SHORT).show()
    }
}
