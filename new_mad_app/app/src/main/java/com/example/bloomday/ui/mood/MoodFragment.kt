package com.example.bloomday.ui.mood

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R
import com.example.bloomday.ui.mood.Mood
import com.example.bloomday.ui.mood.MoodStorage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MoodFragment : Fragment(), MoodAdapter.MoodListener {

    private lateinit var storage: MoodStorage
    private lateinit var adapter: MoodAdapter
    private lateinit var rv: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmpty: TextView
    private var moods: MutableList<Mood> = mutableListOf()

    // default emoji set (you can expand)
    private val emojiList = listOf("😁", "😊", "🙂", "😐", "😔", "😢", "😠", "😴", "🤩", "😇")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mood, container, false)
        storage = MoodStorage(requireContext())

        rv = view.findViewById(R.id.rvMoods)
        fabAdd = view.findViewById(R.id.fabAddMood)
        tvEmpty = view.findViewById(R.id.tvEmptyMoods)

        rv.layoutManager = LinearLayoutManager(requireContext())
        moods = storage.loadMoods()
        adapter = MoodAdapter(moods, storage, this)
        rv.adapter = adapter

        updateEmpty()

        fabAdd.setOnClickListener { showAddMoodDialog() }

        return view
    }

    private fun updateEmpty() {
        tvEmpty.isVisible = moods.isEmpty()
    }

    private fun showAddMoodDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("How are you feeling?")

        val container = layoutInflater.inflate(R.layout.dialog_add_mood, null)

        // grid of emoji buttons dynamically filled
        val grid = container.findViewById<GridLayout>(R.id.gridEmojis)
        val etNote = container.findViewById<EditText>(R.id.etMoodNote)
        val tvSel = container.findViewById<TextView>(R.id.tvSelectedEmoji)
        var selectedEmoji: String? = null

        // make sure grid is empty and use configured column count
        grid.removeAllViews()
        val cols = grid.columnCount.takeIf { it > 0 } ?: 5
        grid.columnCount = cols

        // inflate a small TextView (view_emoji_button.xml) for each emoji and set its text
        emojiList.forEach { emoji ->
            val emojiView = layoutInflater.inflate(R.layout.view_emoji_button, grid, false) as TextView
            emojiView.text = emoji
            emojiView.textSize = 20f
            emojiView.isClickable = true
            emojiView.isFocusable = true

            // basic selection visual: change background when selected
            emojiView.setOnClickListener {
                selectedEmoji = emoji
                // show selected emoji in the dialog
                tvSel.text = "Selected: $emoji"

                // update selection visuals: reset all children then highlight tapped
                for (i in 0 until grid.childCount) {
                    val child = grid.getChildAt(i)
                    child.background = null
                }
                // simple selectable background (uses system ripple if available)
                emojiView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }

            // add to grid
            grid.addView(emojiView)
        }

        // Pre-select first emoji visually (optional)
        if (emojiList.isNotEmpty()) {
            selectedEmoji = emojiList.first()
            tvSel.text = "Selected: ${emojiList.first()}"
            // highlight first child after it's been added
            if (grid.childCount > 0) {
                val firstChild = grid.getChildAt(0)
                firstChild.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }
        }

        builder.setView(container)
        builder.setPositiveButton("Save") { dialog, _ ->
            val note = etNote.text.toString().trim()
            val emoji = selectedEmoji ?: emojiList.firstOrNull() ?: "🙂"
            val mood = Mood(
                id = generateId(),
                emoji = emoji,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            moods.add(0, mood) // add newest at top
            storage.saveMoods(moods)
            adapter.notifyDataSetChanged()
            updateEmpty()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }


    override fun onShare(mood: Mood) {
        val text = "My mood: ${mood.emoji}\n${mood.note}\nAt ${storage.timestampToString(mood.timestamp)}"
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        share.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(share, "Share mood via"))
    }

    override fun onDelete(mood: Mood) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete mood")
            .setMessage("Delete this mood entry?")
            .setPositiveButton("Delete") { _, _ ->
                moods.remove(mood)
                storage.saveMoods(moods)
                adapter.notifyDataSetChanged()
                updateEmpty()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateId(): Long = (System.currentTimeMillis() + Random().nextInt(1000)).absoluteValue
    private val Long.absoluteValue: Long get() = if (this < 0) -this else this
}
