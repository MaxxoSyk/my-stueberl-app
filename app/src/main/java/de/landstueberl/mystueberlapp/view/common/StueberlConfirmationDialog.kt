package de.landstueberl.mystueberlapp.view.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults

/**
 * Asks the user to confirm an action.
 *
 * Styled to match the app's date pickers: 20dp corners, filled confirm button,
 * muted dismiss. Set [isDestructive] for actions that lose data — the confirm
 * button then uses the error colour. Theme-based, so it works in dark mode.
 */
@Composable
fun StueberlConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    dismissLabel: String = stringResource(R.string.common_cancel)
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colorScheme.surface,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = if (isDestructive) {
                    StueberlDefaults.destructiveButtonColors()
                } else {
                    StueberlDefaults.primaryButtonColors()
                }
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Text(dismissLabel)
            }
        }
    )
}