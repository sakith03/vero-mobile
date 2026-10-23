package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GoalValidatorTest {
    private val today = LocalDate.of(2026, 10, 23)
    private val goal = SavingsGoal(id = "goal", name = "MacBook", targetAmountMinor = 100, targetDate = today)
    private val contribution = GoalContribution(id = "c1", goalId = "goal", amountMinor = 1, date = today)

    @Test fun `valid goal and contribution have no errors`() {
        assertTrue(GoalValidator.validateGoal(goal).isValid)
        assertTrue(GoalValidator.validateContribution(contribution, today).isValid)
    }

    @Test fun `goal field errors are independently addressable`() {
        val errors = GoalValidator.validateGoal(goal.copy(id = "", name = " \t", targetAmountMinor = 0,
            openingAmountMinor = -1, targetDate = LocalDate.MAX)).errors
        assertEquals(setOf(GoalField.ID, GoalField.NAME, GoalField.TARGET_AMOUNT,
            GoalField.OPENING_AMOUNT, GoalField.TARGET_DATE), errors.keys)
        assertTrue(errors.values.all { it.isNotBlank() })
    }

    @Test fun `zero negative and minimum long targets are invalid`() {
        listOf(0L, -1L, Long.MIN_VALUE).forEach {
            assertTrue(GoalValidator.validateGoal(goal.copy(targetAmountMinor = it)).errors.containsKey(GoalField.TARGET_AMOUNT))
        }
    }

    @Test fun `zero negative and minimum long contributions are invalid`() {
        listOf(0L, -1L, Long.MIN_VALUE).forEach {
            assertTrue(GoalValidator.validateContribution(contribution.copy(amountMinor = it), today).errors.containsKey(GoalField.AMOUNT))
        }
    }

    @Test fun `zero opening overdue target and excess savings are valid`() {
        assertTrue(GoalValidator.validateGoal(goal.copy(openingAmountMinor = 0, targetDate = today.minusYears(2))).isValid)
        assertTrue(GoalValidator.validateGoal(goal.copy(openingAmountMinor = 200)).isValid)
    }

    @Test fun `long maximum monetary values are valid`() {
        assertTrue(GoalValidator.validateGoal(goal.copy(targetAmountMinor = Long.MAX_VALUE, openingAmountMinor = Long.MAX_VALUE)).isValid)
        assertTrue(GoalValidator.validateContribution(contribution.copy(amountMinor = Long.MAX_VALUE), today).isValid)
    }

    @Test fun `contribution validates against existing balance without overflow`() {
        assertTrue(GoalValidator.validateContribution(contribution, today, Long.MAX_VALUE - 1).isValid)
        assertFalse(GoalValidator.validateContribution(contribution, today, Long.MAX_VALUE).isValid)
    }

    @Test fun `contribution IDs and dates have field errors`() {
        val errors = GoalValidator.validateContribution(contribution.copy(id = " ", goalId = "", date = today.plusDays(1)), today).errors
        assertEquals(setOf(GoalField.ID, GoalField.GOAL_ID, GoalField.DATE), errors.keys)
    }

    @Test fun `supported goal dates include both endpoints`() {
        listOf(GoalValidator.minDate, GoalValidator.maxDate).forEach {
            assertTrue(GoalValidator.validateGoal(goal.copy(targetDate = it)).isValid)
        }
        listOf(GoalValidator.minDate.minusDays(1), GoalValidator.maxDate.plusDays(1)).forEach {
            assertFalse(GoalValidator.validateGoal(goal.copy(targetDate = it)).isValid)
        }
    }

    @Test fun `contribution dates allow history and today but reject extreme dates`() {
        assertTrue(GoalValidator.validateContribution(contribution.copy(date = GoalValidator.minDate), today).isValid)
        listOf(LocalDate.MIN, LocalDate.MAX).forEach {
            assertTrue(GoalValidator.validateContribution(contribution.copy(date = it), today).errors.containsKey(GoalField.DATE))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid existing balance is rejected`() {
        GoalValidator.validateContribution(contribution, today, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported reference date is rejected`() {
        GoalValidator.validateContribution(contribution, LocalDate.MAX)
    }

    @Test fun `copy preserves stable model identities`() {
        assertEquals(goal.id, goal.copy(name = "Updated").id)
        assertEquals(contribution.id, contribution.copy(note = "Updated").id)
        assertTrue(SavingsGoal(name = "A", targetAmountMinor = 1, targetDate = today).id.isNotBlank())
    }
}
