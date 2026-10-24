package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.Currency
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IncomeCalcTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test fun `120 USD for 35880 LKR derives rate 299`() {
        val rate = CurrencyCalculator.deriveRateMicro(12000, 3588000)
        assertEquals(299_000_000L, rate)
        assertEquals("299.00", CurrencyCalculator.formatRate(rate!!))
    }

    @Test fun `last rate 299_40 converts 120 USD to 35928 LKR`() =
        assertEquals(3_592_800L, CurrencyCalculator.toLkrMinor(12000, 299_400_000))

    @Test fun `no rate when an amount is zero`() =
        assertNull(CurrencyCalculator.deriveRateMicro(0, 100))

    @Test fun `zero amount is rejected`() {
        val r = IncomeValidator.validate(Currency.LKR, 0, 0, today, today)
        assertEquals("Enter an amount greater than 0", r.amountError)
    }

    @Test fun `USD needs the LKR received`() {
        val r = IncomeValidator.validate(Currency.USD, 12000, 0, today, today)
        assertNotNull(r.lkrError)
    }

    @Test fun `date far in the future is rejected`() {
        val r = IncomeValidator.validate(Currency.LKR, 100, 100, today.plusYears(2), today)
        assertNotNull(r.dateError)
    }

    @Test fun `valid LKR income passes`() =
        assertEquals(true, IncomeValidator.validate(Currency.LKR, 12500000, 12500000, today, today).isValid)
}
