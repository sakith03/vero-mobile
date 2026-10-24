package com.example.veromobile.ui.goals

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veromobile.R
import com.example.veromobile.domain.calc.GoalValidator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal val GoalOrange = Color(0xFFF47422)
internal val GoalBorder = Color(0xFF1E1E1E)

@Composable
internal fun GoalsTheme(content: @Composable () -> Unit) {
    val font = FontFamily(Font(R.font.goal_poppins_regular), Font(R.font.goal_poppins_semibold, FontWeight.SemiBold))
    fun style(size: Int, bold: Boolean = false) = TextStyle(fontFamily = font, fontSize = size.sp,
        lineHeight = (size * 1.5f).sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Color(0xFFE8380D), onPrimary = Color.White,
            background = Color.Black, onBackground = Color.White, surface = Color(0xFF101010),
            onSurface = Color.White, surfaceVariant = Color(0xFF121212), onSurfaceVariant = Color(0xFF7A7A7A),
            outline = Color(0xFF2A2A2A), error = Color(0xFFFF7A5C)),
        typography = Typography(bodyLarge = style(16), bodyMedium = style(14), bodySmall = style(12),
            titleLarge = style(20, true), titleMedium = style(16, true), titleSmall = style(14, true),
            headlineLarge = style(36, true), headlineMedium = style(26, true), headlineSmall = style(24, true),
            labelLarge = style(16, true), labelMedium = style(13), labelSmall = style(11)),
        content = content
    )
}

internal fun Modifier.goalBackground() = background(Color.Black).drawWithCache {
    val glow = Brush.radialGradient(0f to Color(0xFFE3380D).copy(alpha = .45f),
        .35f to Color(0xFF721C07).copy(alpha = .225f), .7f to Color.Transparent,
        center = Offset(size.width, 0f), radius = size.height.coerceAtLeast(1f))
    onDrawBehind {
        scale(.46f, 1f, pivot = Offset(size.width, 0f)) {
            drawCircle(glow, radius = size.height, center = Offset(size.width, 0f))
        }
    }
}

@Composable
internal fun GoalHeader(title: String, onBack: () -> Unit, action: String? = null,
                        onAction: () -> Unit = {}, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onBack, enabled = enabled,
            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = .08f), CircleShape)) {
            Image(painterResource(R.drawable.goal_back), contentDescription = "Back", modifier = Modifier.size(18.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action == null) Spacer(Modifier.size(48.dp)) else {
            TextButton(onClick = onAction, enabled = enabled, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = .08f), CircleShape)) {
                Text(action, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
internal fun GoalPrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, saving: Boolean = false) {
    Button(onClick = onClick, enabled = enabled && !saving, shape = CircleShape,
        modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(label)
    }
}

@Composable
internal fun GoalTextField(label: String, value: String, onChange: (String) -> Unit,
                          error: String? = null, enabled: Boolean = true, amount: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
        shape = RoundedCornerShape(16.dp), isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (amount) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalDateField(label: String, value: String, onChange: (String) -> Unit, today: LocalDate,
                          error: String? = null, enabled: Boolean = true, allowFuture: Boolean = true) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") }, singleLine = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = { IconButton(onClick = { showPicker = true }, enabled = enabled) {
            Icon(Icons.Outlined.DateRange, "Choose $label")
        } }, colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant))
    if (showPicker) {
        val date = parseGoalDate(value)?.takeIf { GoalValidator.isSupportedDate(it) } ?: today
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1900..9999, selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return GoalValidator.isSupportedDate(day) && (allowFuture || day <= today)
                }
            })
        DatePickerDialog(onDismissRequest = { showPicker = false },
            confirmButton = { TextButton(onClick = {
                picker.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                showPicker = false
            }, enabled = picker.selectedDateMillis != null) { Text("Choose date") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }) {
            DatePicker(state = picker)
        }
    }
}

@Composable
internal fun GoalErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
