package com.example.veromobile.ui.auth

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veromobile.R
import androidx.core.view.WindowCompat

/** Scoped to authentication: teammate screens keep their existing theme. */
@Composable
private fun AuthTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull()
    val bars = remember(activity, view) { activity?.let { WindowCompat.getInsetsController(it.window, view) } }
    DisposableEffect(bars) {
        val lightStatus = bars?.isAppearanceLightStatusBars
        val lightNavigation = bars?.isAppearanceLightNavigationBars
        onDispose {
            lightStatus?.let { bars?.isAppearanceLightStatusBars = it }
            lightNavigation?.let { bars?.isAppearanceLightNavigationBars = it }
        }
    }
    SideEffect {
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
    }
    val font = remember { FontFamily(Font(R.font.goal_poppins_regular),
        Font(R.font.goal_poppins_semibold, FontWeight.SemiBold)) }
    fun style(size: Int, bold: Boolean = false) = TextStyle(fontFamily = font,
        fontSize = size.sp, lineHeight = (size * 1.5f).sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(primary = Color(0xFFE3380D), onPrimary = Color.White,
            background = Color.Black, onBackground = Color.White, surface = Color(0xFF101010),
            onSurface = Color.White, surfaceVariant = Color(0xFF121212), onSurfaceVariant = Color(0xFF9A9A9A),
            outline = Color(0xFF2A2A2A), error = Color(0xFFFF7A5C)),
        typography = MaterialTheme.typography.copy(headlineMedium = style(26, true), titleLarge = style(26, true),
            bodyLarge = style(16), bodyMedium = style(14), bodySmall = style(12),
            labelLarge = style(16, true), labelMedium = style(12)), content = content)
}

@Composable
fun AuthLoadingScreen() = AuthTheme {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onResetPassword: () -> Unit,
    onShowLogin: () -> Unit,
    onShowRegister: () -> Unit,
    onShowForgotPassword: () -> Unit
) = AuthTheme {
    BackHandler(enabled = state.screen != AuthScreen.LOGIN && !state.isSubmitting, onBack = onShowLogin)
    BackHandler(enabled = state.isSubmitting) { }
    Scaffold(containerColor = Color.Transparent, contentColor = Color.White,
        modifier = Modifier.authBackground()) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            val viewportHeight = maxHeight
            val compact = viewportHeight < 700.dp
            Column(Modifier.widthIn(max = 448.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight).padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (state.screen != AuthScreen.LOGIN) {
                    IconButton(onShowLogin, enabled = !state.isSubmitting,
                        modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = .06f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to sign in")
                    }
                } else Spacer(Modifier.height(if (compact) 16.dp else 48.dp))
                Box(Modifier.fillMaxWidth().height(if (compact) 80.dp else 96.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.vero_logo), contentDescription = "Vero logo",
                        modifier = Modifier.size(if (compact) 80.dp else 96.dp))
                }
                Spacer(Modifier.height(if (compact) 16.dp else 20.dp))
                Text(when (state.screen) {
                    AuthScreen.LOGIN -> "Welcome back"
                    AuthScreen.REGISTER -> "Create your account"
                    AuthScreen.FORGOT_PASSWORD -> "Reset your password"
                }, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(if (state.screen == AuthScreen.FORGOT_PASSWORD) "Enter your email to receive a reset link."
                    else "Every rupee, every source, one goal.", Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(if (compact) 24.dp else 32.dp))
                EmailField(state, onEmailChanged)
                if (state.screen != AuthScreen.FORGOT_PASSWORD) {
                    Spacer(Modifier.height(16.dp))
                    PasswordField("Password", state.password, state.passwordError, state.isSubmitting,
                        onPasswordChanged, if (state.screen == AuthScreen.LOGIN) onSignIn else null)
                }
                if (state.screen == AuthScreen.REGISTER) {
                    Spacer(Modifier.height(16.dp))
                    PasswordField("Confirm password", state.confirmPassword, state.confirmPasswordError,
                        state.isSubmitting, onConfirmPasswordChanged, onRegister)
                }
                if (state.screen == AuthScreen.LOGIN) {
                    TextButton(onShowForgotPassword, enabled = !state.isSubmitting,
                        modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF47422))) {
                        Text("Forgot password?", style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.message?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall,
                        color = if (state.screen == AuthScreen.FORGOT_PASSWORD && message.startsWith("If an account"))
                            Color(0xFF6EE7A0) else MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.weight(1f))
                SubmitButton(when (state.screen) {
                    AuthScreen.LOGIN -> "Sign in"
                    AuthScreen.REGISTER -> "Create account"
                    AuthScreen.FORGOT_PASSWORD -> "Send reset link"
                }, state.isSubmitting, when (state.screen) {
                    AuthScreen.LOGIN -> onSignIn
                    AuthScreen.REGISTER -> onRegister
                    AuthScreen.FORGOT_PASSWORD -> onResetPassword
                })
                Spacer(Modifier.height(12.dp))
                if (state.screen == AuthScreen.LOGIN) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("New here?", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onShowRegister, enabled = !state.isSubmitting, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Create account", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                } else {
                    TextButton(onShowLogin, enabled = !state.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
                        Text(if (state.screen == AuthScreen.REGISTER) "Already have an account? Sign in" else "Back to sign in",
                            style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun Modifier.authBackground() = background(Color.Black).drawWithCache {
    val bottomGlow = Brush.radialGradient(listOf(Color(0xFF541100).copy(alpha = .7f), Color.Transparent),
        center = Offset(size.width * .4f, size.height), radius = size.width * 1.2f)
    val topGlow = Brush.radialGradient(listOf(Color(0xFF541100).copy(alpha = .2f), Color.Transparent),
        center = Offset(size.width * .5f, 0f), radius = size.width * .8f)
    onDrawBehind { drawRect(topGlow); drawRect(bottomGlow) }
}

@Composable
private fun EmailField(state: AuthUiState, onEmailChanged: (String) -> Unit) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Email", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(state.email, onEmailChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email" },
            enabled = !state.isSubmitting, singleLine = true, shape = RoundedCornerShape(16.dp),
            placeholder = { Text("you@example.com") }, colors = authFieldColors(),
            isError = state.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                imeAction = if (state.screen == AuthScreen.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PasswordField(label: String, value: String, error: String?, loading: Boolean,
                          onValueChanged: (String) -> Unit, onDone: (() -> Unit)? = null) {
    var visible by rememberSaveable(label) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value, onValueChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            enabled = !loading, singleLine = true, shape = RoundedCornerShape(16.dp), colors = authFieldColors(),
            placeholder = { Text(if (label == "Confirm password") "Re-enter your password" else "Enter your password") },
            isError = error != null,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                imeAction = if (onDone == null) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) },
                onDone = { if (!loading) onDone?.invoke() }),
            trailingIcon = {
                IconButton({ visible = !visible }, enabled = !loading) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        if (visible) "Hide password" else "Show password")
                }
            })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF121212), unfocusedContainerColor = Color(0xFF121212),
    disabledContainerColor = Color(0xFF121212), errorContainerColor = Color(0xFF121212),
    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFF2A2A2A))

@Composable
private fun SubmitButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = !loading, shape = CircleShape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
        }
        Text(if (loading) "Please wait..." else label)
    }
}
