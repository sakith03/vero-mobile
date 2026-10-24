package com.example.veromobile.data.repository

import androidx.room.withTransaction
import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.data.firestore.toFirestoreDto
import com.example.veromobile.data.local.*
import com.example.veromobile.di.RecurringClock
import com.example.veromobile.domain.calc.RecurrenceCalculator
import com.example.veromobile.domain.model.*
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

class RecurringRepository @Inject constructor(
    private val db: AppDatabase,
    private val firestoreRepository: FirestoreRepository,
    private val session: UserSession,
    @param:RecurringClock private val clock: Clock
) {
    // A repository belongs to the graph that created it; an old draft cannot follow a new account.
    private val boundOwner = session.userId
    private val dao get() = db.recurringDao()
    fun currentOwner(): String? = session.userId.takeIf { it.isNotBlank() && it != "dev-user" }
    fun today(): LocalDate = LocalDate.now(clock)
    private fun owner() = session.userId.also {
        check(it.isNotBlank() && it != "dev-user" && it == boundOwner) { "Sign in and reopen recurring payments" }
    }
    private fun checkOwner(user: String) { check(owner() == user) { "Account changed. Reopen recurring payments." } }

    fun observe(): Flow<RecurringOverview> = flow {
        val user = owner()
        emitAll(combine(dao.observeRules(user), dao.observeOccurrences(user)) { rules, occurrences ->
            checkOwner(user)
            RecurringOverview(rules.map { it.toDomain() }, occurrences.map { it.toDomain() })
        })
    }

    suspend fun find(id: String): RecurringRule? {
        val user = owner()
        val row = dao.rule(id, user)
        checkOwner(user)
        return row?.toDomain()
    }

    suspend fun save(rule: RecurringRule): Result<Unit> = safely {
        require(rule.id.isNotBlank() && rule.name.isNotBlank() && rule.name.length <= 100)
        require(rule.amountMinor in 1..99_999_999_999L && rule.initialDate.year in 1900..9998)
        require(DefaultCategories.all.any { it.id == rule.categoryId }) { "Choose a category" }
        require(rule.kind != RecurringKind.SUBSCRIPTION || rule.frequency == RecurringFrequency.MONTHLY) {
            "Subscriptions use monthly billing"
        }
        val user = owner()
        db.withTransaction {
            checkOwner(user)
            val existing = dao.rule(rule.id, user)
            if (existing == null) {
                check(rule.revision == 0L && rule.active)
                dao.insert(RecurringRuleEntity(rule.id, user, rule.name.trim(), rule.amountMinor, rule.frequency.name,
                    rule.initialDate.toEpochDay(), rule.categoryId, rule.channel.name, rule.kind.name, true,
                    rule.initialDate.toEpochDay(), 0, clock.millis(), clock.millis()))
            } else {
                check(existing.active && existing.revision == rule.revision) { "Rule changed. Reopen it before editing." }
                check(existing.nextDateEpochDay > today().toEpochDay()) { "Load the remaining due bills before editing this rule." }
                // Existing due snapshots remain payable; edits apply only to dates after today.
                val next = RecurrenceCalculator.after(rule.initialDate, rule.frequency, today())
                dao.update(existing.copy(name = rule.name.trim(), amountMinor = rule.amountMinor,
                    frequency = rule.frequency.name, initialDateEpochDay = rule.initialDate.toEpochDay(),
                    categoryId = rule.categoryId, channel = rule.channel.name, kind = rule.kind.name,
                    nextDateEpochDay = next.toEpochDay(), revision = existing.revision + 1, updatedAt = clock.millis()))
            }
            generate(user)
            checkOwner(user)
        }
        firestoreRepository.create(FirestoreCollections.recurringRules, rule.toFirestoreDto(user))
    }

    /** At most 120 due rows per rule per refresh; never pre-generate future financial records. */
    suspend fun refresh(): Result<Unit> = safely {
        val user = owner()
        db.withTransaction { checkOwner(user); generate(user); checkOwner(user) }
    }

    private suspend fun generate(user: String) {
        for (rule in dao.activeRules(user)) {
            var next = LocalDate.ofEpochDay(rule.nextDateEpochDay)
            val anchor = LocalDate.ofEpochDay(rule.initialDateEpochDay).dayOfMonth
            var count = 0
            while (next <= today() && count < 120) {
                checkOwner(user)
                val id = "${rule.id}:${next.toEpochDay()}"
                if (dao.occurrence(id, user) == null) dao.insertOccurrence(RecurringOccurrenceEntity(
                    id, rule.id, user, rule.name, rule.amountMinor, next.toEpochDay(), rule.categoryId, rule.channel))
                next = RecurrenceCalculator.next(next, RecurringFrequency.valueOf(rule.frequency), anchor)
                count++
            }
            if (count > 0) dao.update(rule.copy(nextDateEpochDay = next.toEpochDay()))
        }
    }

    suspend fun confirm(occurrenceId: String, amountMinor: Long, date: LocalDate): Result<Unit> = safely {
        require(amountMinor in 1..99_999_999_999L) { "Enter a positive amount with up to 2 decimals" }
        require(date.year in 1900..9998 && date <= today()) { "Choose a payment date no later than today" }
        val user = owner()
        db.withTransaction {
            checkOwner(user)
            val row = checkNotNull(dao.occurrence(occurrenceId, user)) { "Payment no longer exists" }
            check(row.scheduledDateEpochDay <= today().toEpochDay()) { "This payment is not due yet" }
            val expense = Expense(id = "recurring-payment:$occurrenceId", amountMinor = amountMinor,
                categoryId = row.categoryId, date = date, channel = PaymentChannel.valueOf(row.channel),
                note = "${row.name} · scheduled ${LocalDate.ofEpochDay(row.scheduledDateEpochDay)}",
                updatedAt = clock.millis()).toEntity(user)
            dao.confirm(occurrenceId, user, expense)
            checkOwner(user)
        }
    }

    suspend fun cancel(rule: RecurringRule): Result<Unit> = safely {
        val user = owner()
        db.withTransaction {
            checkOwner(user)
            val row = checkNotNull(dao.rule(rule.id, user)) { "Rule no longer exists" }
            if (row.active) {
                check(row.revision == rule.revision) { "Rule changed. Reopen it before cancelling." }
                dao.update(row.copy(active = false, revision = row.revision + 1, updatedAt = clock.millis()))
                dao.cancelPending(row.id, user)
            }
            checkOwner(user)
        }
        firestoreRepository.update(FirestoreCollections.recurringRules, rule.copy(active = false).toFirestoreDto(user))
    }

    private suspend fun safely(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }
}
