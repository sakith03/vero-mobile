package com.example.veromobile.data.repository

import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.data.firestore.toFirestoreDto
import com.example.veromobile.data.local.*
import com.example.veromobile.di.InvoiceClock
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.calc.IncomeValidator
import com.example.veromobile.domain.calc.InvoiceRules
import com.example.veromobile.domain.model.*
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect

class InvoiceRepository @Inject constructor(
    private val dao: InvoiceDao,
    private val firestoreRepository: FirestoreRepository,
    private val session: UserSession,
    @param:InvoiceClock private val clock: Clock
) {
    fun today(): LocalDate = LocalDate.now(clock)
    private fun owner() = session.userId.also { check(it.isNotBlank()) { "Local user is unavailable" } }

    fun observe(): Flow<List<Invoice>> = flow {
        val user = owner()
        dao.observe(user).map { rows ->
            check(user == owner()) { "Local user changed; reload invoices" }
            rows.map { it.toDomain() }
        }.collect { emit(it) }
    }

    suspend fun find(id: String): Invoice? = dao.find(id, owner())?.toDomain()

    suspend fun save(invoice: Invoice): Result<Unit> = safely {
        require(invoice.id.isNotBlank() && invoice.isUnpaid)
        require(InvoiceRules.errors(invoice.client, invoice.project, invoice.milestone,
            CurrencyCalculator.toPlainText(invoice.amountMinor), invoice.dueDate.toString()).isEmpty()) { "Check the invoice fields" }
        val user = owner()
        val existing = dao.find(invoice.id, user)
        val entity = invoice.toEntity(user).copy(createdAt = existing?.createdAt ?: clock.millis(),
            updatedAt = clock.millis(), notifiedDueEpochDay = existing?.notifiedDueEpochDay)
        dao.save(entity)
        firestoreRepository.create(FirestoreCollections.invoices, invoice.toFirestoreDto(user))
    }

    suspend fun receive(invoice: Invoice, receipt: InvoiceReceipt): Result<Unit> = safely {
        val user = owner()
        require(receipt.amountMinor in 1..InvoiceRules.MAX_AMOUNT_MINOR &&
            receipt.lkrMinor in 1..InvoiceRules.MAX_AMOUNT_MINOR) { "Enter positive received amounts within the supported range" }
        val validation = IncomeValidator.validate(invoice.currency, receipt.amountMinor, receipt.lkrMinor, receipt.date, today())
        require(validation.isValid && receipt.date <= today()) { "Check the received amounts and actual date" }
        val rate = if (invoice.currency == Currency.LKR) CurrencyCalculator.LKR_RATE_MICRO
            else requireNotNull(CurrencyCalculator.deriveRateMicro(receipt.amountMinor, receipt.lkrMinor))
        val income = Income(id = "invoice-receipt:${invoice.id}", source = IncomeSource.FREELANCE,
            originalAmountMinor = receipt.amountMinor, originalCurrency = invoice.currency,
            exchangeRateMicro = rate, rateSource = if (invoice.currency == Currency.LKR) RateSource.NONE else RateSource.MANUAL,
            lkrAmountMinor = if (invoice.currency == Currency.LKR) receipt.amountMinor else receipt.lkrMinor,
            date = receipt.date, note = "${invoice.client} · ${invoice.project} · ${invoice.milestone}",
            invoiceId = invoice.id, updatedAt = clock.millis())
        dao.receive(invoice.id, user, invoice.revision, income.toEntity(user))

        val updatedInvoice = invoice.copy(status = InvoiceStatus.RECEIVED, receivedIncomeId = income.id, updatedAt = clock.millis())
        firestoreRepository.create(FirestoreCollections.invoices, updatedInvoice.toFirestoreDto(user))
        firestoreRepository.create(FirestoreCollections.incomes, income.toFirestoreDto(user))
    }

    suspend fun void(invoice: Invoice): Result<Unit> = safely {
        val user = owner()
        dao.voidInvoice(invoice.id, user, invoice.revision, clock.millis())
        val inv = find(invoice.id)
        if (inv != null) {
            firestoreRepository.update(FirestoreCollections.invoices, inv.toFirestoreDto(user))
        }
    }

    private suspend fun safely(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
