package com.example.veromobile.ui.goals

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.data.repository.SavingsGoalRepositoryImpl
import com.example.veromobile.domain.session.UserSession
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.repository.GoalError
import com.example.veromobile.domain.repository.GoalResult
import com.example.veromobile.domain.repository.SavingsGoalRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.UUID
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SavingsGoalsUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var repository: SavingsGoalRepository
    private lateinit var viewModel: SavingsGoalsViewModel
    private val displayedViewModel = mutableStateOf<SavingsGoalsViewModel?>(null)
    private val store = ViewModelStore()
    private val databaseName = "goals-ui-${UUID.randomUUID()}.db"
    private var failDetailsRead = false
    private val clock = Clock.fixed(Instant.parse("2026-10-23T06:00:00Z"), ZoneId.of("Asia/Colombo"))

    @Before fun setUp() {
        openDatabase()
        compose.runOnIdle {
            viewModel = SavingsGoalsViewModel(repository, SavedStateHandle(), clock)
            store.put("goals", viewModel)
            displayedViewModel.value = viewModel
        }
        compose.setContent { SavingsGoalsRoute(onBack = {}, viewModel = checkNotNull(displayedViewModel.value)) }
        compose.waitUntil(5000) { !viewModel.uiState.value.isLoading }
    }

    @After fun tearDown() {
        compose.runOnIdle { store.clear() }
        db.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
    }

    private fun openDatabase() {
        // A private on-disk test database lets us verify persistence across a fresh connection/VM.
        db = Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java, databaseName).build()
        val roomRepository = SavingsGoalRepositoryImpl(db.goalDao(), object : UserSession { override val userId = "ui-test" }, clock)
        repository = object : SavingsGoalRepository by roomRepository {
            override fun observeGoalDetails(goalId: String, today: LocalDate): Flow<GoalResult<GoalDetails?>> =
                if (failDetailsRead) flowOf(GoalResult.Failure(GoalError.Storage))
                else roomRepository.observeGoalDetails(goalId, today)
        }
    }

    private fun createGoal() {
        compose.onNodeWithText("Create goal").performClick()
        compose.onNodeWithText("Goal name").performTextInput("Laptop")
        compose.onNodeWithText("Target amount (LKR)").performTextInput("490000")
        compose.onNodeWithText("Opening savings (LKR)").performTextReplacement("11200")
        compose.onNodeWithText("Save goal").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.page == GoalPage.DETAILS && viewModel.uiState.value.details != null }
    }

    @Test fun emptyStateAndFieldErrorsAreVisible() {
        compose.onNodeWithText("Make room for your next goal").assertIsDisplayed()
        captureReviewScreenshot("empty")
        compose.onNodeWithText("Create goal").performClick()
        compose.onNodeWithText("Save goal").performScrollTo().performClick()
        compose.onNodeWithText("Enter a goal name").assertExists()
        compose.onNodeWithText("Enter a valid amount with up to 2 decimal places").assertExists()
        assertEquals(GoalPage.CREATE, viewModel.uiState.value.page)
    }

    @Test fun createContributeAndEditUseRealRoomData() {
        createGoal()
        compose.onNodeWithText("Laptop").assertIsDisplayed()
        compose.onNodeWithText("+10k").performScrollTo().performClick()
        compose.onNodeWithText("Amount (LKR)").assertTextContains("10000")
        compose.onNodeWithText("Note (optional)").performTextInput("Freelance saving")
        compose.onNodeWithText("Confirm contribution").performScrollTo().performClick()
        compose.waitUntil(5000) { !viewModel.uiState.value.contribution.isOpen && viewModel.uiState.value.details?.contributions?.size == 1 }
        assertEquals(2_120_000L, viewModel.uiState.value.details!!.summary.currentSavingsMinor)
        val goalId = viewModel.uiState.value.details!!.goal.id
        val beforeEdit = runBlocking { db.goalDao().getGoalDetails(goalId, "ui-test") }!!
        compose.onNodeWithText("Edit").performScrollTo().performClick()
        compose.onNodeWithText("Goal name").performClick().performTextReplacement("Laptop updated")
        compose.onNodeWithText("Goal name").assertTextContains("Laptop updated")
        compose.runOnIdle { assertEquals("Laptop updated", viewModel.uiState.value.form.name) }
        closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText("Save changes").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        try {
            compose.waitUntil(5000) {
                viewModel.uiState.value.page == GoalPage.DETAILS && viewModel.uiState.value.details?.goal?.name == "Laptop updated"
            }
        } catch (error: Exception) {
            throw AssertionError("Edit state: ${viewModel.uiState.value}", error)
        }
        compose.onNodeWithText("Laptop updated").assertIsDisplayed()
        assertEquals(1, viewModel.uiState.value.details!!.contributions.size)
        assertEquals(3_906_667L, viewModel.uiState.value.details!!.summary.requiredMonthlySavingsMinor)
        val afterEdit = runBlocking { db.goalDao().getGoalDetails(goalId, "ui-test") }!!
        assertEquals("Laptop updated", afterEdit.goal.name)
        assertEquals(beforeEdit.goal.id, afterEdit.goal.id)
        assertEquals(beforeEdit.goal.createdAt, afterEdit.goal.createdAt)
        assertEquals(beforeEdit.contributions, afterEdit.contributions)
        assertEquals(1, runBlocking { db.goalDao().observeGoals("ui-test").first().size })

        // Cold reload: discard all ViewModels and reopen the file, with no saved UI state.
        compose.runOnIdle { store.clear() }
        db.close()
        openDatabase()
        compose.runOnIdle {
            viewModel = SavingsGoalsViewModel(repository, SavedStateHandle(), clock)
            store.put("goals", viewModel)
            displayedViewModel.value = viewModel
        }
        compose.waitUntil(5000) { !viewModel.uiState.value.isLoading && viewModel.uiState.value.details?.goal?.name == "Laptop updated" }
        compose.onNodeWithText("Laptop updated").assertIsDisplayed()
        assertEquals(afterEdit, runBlocking { db.goalDao().getGoalDetails(goalId, "ui-test") })
        assertEquals(1, runBlocking { db.goalDao().observeGoals("ui-test").first().size })
    }

    @Test fun failedWriteKeepsContributionInputAndRetryUsesTheSameId() {
        createGoal()
        compose.onNodeWithText("+5k").performScrollTo().performClick()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_ui_contribution BEFORE INSERT
            ON goal_contributions BEGIN SELECT RAISE(ABORT, 'test failure'); END""")
        compose.onNodeWithText("Confirm contribution").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.contribution.error != null }
        assertEquals("5000", viewModel.uiState.value.contribution.amount)
        assertTrue(viewModel.uiState.value.contribution.isOpen)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_ui_contribution")
        compose.onNodeWithText("Confirm contribution").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details?.contributions?.size == 1 }
        assertEquals(1_620_000L, viewModel.uiState.value.details!!.summary.currentSavingsMinor)
    }

    @Test fun duplicateClicksCannotSubmitTwice() {
        createGoal()
        compose.runOnIdle {
            viewModel.openContribution(100)
            viewModel.saveContribution()
            viewModel.saveContribution()
        }
        compose.waitUntil(5000) { viewModel.uiState.value.details?.contributions?.size == 1 }
        assertEquals(1, viewModel.uiState.value.details!!.contributions.size)
    }

    @Test fun formRestoresDraftAndStableIdFromSavedState() {
        val saved = SavedStateHandle(mapOf("goal.page" to GoalPage.CREATE.name, "goal.draftId" to "restored-goal",
            "goal.createdAt" to 1L, "goal.name" to "Restored", "goal.target" to "1000",
            "goal.opening" to "50", "goal.date" to "2027-10-23"))
        lateinit var restored: SavingsGoalsViewModel
        compose.runOnIdle {
            restored = SavingsGoalsViewModel(repository, saved, clock)
            store.put("restored", restored)
            assertEquals("Restored", restored.uiState.value.form.name)
            restored.saveGoal()
        }
        compose.waitUntil(5000) { restored.uiState.value.details?.goal?.id == "restored-goal" }
        assertEquals(5000L, restored.uiState.value.details!!.goal.openingAmountMinor)
    }

    @Test fun detailReadErrorsAreDistinctFromEmptyStateAndCanRetry() {
        createGoal()
        val goalId = viewModel.uiState.value.details!!.goal.id
        // Closing Room ends its connection scope; it is not a deterministic query failure.
        // Inject the repository's explicit failure result, then retry the healthy Room delegate.
        compose.runOnIdle {
            failDetailsRead = true
            viewModel.selectGoal(goalId)
        }
        compose.waitUntil(5000) { viewModel.uiState.value.error != null }
        assertFalse(viewModel.uiState.value.isLoading)
        compose.onNodeWithText("Couldn't load savings goals").assertIsDisplayed()
        compose.onNodeWithText("Couldn't access your saved goals. Your entry is kept; please try again.").assertIsDisplayed()
        compose.onNodeWithText("Make room for your next goal").assertDoesNotExist()
        compose.onNodeWithText("Retry").assertIsDisplayed()
        captureReviewScreenshot("load-error")
        compose.runOnIdle { failDetailsRead = false }
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(5000) { !viewModel.uiState.value.isLoading && viewModel.uiState.value.details?.goal?.id == goalId }
        assertNull(viewModel.uiState.value.error)
        compose.onNodeWithText("Couldn't load savings goals").assertDoesNotExist()
        compose.onNodeWithText("Laptop").assertIsDisplayed()
    }

    private fun captureReviewScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "goals-review").apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }
}
