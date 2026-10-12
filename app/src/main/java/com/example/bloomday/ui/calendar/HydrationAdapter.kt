package com.example.bloomday.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomday.R

class HydrationAdapter(
    private var items: MutableList<HydrationEntry>,
    private val listener: HydrationListener
) : RecyclerView.Adapter<HydrationAdapter.VH>() {

    interface HydrationListener {
        fun onDelete(entry: HydrationEntry)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvNotes: TextView = view.findViewById(R.id.tvNotes)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteHydration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_hydration, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvAmount.text = "${e.amount} 🥤"
        holder.tvDate.text = e.date
        holder.tvNotes.text = if (e.notes.isBlank()) "(no note)" else e.notes
        holder.btnDelete.setOnClickListener { listener.onDelete(e) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: MutableList<HydrationEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
