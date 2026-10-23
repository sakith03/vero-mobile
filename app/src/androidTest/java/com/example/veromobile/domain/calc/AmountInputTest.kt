package com.example.veromobile.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputTest {
    @Test fun wholeAmountBecomesMinorUnits() =
        assertEquals(45000L, AmountInput.toMinorUnits("450"))

    @Test fun decimalSumHasNoFloatingPointError() =
        assertEquals(30L, AmountInput.toMinorUnits("0.1") + AmountInput.toMinorUnits("0.2"))

    @Test fun onlyOneDotAndMaxTwoDecimals() {
        var t = ""
        "1.2.345".forEach { t = AmountInput.append(t, it) }
        assertEquals("1.23", t)
    }

    @Test fun leadingDotBecomesZeroDot() =
        assertEquals("0.", AmountInput.append("", '.'))
}
