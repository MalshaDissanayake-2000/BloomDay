package com.example.bloomday.ui.habits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R
import android.widget.ImageButton

class HabitsAdapter(
    private var items: MutableList<Habit>,
    private val listener: Listener,
    private val storage: HabitsStorage
) : RecyclerView.Adapter<HabitsAdapter.VH>() {

    interface Listener {
        fun onEdit(habit: Habit)
        fun onDelete(habit: Habit)
        fun onToggleCompletion(habit: Habit, completed: Boolean)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val toggle: ToggleButton = view.findViewById(R.id.toggleDone)
        val tvTitle: TextView = view.findViewById(R.id.tvHabitTitle)
        val tvNotes: TextView = view.findViewById(R.id.tvHabitNotes)
        val imgDot: View = view.findViewById(R.id.imgDot)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteMood) // changed from ImageButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]
        holder.tvTitle.text = h.title
        holder.tvNotes.text = h.notes
        val completed = storage.isCompleted(h.id)
        holder.toggle.isChecked = completed
        holder.imgDot.visibility = if (completed) View.VISIBLE else View.GONE

        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.setOnClickListener {
            val nowChecked = holder.toggle.isChecked
            if (nowChecked) {
                storage.markCompleted(h.id)
            } else {
                storage.markUncompleted(h.id)
            }
            holder.imgDot.visibility = if (nowChecked) View.VISIBLE else View.GONE
            listener.onToggleCompletion(h, nowChecked)
        }

        holder.btnEdit.setOnClickListener { listener.onEdit(h) }
        holder.btnDelete.setOnClickListener { listener.onDelete(h) } // works with TextView now
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: MutableList<Habit>) {
        items = newItems
        notifyDataSetChanged()
    }
}
