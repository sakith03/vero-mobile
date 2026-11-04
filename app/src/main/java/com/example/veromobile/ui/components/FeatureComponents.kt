package com.example.veromobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.example.veromobile.R

val FeatureOrange = Color(0xFFE3380D)
val FeatureWarm = Color(0xFFF47422)
val FeatureBorder = Color(0xFF1E1E1E)
val LocalFeatureInsets = staticCompositionLocalOf { PaddingValues(0.dp) }

/** Used only by the included feature screens. The app-wide and accepted auth themes stay unchanged. */
@Composable
fun FeatureTheme(content: @Composable () -> Unit) {
    val font = remember { FontFamily(Font(R.font.goal_poppins_regular), Font(R.font.goal_poppins_semibold, FontWeight.SemiBold)) }
    fun style(size: Int, bold: Boolean = false) = TextStyle(fontFamily = font, fontSize = size.sp,
        lineHeight = (size * 1.5f).sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    MaterialTheme(colorScheme = darkColorScheme(primary = FeatureOrange, onPrimary = Color.White,
        primaryContainer = Color(0xFF391409), onPrimaryContainer = Color.White,
        secondary = FeatureWarm, onSecondary = Color.Black, secondaryContainer = Color(0xFF301A0C),
        onSecondaryContainer = Color(0xFFFFC49B), tertiary = FeatureWarm, onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF301A0C), onTertiaryContainer = Color.White,
        background = Color.Black, onBackground = Color.White, surface = Color(0xFF101010), onSurface = Color.White,
        surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0E0E0E), surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF121212), surfaceContainerHighest = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF121212), onSurfaceVariant = Color(0xFF9A9A9A), surfaceTint = FeatureOrange,
        outline = Color(0xFF2A2A2A), outlineVariant = FeatureBorder, error = Color(0xFFFF7A5C), onError = Color.Black,
        errorContainer = Color(0xFF391409), onErrorContainer = Color(0xFFFFB59E)),
        typography = Typography(displayLarge = style(44, true), displayMedium = style(40, true), displaySmall = style(32, true),
            headlineLarge = style(32, true), headlineMedium = style(26, true), headlineSmall = style(24, true),
            titleLarge = style(20, true), titleMedium = style(16, true), titleSmall = style(14, true),
            bodyLarge = style(16), bodyMedium = style(14), bodySmall = style(12),
            labelLarge = style(14, true), labelMedium = style(12), labelSmall = style(10, true)),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = { CompositionLocalProvider(LocalContentColor provides Color.White, content = content) })
}

/** Consume the measured navigation inset only inside included screens, never around excluded routes. */
@Composable
fun FeatureScreen(content: @Composable () -> Unit) = FeatureTheme {
    val insets = LocalFeatureInsets.current
    Box(Modifier.fillMaxSize().padding(bottom = insets.calculateBottomPadding()).consumeWindowInsets(insets)) {
        CompositionLocalProvider(LocalFeatureInsets provides PaddingValues(0.dp), content = content)
    }
}

@Composable
fun FeaturePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FeatureBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun FeatureState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) = FeaturePanel {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (action != null) OutlinedButton(onAction, Modifier.heightIn(min = 48.dp)) { Text(action) }
}

@Composable
fun FeatureAmount(value: String, modifier: Modifier = Modifier, large: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface, maxFontSize: TextUnit = if (large) 40.sp else 20.sp) {
    BasicText(value, modifier.fillMaxWidth(), maxLines = 1,
        style = (if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge)
            .copy(color = color, textAlign = if (large) TextAlign.Center else TextAlign.Start),
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxFontSize))
}

@Composable
fun featureFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant)
