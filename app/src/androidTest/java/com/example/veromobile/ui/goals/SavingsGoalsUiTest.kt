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
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import com.example.veromobile.data.firestore.FirestoreCollection
import com.example.veromobile.data.firestore.UserFirestoreDocument
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
            AppDatabase::class.java, databaseName)
            .addMigrations(com.example.veromobile.data.local.GoalMigrations.MIGRATION_1_2,
                com.example.veromobile.data.local.GoalMigrations.MIGRATION_2_3,
                com.example.veromobile.data.local.InvoiceMigrations.MIGRATION_3_4, com.example.veromobile.data.local.RecurringMigrations.MIGRATION_4_5).build()
        val roomRepository = SavingsGoalRepositoryImpl(db.goalDao(), GoalsTestFirestore,
            object : UserSession { override val userId = "ui-test" }, clock)
        repository = object : SavingsGoalRepository by roomRepository {
            override fun observeGoalDetails(goalId: String, today: LocalDate): Flow<GoalResult<GoalDetails?>> =
                if (failDetailsRead) flowOf(GoalResult.Failure(GoalError.Storage))
                else roomRepository.observeGoalDetails(goalId, today)
        }
    }

    private fun scrollLandingTo(matcher: SemanticsMatcher) {
        compose.onNodeWithTag("goals-landing").performScrollToNode(matcher)
    }

    private fun createGoal(name: String = "Laptop", openDetails: Boolean = true) {
        scrollLandingTo(hasText("Goal name"))
        compose.onNodeWithText("Goal name").performTextReplacement(name)
        compose.onNodeWithText("Target amount (LKR)").performTextReplacement("490000")
        compose.onNodeWithText("Opening savings (LKR)").performTextReplacement("11200")
        closeSoftKeyboard()
        compose.onNodeWithText("Create Goal").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.goals.any { it.name == name } && !viewModel.uiState.value.form.isSaving }
        assertEquals(GoalPage.LANDING, viewModel.uiState.value.page)
        assertEquals("", viewModel.uiState.value.form.name)
        scrollLandingTo(hasText(name))
        compose.onNodeWithText(name).assertIsDisplayed()
        if (openDetails) {
            compose.onNodeWithText(name).performClick()
            compose.waitUntil(5000) { viewModel.uiState.value.details?.goal?.name == name }
        }
    }

    @Test fun emptyStateAndFieldErrorsAreVisible() {
        compose.onNodeWithText("Create New Goal").assertIsDisplayed()
        captureReviewScreenshot("landing-top")
        scrollLandingTo(hasText("My Goals"))
        compose.onNodeWithText("My Goals").assertIsDisplayed()
        scrollLandingTo(hasText("Your first milestone is waiting"))
        compose.onNodeWithText("Your first milestone is waiting").assertIsDisplayed()
        captureReviewScreenshot("empty")
        scrollLandingTo(hasText("Create Goal"))
        compose.onNodeWithText("Create Goal").performClick()
        compose.onNodeWithText("Enter a goal name").assertExists()
        compose.onNodeWithText("Enter a valid amount with up to 2 decimal places").assertExists()
        assertEquals(GoalPage.LANDING, viewModel.uiState.value.page)
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
        compose.waitUntil(5000) { !viewModel.uiState.value.isLoading && viewModel.uiState.value.goals.any { it.name == "Laptop updated" } }
        assertEquals(GoalPage.LANDING, viewModel.uiState.value.page)
        scrollLandingTo(hasText("Laptop updated"))
        compose.onNodeWithText("Laptop updated").assertIsDisplayed()
        assertEquals(afterEdit, runBlocking { db.goalDao().getGoalDetails(goalId, "ui-test") })
        assertEquals(1, runBlocking { db.goalDao().observeGoals("ui-test").first().size })
    }

    @Test fun multipleGoalsOpenIndependentDetailsAndBackReturnsToLanding() {
        createGoal("Laptop", openDetails = false)
        createGoal("Holiday", openDetails = false)
        assertEquals(2, viewModel.uiState.value.goals.size)
        scrollLandingTo(hasText("Laptop"))
        compose.onNodeWithText("Laptop").performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details?.goal?.name == "Laptop" }
        compose.onNodeWithText("+5k").performScrollTo().performClick()
        compose.onNodeWithText("Confirm contribution").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details?.contributions?.size == 1 }
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithTag("goals-landing").assertExists()
        scrollLandingTo(hasText("Holiday"))
        compose.onNodeWithText("Holiday").performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details?.goal?.name == "Holiday" }
        assertTrue(viewModel.uiState.value.details!!.contributions.isEmpty())
        assertEquals(1_120_000L, viewModel.uiState.value.details!!.summary.currentSavingsMinor)
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        assertEquals(GoalPage.LANDING, viewModel.uiState.value.page)
        scrollLandingTo(hasText("Holiday"))
        captureReviewScreenshot("multiple-goals")
    }

    @Test fun createDraftSurvivesDetailEditingAndFailedSave() {
        createGoal(openDetails = false)
        scrollLandingTo(hasText("Goal name"))
        compose.onNodeWithText("Goal name").performClick().performTextReplacement("Camera draft")
        compose.onNodeWithText("Target amount (LKR)").performScrollTo().performClick().performTextReplacement("75000")
        compose.onNodeWithText("Create Goal").performScrollTo().assertIsDisplayed()
        captureReviewScreenshot("keyboard")
        closeSoftKeyboard()
        scrollLandingTo(hasText("Laptop"))
        compose.onNodeWithText("Laptop").performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details != null }
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Goal name").performTextReplacement("Laptop edited")
        closeSoftKeyboard()
        compose.onNodeWithText("Save changes").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.details?.goal?.name == "Laptop edited" }
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        scrollLandingTo(hasText("Goal name"))
        compose.onNodeWithText("Goal name").assertTextContains("Camera draft")
        compose.onNodeWithText("Target amount (LKR)").assertTextContains("75000")
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_ui_goal BEFORE INSERT
            ON savings_goals BEGIN SELECT RAISE(ABORT, 'test failure'); END""")
        compose.onNodeWithText("Create Goal").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.form.error != null }
        assertEquals("Camera draft", viewModel.uiState.value.form.name)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_ui_goal")
        compose.onNodeWithText("Create Goal").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.goals.size == 2 && viewModel.uiState.value.form.name.isEmpty() }
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
        val saved = SavedStateHandle(mapOf("goal.page" to GoalPage.LANDING.name, "create.id" to "restored-goal",
            "create.createdAt" to 1L, "goal.name" to "Restored", "goal.target" to "1000",
            "goal.opening" to "50", "goal.date" to "2027-10-23"))
        lateinit var restored: SavingsGoalsViewModel
        compose.runOnIdle {
            restored = SavingsGoalsViewModel(repository, saved, clock)
            store.put("restored", restored)
            assertEquals("Restored", restored.uiState.value.form.name)
            restored.saveGoal()
        }
        compose.waitUntil(5000) { restored.uiState.value.goals.any { it.id == "restored-goal" } }
        assertEquals(5000L, restored.uiState.value.goals.single { it.id == "restored-goal" }.openingAmountMinor)
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

/** Local persistence tests must never contact a real Firebase account. */
internal object GoalsTestFirestore : FirestoreRepository {
    override suspend fun <T : UserFirestoreDocument> create(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> update(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> get(
        collection: FirestoreCollection<T>, documentId: String
    ): FirestoreResult<T?> = error("Not used by local Goals tests")

    override suspend fun <T : UserFirestoreDocument> softDelete(
        collection: FirestoreCollection<T>, documentId: String, updatedAt: Long
    ): FirestoreResult<Unit> = error("Not used by local Goals tests")

    override fun <T : UserFirestoreDocument> observeAll(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> =
        error("Not used by local Goals tests")

    override fun <T : UserFirestoreDocument> observeMonthly(
        collection: FirestoreCollection<T>, startEpochDay: Long, endEpochDayExclusive: Long
    ): Flow<FirestoreResult<List<T>>> =
        error("Not used by local Goals tests")
}
