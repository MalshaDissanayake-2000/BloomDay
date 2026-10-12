package com.example.bloomday.ui.mood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R

class MoodAdapter(
    private val items: List<Mood>,
    private val storage: MoodStorage,
    private val listener: MoodListener
) : RecyclerView.Adapter<MoodAdapter.MoodVH>() {

    interface MoodListener {
        fun onShare(mood: Mood)
        fun onDelete(mood: Mood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
        return MoodVH(v)
    }

    override fun onBindViewHolder(holder: MoodVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MoodVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvItemEmoji)
        private val tvNote: TextView = itemView.findViewById(R.id.tvItemNote)
        private val tvTime: TextView = itemView.findViewById(R.id.tvItemTime)

        // changed to TextView so we can show emoji characters (📤, 🗑️)
        private val btnShareEmoji: TextView = itemView.findViewById(R.id.btnShareMood)
        private val btnDeleteEmoji: TextView = itemView.findViewById(R.id.btnDeleteMood)

        fun bind(mood: Mood) {
            tvEmoji.text = mood.emoji
            tvNote.text = mood.note.ifBlank { "(no note)" }
            tvTime.text = storage.timestampToString(mood.timestamp)

            // Ensure the emoji buttons show proper emoji if layout doesn't already contain them
            // (This is harmless if your layout already has emoji text.)
            if (btnShareEmoji.text.isNullOrBlank()) {
                btnShareEmoji.text = "📤"
            }
            if (btnDeleteEmoji.text.isNullOrBlank()) {
                btnDeleteEmoji.text = "🗑️"
            }

            // Accessibility content descriptions
            btnShareEmoji.contentDescription = "Share mood"
            btnDeleteEmoji.contentDescription = "Delete mood"

            // Click actions forwarded to listener
            btnShareEmoji.setOnClickListener { listener.onShare(mood) }
            btnDeleteEmoji.setOnClickListener { listener.onDelete(mood) }

            // Optional: make sure emoji TextViews are focusable/clickable for accessibility
            btnShareEmoji.isClickable = true
            btnShareEmoji.isFocusable = true
            btnDeleteEmoji.isClickable = true
            btnDeleteEmoji.isFocusable = true
        }
    }
}
