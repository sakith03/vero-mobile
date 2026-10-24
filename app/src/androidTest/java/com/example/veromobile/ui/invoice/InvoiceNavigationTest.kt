package com.example.veromobile.ui.invoice

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.veromobile.MainActivity
import org.junit.Rule
import org.junit.Test

class InvoiceNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun existingFreelanceButtonsOpenFormAndListAndReturnToIncome() {
        compose.onNodeWithText("Income", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Freelance").performClick()
        compose.onNodeWithText("New invoice").performScrollTo().performClick()
        compose.onNodeWithText("Client").assertExists()
        compose.onNodeWithText("Save invoice").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Open invoices").performScrollTo().performClick()
        compose.onNodeWithText("Invoices").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Open invoices").assertExists()
    }
}
