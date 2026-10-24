package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class InvoiceRulesTest {
    private val today = LocalDate.of(2026, 10, 24)
    private val invoice = Invoice(client = "Client", project = "Site", milestone = "Final",
        amountMinor = 15000, currency = Currency.USD, dueDate = today)

    @Test fun overdueBeginsAfterDueDateAndNeverAppliesToReceivedOrVoid() {
        assertEquals(InvoiceStatus.EXPECTED, invoice.statusOn(today))
        assertEquals(InvoiceStatus.OVERDUE, invoice.statusOn(today.plusDays(1)))
        for (status in listOf(InvoiceStatus.RECEIVED, InvoiceStatus.VOID)) {
            assertEquals(status, invoice.copy(status = status).statusOn(today.plusDays(1)))
        }
    }

    @Test fun totalsSeparateCurrencyAndExcludeReceivedAndVoid() {
        val rows = listOf(invoice, invoice.copy(currency = Currency.LKR, amountMinor = 100),
            invoice.copy(dueDate = today.minusDays(1), amountMinor = 200),
            invoice.copy(status = InvoiceStatus.RECEIVED), invoice.copy(status = InvoiceStatus.VOID))
        val expected = InvoiceRules.totals(rows, InvoiceStatus.EXPECTED, today)
        assertEquals(15000.toBigInteger(), expected[Currency.USD])
        assertEquals(100.toBigInteger(), expected[Currency.LKR])
        assertEquals(200.toBigInteger(), InvoiceRules.totals(rows, InvoiceStatus.OVERDUE, today)[Currency.USD])
    }

    @Test fun invalidOrOversizedAmountsAndRequiredFieldsAreRejected() {
        for (value in listOf("", "0", "-1", "1.001", "1000000000", "1e5", "abc")) assertNull(value, InvoiceRules.amount(value))
        assertEquals(12345L, InvoiceRules.amount("123.45"))
        assertEquals(5, InvoiceRules.errors("", "", "", "0", "bad").size)
    }
}
