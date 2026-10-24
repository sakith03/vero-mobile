package com.example.veromobile.data.firestore

data class FirestoreCollection<T : UserFirestoreDocument>(
    val name: String,
    val documentType: Class<T>,
    val monthlyDateField: String? = null
)

object FirestoreCollections {
    val incomes = FirestoreCollection("incomes", IncomeFirestoreDto::class.java, "dateEpochDay")
    val expenses = FirestoreCollection("expenses", ExpenseFirestoreDto::class.java, "dateEpochDay")
    val categories = FirestoreCollection("categories", CategoryFirestoreDto::class.java)
    val invoices = FirestoreCollection("invoices", InvoiceFirestoreDto::class.java, "dueDateEpochDay")
    val cryptoTrades = FirestoreCollection("cryptoTrades", CryptoTradeFirestoreDto::class.java, "closedDateEpochDay")
    val recurringRules = FirestoreCollection("recurringRules", RecurringRuleFirestoreDto::class.java, "nextDueDateEpochDay")
    val savingsGoals = FirestoreCollection("savingsGoals", SavingsGoalFirestoreDto::class.java, "targetDateEpochDay")
    val goalContributions = FirestoreCollection(
        "goalContributions",
        GoalContributionFirestoreDto::class.java,
        "dateEpochDay"
    )
}
