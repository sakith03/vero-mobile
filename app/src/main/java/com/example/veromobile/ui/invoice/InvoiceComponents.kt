package com.example.veromobile.ui.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.example.veromobile.R
import com.example.veromobile.ui.components.FeatureScreen
import com.example.veromobile.ui.components.featureFieldColors
import java.time.*

internal val InvoiceAccent = Color(0xFFE3380D)
internal val InvoiceBorder = Color(0xFF1E1E1E)
@Composable
internal fun InvoiceTheme(content: @Composable () -> Unit) {
    val font = FontFamily(Font(R.font.goal_poppins_regular), Font(R.font.goal_poppins_semibold, FontWeight.SemiBold))
    fun text(size: Int, bold: Boolean = false) = TextStyle(fontFamily = font, fontSize = size.sp,
        lineHeight = (size * 1.5f).sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    FeatureScreen {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = InvoiceAccent, onPrimary = Color.White,
        secondary = InvoiceAccent, onSecondary = Color.White, secondaryContainer = InvoiceAccent.copy(alpha = .2f),
        onSecondaryContainer = Color(0xFFFFB59E), surfaceTint = InvoiceAccent,
        surfaceContainer = Color(0xFF101010), surfaceContainerHigh = Color(0xFF101010),
        surfaceContainerLow = Color(0xFF0E0E0E), surfaceContainerHighest = Color(0xFF1E1E1E),
        background = Color.Black, onBackground = Color.White, surface = Color(0xFF101010), onSurface = Color.White,
        surfaceVariant = Color(0xFF0E0E0E), onSurfaceVariant = Color(0xFF7A7A7A), outline = InvoiceBorder,
        error = Color(0xFFFF8A6B)), typography = MaterialTheme.typography.copy(bodyLarge = text(16), bodyMedium = text(14),
        bodySmall = text(12), titleLarge = text(24, true), titleMedium = text(18, true), titleSmall = text(15, true),
        labelLarge = text(13, true), labelMedium = text(12), labelSmall = text(10, true)), content = content)
    }
}
@Composable
internal fun InvoiceHeader(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        action()
    }
}
@Composable
internal fun InvoiceField(label: String, value: String, change: (String) -> Unit,
                          error: String? = null, money: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, singleLine = true, shape = RoundedCornerShape(16.dp), isError = error != null,
        supportingText = error?.let { { Text(it) } }, colors = featureFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = if (money) KeyboardType.Decimal else KeyboardType.Text))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InvoiceDateField(label: String, value: String, change: (String) -> Unit, error: String?, enabled: Boolean) {
    var picker by remember { mutableStateOf(false) }
    OutlinedTextField(value, change, label = { Text(label) }, placeholder = { Text("YYYY-MM-DD") },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
        shape = RoundedCornerShape(16.dp), isError = error != null, colors = featureFieldColors(),
        supportingText = error?.let { { Text(it) } }, trailingIcon = {
            IconButton({ picker = true }, enabled = enabled) { Icon(Icons.Outlined.DateRange, "Choose $label") }
        })
    if (picker) {
        val day = com.example.veromobile.domain.calc.InvoiceRules.date(value) ?: com.example.veromobile.core.AppTime.today()
        val state = rememberDatePickerState(initialSelectedDateMillis =
            day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1900..9999)
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
            TextButton({ state.selectedDateMillis?.let { change(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; picker = false }) { Text("OK") }
        }, dismissButton = { TextButton({ picker = false }) { Text("Cancel") } }) { DatePicker(state) }
    }
}
