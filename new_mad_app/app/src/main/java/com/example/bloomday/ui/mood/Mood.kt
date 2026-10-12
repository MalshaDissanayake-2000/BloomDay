package com.example.bloomday.ui.mood

data class Mood(
    val id: Long,
    var emoji: String,
    var note: String = "",
    var timestamp: Long = System.currentTimeMillis()
)