package de.landstueberl.mystueberlapp.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.util.toLocalDateFromPicker
import de.landstueberl.mystueberlapp.util.toPickerMillis
import java.time.LocalDate

/**
 * The app's date picker dialog.
 *
 * Only dates within [minDate]..[maxDate] can be picked; either bound may be
 * null for "no limit". The picker opens on [initialDate], or today when null,
 * moved into the allowed range if necessary.
 *
 * The caller closes the dialog: [onConfirm] is not followed by [onDismiss].
 *
 * @param title optional header above the calendar; [hint] is shown below it
 * @param isDestructive red accent and confirm button, e.g. for "removed on"
 */
@Composable
fun StueberlDatePickerDialog(
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    title: String? = null,
    hint: String? = null,
    confirmLabel: String = stringResource(R.string.common_ok),
    isDestructive: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (isDestructive) colorScheme.error else colorScheme.primary
    val onAccent = if (isDestructive) colorScheme.onError else colorScheme.onPrimary

    val startDate = remember(initialDate, minDate, maxDate) {
        (initialDate ?: LocalDate.now()).clampTo(minDate, maxDate)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.toPickerMillis(),
        yearRange = yearRangeOf(minDate, maxDate),
        selectableDates = remember(minDate, maxDate) { DateRange(minDate, maxDate) }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        colors = DatePickerDefaults.colors(containerColor = colorScheme.surface),
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.toLocalDateFromPicker()
                        ?.let(onConfirm)
                },
                enabled = datePickerState.selectedDateMillis != null,
                shape = RoundedCornerShape(10.dp),
                colors = if (isDestructive) {
                    StueberlDefaults.destructiveButtonColors()
                } else {
                    StueberlDefaults.primaryButtonColors()
                }
            ) {
                Text(text = confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    ) {
        Column {
            if (title != null) {
                DatePickerHeader(title = title, hint = hint, accent = accent)
            }

            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = pickerColors(accent = accent, onAccent = onAccent)
            )
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────
@Composable
private fun DatePickerHeader(title: String, hint: String?, accent: Color) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (hint != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    )
}

// ── Colors ─────────────────────────────────────────────────────────────
@Composable
private fun pickerColors(accent: Color, onAccent: Color): DatePickerColors =
    with(MaterialTheme.colorScheme) {
        DatePickerDefaults.colors(
            containerColor = surface,
            selectedDayContainerColor = accent,
            selectedDayContentColor = onAccent,
            todayDateBorderColor = accent,
            todayContentColor = accent,
            dayContentColor = onSurface,
            disabledDayContentColor = onSurfaceVariant.copy(alpha = 0.3f),
            weekdayContentColor = onSurfaceVariant,
            subheadContentColor = onSurfaceVariant,
            navigationContentColor = secondary,
            yearContentColor = onSurface,
            selectedYearContainerColor = accent,
            selectedYearContentColor = onAccent,
            currentYearContentColor = accent
        )
    }

// ── Range ──────────────────────────────────────────────────────────────
private class DateRange(
    private val minDate: LocalDate?,
    private val maxDate: LocalDate?
) : SelectableDates {

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = utcTimeMillis.toLocalDateFromPicker()
        return (minDate == null || !date.isBefore(minDate)) &&
                (maxDate == null || !date.isAfter(maxDate))
    }

    override fun isSelectableYear(year: Int): Boolean =
        (minDate == null || year >= minDate.year) &&
                (maxDate == null || year <= maxDate.year)
}

/**
 * Years offered in the year list. Never empty: if [minDate] lies after
 * [maxDate] (e.g. the device clock was changed), it falls back to [minDate]'s year.
 */
private fun yearRangeOf(minDate: LocalDate?, maxDate: LocalDate?): IntRange {
    val first = minDate?.year ?: DatePickerDefaults.YearRange.first
    val last = maxDate?.year ?: DatePickerDefaults.YearRange.last
    return first..maxOf(first, last)
}

/**
 * Moves the date into [min]..[max]. The min bound wins if the range is empty,
 * matching the old product dialog's behavior.
 */
private fun LocalDate.clampTo(min: LocalDate?, max: LocalDate?): LocalDate = when {
    min != null && isBefore(min) -> min
    max != null && isAfter(max) -> max
    else -> this
}