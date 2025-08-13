package com.example.networkofone.utils
import java.text.NumberFormat
import java.util.Locale
import kotlin.text.toDoubleOrNull

object NumberFormatterUtil {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    fun format(value: String): String {
        return numberFormat.format(value.toDoubleOrNull() ?: 0.0)
    }
    fun format(value: Int): String {
        return numberFormat.format(value)
    }
    fun format(value: Double): String {
        return numberFormat.format(value)
    }
    fun format(value: Long): String {
        return numberFormat.format(value)
    }
}