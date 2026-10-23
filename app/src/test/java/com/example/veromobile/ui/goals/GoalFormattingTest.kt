package com.example.veromobile.ui.goals

import org.junit.Assert.*
import org.junit.Test

class GoalFormattingTest {
    @Test fun `decimal input is converted exactly`() {
        assertEquals(49_000_000L, parseGoalAmount("490000"))
        assertEquals(30L, parseGoalAmount("0.30"))
        assertEquals(1_120_000L, parseGoalAmount("11,200"))
        assertEquals(123_456L, parseGoalAmount(" 1,234.56 "))
    }

    @Test fun `invalid amounts and fractional minor units are rejected`() {
        listOf("", "-1", "abc", "1.001", "1,00", "1e3", "NaN", "Infinity").forEach { assertNull(parseGoalAmount(it)) }
    }

    @Test fun `maximum balance round trips and overflow is rejected`() {
        assertEquals(Long.MAX_VALUE, parseGoalAmount("92233720368547758.07"))
        assertNull(parseGoalAmount("92233720368547758.08"))
        assertNull(parseGoalAmount("9".repeat(1000)))
        assertEquals(Long.MAX_VALUE, parseGoalAmount(goalAmountInput(Long.MAX_VALUE)))
    }

    @Test fun `money display preserves sub rupee amounts`() {
        assertEquals("LKR 0.01", goalMoney(1))
        assertEquals("LKR 0.10", goalMoney(10))
        assertEquals("LKR 25.50", goalMoney(2550))
        assertEquals("LKR 0", goalMoney(0))
        assertEquals("LKR 11,200", goalMoney(1_120_000))
        assertEquals("LKR 1,234.56", goalMoney(123_456))
        assertEquals("LKR 92,233,720,368,547,758.07", goalMoney(Long.MAX_VALUE))
        assertEquals("0", goalAmountInput(0))
    }

    @Test fun `date input rejects invalid dates without changing the day`() {
        assertEquals("2028-02-29", parseGoalDate("2028-02-29").toString())
        assertNull(parseGoalDate("2027-02-29"))
        assertNull(parseGoalDate("2026-13-01"))
        assertNull(parseGoalDate("23/10/2026"))
    }
}
