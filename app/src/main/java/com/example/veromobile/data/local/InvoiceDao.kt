package com.example.veromobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class InvoiceDao {
    @Query("SELECT * FROM invoices WHERE userId = :owner ORDER BY dueDateEpochDay, id")
    abstract fun observe(owner: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id AND userId = :owner")
    abstract suspend fun find(id: String, owner: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(invoice: InvoiceEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun update(invoice: InvoiceEntity)

    // Deliberately separate from IncomeDao.insert(REPLACE): receipt must never overwrite income.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertIncome(income: IncomeEntity)

    @Query("SELECT * FROM incomes WHERE invoiceId = :id AND userId = :owner")
    abstract suspend fun linkedIncome(id: String, owner: String): List<IncomeEntity>

    @Transaction
    open suspend fun save(invoice: InvoiceEntity) {
        val existing = find(invoice.id, invoice.userId)
        if (existing == null) {
            check(invoice.revision == 0L && invoice.status == "EXPECTED") { "Invoice no longer exists" }
            insert(invoice)
        } else {
            // Repeating an identical creation is safe; stale edits must not replace newer values.
            if (existing == invoice.copy(createdAt = existing.createdAt, updatedAt = existing.updatedAt)) return
            check(existing.status == "EXPECTED" && existing.revision == invoice.revision) { "Invoice changed; reopen it before editing" }
            update(invoice.copy(createdAt = existing.createdAt, revision = existing.revision + 1,
                notifiedDueEpochDay = existing.notifiedDueEpochDay))
        }
    }

    @Transaction
    open suspend fun receive(id: String, owner: String, revision: Long, income: IncomeEntity) {
        val invoice = checkNotNull(find(id, owner)) { "Invoice not found" }
        check(income.id == "invoice-receipt:$id" && income.userId == owner && income.invoiceId == id && income.source == "FREELANCE")
        check(income.originalCurrency == invoice.currency) { "Invoice currency changed; reopen the confirmation" }
        val linked = linkedIncome(id, owner)
        if (invoice.status == "RECEIVED") {
            val previous = linked.singleOrNull()
            check(previous != null && previous.id == invoice.receivedIncomeId && previous.source == "FREELANCE" &&
                previous.originalAmountMinor == income.originalAmountMinor && previous.originalCurrency == income.originalCurrency &&
                previous.lkrAmountMinor == income.lkrAmountMinor && previous.exchangeRateMicro == income.exchangeRateMicro &&
                previous.rateSource == income.rateSource &&
                previous.dateEpochDay == income.dateEpochDay && !previous.isDeleted) { "Receipt differs from the income already recorded" }
            return
        }
        check(invoice.status == "EXPECTED" && invoice.revision == revision) { "Invoice changed; reopen the confirmation" }
        check(linked.isEmpty()) { "Invoice already has linked income; review it before receiving" }
        insertIncome(income)
        update(invoice.copy(status = "RECEIVED", receivedIncomeId = income.id,
            receivedDateEpochDay = income.dateEpochDay, receivedAmountMinor = income.originalAmountMinor,
            receivedLkrMinor = income.lkrAmountMinor, updatedAt = income.updatedAt, revision = invoice.revision + 1))
    }

    @Transaction
    open suspend fun voidInvoice(id: String, owner: String, revision: Long, now: Long) {
        val invoice = checkNotNull(find(id, owner)) { "Invoice not found" }
        if (invoice.status == "VOID") return
        check(invoice.status == "EXPECTED" && invoice.revision == revision) { "Only unchanged unpaid invoices can be voided" }
        update(invoice.copy(status = "VOID", updatedAt = now, revision = invoice.revision + 1))
    }

    @Query("SELECT * FROM invoices WHERE userId = :owner AND status = 'EXPECTED' AND dueDateEpochDay < :today AND (notifiedDueEpochDay IS NULL OR notifiedDueEpochDay != dueDateEpochDay)")
    abstract suspend fun newlyOverdue(owner: String, today: Long): List<InvoiceEntity>

    @Query("UPDATE invoices SET notifiedDueEpochDay = :due WHERE id = :id AND userId = :owner AND status = 'EXPECTED' AND dueDateEpochDay = :due")
    abstract suspend fun markNotified(id: String, owner: String, due: Long)

    @Query("SELECT id FROM invoices WHERE userId = :owner AND status != 'EXPECTED'")
    abstract suspend fun closedIds(owner: String): List<String>
}
