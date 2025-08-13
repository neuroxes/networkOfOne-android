package com.example.networkofone.utils

import com.example.networkofone.mvvm.models.GameData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GameSorter {
    private val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US)

    fun sortGamesByDateTime(games: List<GameData>): List<GameData> {
        return games.sortedWith(compareBy {
            try {
                dateTimeFormat.parse("${it.date} ${it.time}") ?: Date(0)
            } catch (e: Exception) {
                Date(0) // Default to epoch if parsing fails
            }
        })
    }
}