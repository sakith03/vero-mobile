package com.example.veromobile.domain.calc

import java.math.BigDecimal
import java.math.RoundingMode

object AmountInput {
    private const val MAX_INT_DIGITS = 9

    fun append(current: String, key: Char): String = when {
        key == '.' -> when {
            '.' in current -> current
            current.isEmpty() -> "0."
            else -> "$current."
        }
        !key.isDigit() -> current
        current == "0" -> key.toString()
        else -> {
            val dot = current.indexOf('.')
            when {
                dot >= 0 && current.length - dot > 2 -> current   // max 2 decimals
                dot < 0 && current.length >= MAX_INT_DIGITS -> current
                else -> current + key
            }
        }
    }

    fun backspace(current: String): String = current.dropLast(1)

    fun toMinorUnits(text: String): Long {
        val clean = text.trimEnd('.').ifEmpty { "0" }
        return BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}