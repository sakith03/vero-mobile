package com.example.veromobile.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputTest {
    @Test fun `450 becomes 45000 minor units`() =
        assertEquals(45000L, AmountInput.toMinorUnits("450"))

    @Test fun `decimal sum has no floating point error`() =
        assertEquals(30L, AmountInput.toMinorUnits("0.1") + AmountInput.toMinorUnits("0.2"))

    @Test fun `only one dot and max two decimals`() {
        var t = ""
        "1.2.345".forEach { t = AmountInput.append(t, it) }
        assertEquals("1.23", t)
    }

    @Test fun `leading dot becomes 0 dot`() =
        assertEquals("0.", AmountInput.append("", '.'))
}