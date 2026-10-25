package com.example.veromobile.ui.auth

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.MainActivity
import com.example.veromobile.ui.theme.VeroMobileTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures only: no credentials, Firebase calls or database setup. */
class AuthPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationValidationPasswordVisibilityAndKeyboardRemainUsable() {
        var state by mutableStateOf(AuthUiState(isResolvingSession = false))
        compose.runOnUiThread {
            compose.activity.setContent { VeroMobileTheme {
                AuthScreen(state,
                    onEmailChanged = { state = state.copy(email = it, emailError = null) },
                    onPasswordChanged = { state = state.copy(password = it) },
                    onConfirmPasswordChanged = { state = state.copy(confirmPassword = it) },
                    onSignIn = { state = state.copy(emailError = "Enter a valid email") },
                    onRegister = {},
                    onResetPassword = { state = state.copy(message = "If an account exists, a reset link will be sent.") },
                    onShowLogin = { state = state.copy(screen = AuthScreen.LOGIN, password = "", confirmPassword = "", emailError = null) },
                    onShowRegister = { state = state.copy(screen = AuthScreen.REGISTER, password = "", confirmPassword = "", emailError = null) },
                    onShowForgotPassword = { state = state.copy(screen = AuthScreen.FORGOT_PASSWORD, password = "", confirmPassword = "", emailError = null) })
            } }
        }
        compose.onNodeWithContentDescription("Vero logo").assertExists()
        compose.onNodeWithText("vero mobile").assertDoesNotExist()
        capture("login")
        compose.onNodeWithText("Sign in", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid email").assertExists()
        capture("login-validation")
        compose.onNodeWithContentDescription("Password", substring = false).performScrollTo().performClick().performTextInput("test1234")
        waitForKeyboard()
        compose.onNodeWithContentDescription("Show password").performClick()
        compose.onNodeWithContentDescription("Hide password").assertExists()
        compose.onNodeWithText("Sign in", substring = false).performScrollTo().assertIsDisplayed()
        capture("login-keyboard")
        closeSoftKeyboard()
        compose.onNodeWithText("Create account", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Create your account").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Vero logo").assertExists()
        capture("create-account")
        compose.onNodeWithContentDescription("Confirm password", substring = false).performScrollTo().performClick().performTextInput("test1234")
        waitForKeyboard()
        compose.onNodeWithText("Create account", substring = false).performScrollTo().assertIsDisplayed()
        capture("create-account-keyboard")
        closeSoftKeyboard()
        compose.onNodeWithContentDescription("Back to sign in").performScrollTo().performClick()
        compose.onNodeWithText("Forgot password?").performScrollTo().performClick()
        compose.onNodeWithText("Reset your password").performScrollTo().assertIsDisplayed()
        capture("forgot-password")
        compose.onNodeWithText("Send reset link").performScrollTo().performClick()
        compose.onNodeWithText("If an account exists, a reset link will be sent.").assertExists()
        capture("reset-feedback")
        compose.runOnIdle { state = state.copy(isSubmitting = true) }
        compose.onNodeWithText("Please wait...").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Email", substring = false).assertIsNotEnabled()
        compose.onNodeWithText("Continue with Google").assertDoesNotExist()
        capture("submitting")
    }

    private fun waitForKeyboard() {
        compose.waitUntil(10_000) {
            val insets = compose.activity.window.decorView.rootWindowInsets
            insets != null && WindowInsetsCompat.toWindowInsetsCompat(insets).isVisible(WindowInsetsCompat.Type.ime())
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "auth-review").apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try { File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
}
