package com.example.veromobile.ui.review

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.core.AppTime
import com.example.veromobile.data.firestore.*
import com.example.veromobile.data.local.*
import com.example.veromobile.data.repository.*
import com.example.veromobile.domain.repository.*
import com.example.veromobile.domain.session.UserSession
import com.example.veromobile.ui.auth.AccountScreen
import com.example.veromobile.ui.components.*
import com.example.veromobile.ui.dashboard.*
import com.example.veromobile.ui.expense.*
import com.example.veromobile.ui.goals.*
import com.example.veromobile.ui.income.*
import com.example.veromobile.ui.invoice.*
import com.example.veromobile.ui.recurring.*
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/** Debug-only manual review. No seeded money, Firebase credentials, uploads or production finance.db. */
class FeatureReviewActivity : ComponentActivity() {
    private val owner = object : UserSession { override val userId = "ui-review-local" }
    private val clock = Clock.system(AppTime.zone)
    private val database by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "ui-review.db")
        .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3,
            InvoiceMigrations.MIGRATION_3_4, RecurringMigrations.MIGRATION_4_5).build() }
    private val cloud by lazy { ReviewData(database, owner.userId) }
    private val goalsRepository by lazy { SavingsGoalRepositoryImpl(database.goalDao(), cloud, owner, clock) }
    private val invoiceRepository by lazy { InvoiceRepository(database.invoiceDao(), cloud, owner, clock) }
    private val recurringRepository by lazy { RecurringRepository(database, cloud, owner, clock) }
    private val incomeRepository by lazy { IncomeRepositoryImpl(database.incomeDao(), cloud, owner) }
    private val expenseRepository by lazy { ExpenseRepositoryImpl(database.expenseDao(), cloud, owner) }

    private fun <T : ViewModel> model(key: String, type: Class<T>, build: () -> T): T =
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = build() as V
        })[key, type]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val dashboard = model("dashboard", DashboardViewModel::class.java) { DashboardViewModel(cloud, incomeRepository, goalsRepository) }
        var failContribution = intent.getBooleanExtra("failContribution", false)
        val goals = model("goals", SavingsGoalsViewModel::class.java) { SavingsGoalsViewModel(object : SavingsGoalRepository by goalsRepository {
            override suspend fun addContribution(contribution: com.example.veromobile.domain.model.GoalContribution): GoalResult<Unit> {
                if (failContribution) { failContribution = false; return GoalResult.Failure(GoalError.Storage) }
                return goalsRepository.addContribution(contribution)
            }
        }, SavedStateHandle(), clock) }
        val previewGoals = model("previewGoals", SavingsGoalsViewModel::class.java) { SavingsGoalsViewModel(goalsRepository, SavedStateHandle(), clock) }
        val invoices = model("invoices", InvoiceViewModel::class.java) { InvoiceViewModel(invoiceRepository, SavedStateHandle()) }
        val recurring = model("recurring", RecurringViewModel::class.java) { RecurringViewModel(recurringRepository, SavedStateHandle()) }
        var failIncome = intent.getBooleanExtra("failIncome", false)
        var failExpense = intent.getBooleanExtra("failExpense", false)
        val income = model("income", AddIncomeViewModel::class.java) { AddIncomeViewModel(object : IncomeRepository by incomeRepository {
            override suspend fun addIncome(income: com.example.veromobile.domain.model.Income): Result<Unit> {
                if (failIncome) { failIncome = false; return Result.failure(IllegalStateException("Review-only failure before any write")) }
                return incomeRepository.addIncome(income)
            }
        }) }
        val expense = model("expense", AddExpenseViewModel::class.java) { AddExpenseViewModel(object : ExpenseRepository by expenseRepository {
            override suspend fun addExpense(expense: com.example.veromobile.domain.model.Expense): Result<Unit> {
                if (failExpense) { failExpense = false; return Result.failure(IllegalStateException("Review-only failure before any write")) }
                return expenseRepository.addExpense(expense)
            }
        }) }
        setContent {
            var page by rememberSaveable { mutableStateOf(intent.getStringExtra("screen") ?: "dashboard") }
            var invoiceId by rememberSaveable { mutableStateOf(intent.getStringExtra("invoiceId")) }
            val tab = when (page) { "goals" -> AppNavTab.GOALS; "recurring" -> AppNavTab.RECURRING
                "income", "expense" -> AppNavTab.ADD; else -> AppNavTab.DASHBOARD }
            val selectType: (EntryType) -> Unit = { page = if (it == EntryType.INCOME) "income" else "expense" }
            val openForm: (String?) -> Unit = { invoiceId = it; page = "invoiceForm" }
            FeatureTheme {
            Scaffold(containerColor = androidx.compose.ui.graphics.Color.Black, bottomBar = {
                if (page in listOf("dashboard", "goals", "recurring")) AppBottomNavBar(tab,
                    onTabSelected = { when (it) {
                        AppNavTab.GOALS -> { goals.createGoal(); page = "goals" }
                        AppNavTab.RECURRING -> page = "recurring"
                        AppNavTab.ADD -> page = "expense"
                        AppNavTab.DASHBOARD -> page = "dashboard"
                        AppNavTab.SPENDING -> Unit // excluded feature is never previewed or replaced
                    } }, onAddClick = { page = "expense" })
            }) { padding ->
                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                    CompositionLocalProvider(LocalFeatureInsets provides padding) {
                        when (page) {
                            "dashboard" -> DashboardRoute(dashboard, { page = "account" },
                                { goals.createGoal(); page = "goals" }, { page = "invoices" },
                                accountLabel = "Local UI review", goalsViewModel = previewGoals, invoicesViewModel = invoices)
                            "expense" -> AddExpenseRoute({ page = "dashboard" }, selectType, { page = "account" }, expense)
                            "income" -> AddIncomeRoute({ page = "dashboard" }, selectType,
                                { page = "invoices" }, { openForm(null) }, { page = "account" }, income)
                            "goals" -> SavingsGoalsRoute({ page = "dashboard" }, goals)
                            "invoices" -> InvoicesScreen({ page = "income" }, { openForm(null) }, { openForm(it) }, invoices)
                            "invoiceForm" -> {
                                val form = model("invoice-form:${invoiceId ?: "new"}", InvoiceViewModel::class.java) {
                                    InvoiceViewModel(invoiceRepository, SavedStateHandle())
                                }
                                NewInvoiceScreen(onBack = { page = "invoices" }, onSaved = { page = "invoices" },
                                    invoiceId = invoiceId, viewModel = form)
                            }
                            "recurring" -> RecurringRoute({ page = "dashboard" }, recurring)
                            "account" -> AccountScreen(intent.getStringExtra("accountLabel") ?: "Local UI review", { page = "dashboard" }, { finish() })
                        }
                    }
                }
            }
            }
        }
    }
}

/** Only reads records actually saved through the real forms into the separate review Room database. */
private class ReviewData(private val db: AppDatabase, private val owner: String) : FirestoreRepository {
    override suspend fun <T : UserFirestoreDocument> create(collection: FirestoreCollection<T>, document: T) = FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED)
    override suspend fun <T : UserFirestoreDocument> update(collection: FirestoreCollection<T>, document: T) = FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED)
    override suspend fun <T : UserFirestoreDocument> get(collection: FirestoreCollection<T>, documentId: String): FirestoreResult<T?> = FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED)
    override suspend fun <T : UserFirestoreDocument> softDelete(collection: FirestoreCollection<T>, documentId: String, updatedAt: Long) = FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED)
    override fun <T : UserFirestoreDocument> observeAll(collection: FirestoreCollection<T>): Flow<FirestoreResult<List<T>>> = flowOf(FirestoreResult.Success(emptyList()))
    override fun <T : UserFirestoreDocument> observeMonthly(collection: FirestoreCollection<T>, startEpochDay: Long, endEpochDayExclusive: Long): Flow<FirestoreResult<List<T>>> {
        if (collection.name !in listOf("incomes", "expenses")) return observeAll(collection)
        return db.invalidationTracker.createFlow(collection.name).map {
            withContext(Dispatchers.IO) {
                val rows = mutableListOf<T>()
                db.openHelper.readableDatabase.query("SELECT * FROM ${collection.name} WHERE userId = ? AND isDeleted = 0 AND dateEpochDay >= ? AND dateEpochDay < ?",
                    arrayOf<Any>(owner, startEpochDay, endEpochDayExclusive)).use { cursor ->
                    fun text(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name)).orEmpty()
                    fun number(name: String) = cursor.getLong(cursor.getColumnIndexOrThrow(name))
                    while (cursor.moveToNext()) {
                        val document: UserFirestoreDocument = if (collection.name == "incomes") IncomeFirestoreDto(id = text("id"), userId = owner,
                            source = text("source"), lkrAmountMinor = number("lkrAmountMinor"), dateEpochDay = number("dateEpochDay"),
                            note = text("note"), updatedAt = number("updatedAt"))
                        else ExpenseFirestoreDto(id = text("id"), userId = owner, amountMinor = number("amountMinor"),
                            categoryId = text("categoryId"), channel = text("channel"), dateEpochDay = number("dateEpochDay"),
                            note = text("note"), updatedAt = number("updatedAt"))
                        rows.add(requireNotNull(collection.documentType.cast(document)))
                    }
                }
                FirestoreResult.Success(rows)
            }
        }
    }
}
