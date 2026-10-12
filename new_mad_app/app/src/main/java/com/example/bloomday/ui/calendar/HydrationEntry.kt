package com.example.bloomday.ui.calendar

data class HydrationEntry(
    val id: Long,
    val date: String,
    val amount: Int,
    val notes: String,
    val timestamp: Long
)