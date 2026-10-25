package com.example.veromobile.ui.recurring

import androidx.activity.compose.setContent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.MainActivity
import com.example.veromobile.core.AppTime
import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.data.repository.RecurringRepository
import com.example.veromobile.data.repository.LocalOnlyFirestoreRepository
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Isolated Room/session fixture; never changes FirebaseAuth or the production finance.db. */
class RecurringUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "recurring-ui-${UUID.randomUUID()}.db"
    private lateinit var db: AppDatabase
    private val store = ViewModelStore()
    private lateinit var repository: RecurringRepository
    private lateinit var model: RecurringViewModel

    @Before fun setup() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        repository = RecurringRepository(db, LocalOnlyFirestoreRepository,
            object : UserSession { override val userId = "recurring-ui-test" }, Clock.system(AppTime.zone))
        compose.runOnUiThread {
            model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = RecurringViewModel(repository, SavedStateHandle()) as T
            })[RecurringViewModel::class.java]
            compose.activity.setContent { RecurringRoute(onBack = {}, viewModel = model) }
        }
    }
    @After fun cleanup() {
        compose.runOnUiThread { compose.activity.setContent {}; store.clear() }
        db.close(); context.deleteDatabase(name)
    }
    private fun add(name: String, amount: String, subscription: Boolean = false) {
        compose.waitUntil(10_000) { !model.state.value.loading && !model.state.value.busy }
        scrollTo("Add recurring")
        compose.onNodeWithText("Add recurring").performClick()
        if (subscription) compose.onNodeWithText("Subscription").performClick()
        compose.onNodeWithText("Name").performTextInput(name)
        compose.onNodeWithText("Expected amount (LKR)").performTextInput(amount)
        compose.onNodeWithText("Utilities").performScrollTo().performClick()
        compose.onNodeWithText("Save recurring rule").performClick()
        compose.waitUntil(10_000) { runBlocking { repository.observe().first().rules.any { it.name == name } } }
    }
    private fun scrollTo(text: String) { compose.onNodeWithTag("recurring-list").performScrollToNode(hasText(text)) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun electricityConfirmationAndNetflixCancellationWorkThroughTheUi() {
        add("Electricity", "125.00")
        screenshot("recurring-overview.png")
        scrollTo("Confirm payment")
        compose.onNodeWithText("Confirm payment").performClick()
        compose.onNodeWithText("Actual amount (LKR)").performTextReplacement("142.50")
        compose.onNodeWithText("Confirm", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { repository.observe().first().occurrences.any { it.status == "CONFIRMED" } } }
        runBlocking {
            val occurrence = repository.observe().first().occurrences.single()
            assertTrue(repository.confirm(occurrence.id, 14_250, repository.today()).isSuccess)
            assertEquals(14_250L, db.recurringDao().expense("recurring-payment:${occurrence.id}")!!.amountMinor)
            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try { assertEquals(1, reopened.recurringDao().observeOccurrences("recurring-ui-test").first().size) }
            finally { reopened.close() }
        }
        add("Netflix", "1500.00", subscription = true)
        scrollTo("Manage subscriptions")
        compose.onNodeWithText("Manage subscriptions").performClick()
        compose.onNodeWithText("LKR 1,500.00").assertExists()
        screenshot("recurring-subscriptions.png")
        scrollTo("Cancel")
        compose.onNodeWithText("Cancel", substring = false).performClick()
        compose.onNodeWithText("Cancel rule").performClick()
        compose.waitUntil(10_000) { runBlocking { repository.observe().first().rules.any { it.name == "Netflix" && !it.active } } }
        compose.onNodeWithText("LKR 0.00").assertExists()
        runBlocking {
            repository.refresh().getOrThrow()
            assertEquals(1, repository.observe().first().occurrences.count { it.status == "CONFIRMED" })
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM expenses").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        }
    }
}
