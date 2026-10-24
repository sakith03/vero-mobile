package com.example.veromobile.domain.calc

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyCalculator {
    const val LKR_RATE_MICRO = 1_000_000L
    private val MICRO = BigDecimal(1_000_000)

    /** Both inputs are minor units, so the ratio is directly LKR per 1 unit of foreign currency. */
    fun deriveRateMicro(originalMinor: Long, lkrMinor: Long): Long? {
        if (originalMinor <= 0 || lkrMinor <= 0) return null
        return BigDecimal(lkrMinor).multiply(MICRO)
            .divide(BigDecimal(originalMinor), 0, RoundingMode.HALF_UP).toLong()
    }

    fun toLkrMinor(originalMinor: Long, rateMicro: Long): Long =
        BigDecimal(originalMinor).multiply(BigDecimal(rateMicro))
            .divide(MICRO, 0, RoundingMode.HALF_UP).toLong()

    fun formatRate(rateMicro: Long): String =
        BigDecimal(rateMicro).movePointLeft(6).setScale(2, RoundingMode.HALF_UP).toPlainString()

    fun formatMoney(minor: Long): String =
        DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US)).format(BigDecimal(minor).movePointLeft(2))

    /** Minor units -> plain text for an input field, e.g. 3588000 -> "35880". */
    fun toPlainText(minor: Long): String =
        BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
}