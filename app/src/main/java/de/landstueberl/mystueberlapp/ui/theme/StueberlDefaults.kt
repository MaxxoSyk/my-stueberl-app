package de.landstueberl.mystueberlapp.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

/** Shared component colors, derived from the theme so they work in light and dark mode. */
object StueberlDefaults {

    @Composable
    fun textFieldColors() = with(MaterialTheme.colorScheme) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = surface,
            unfocusedContainerColor = surface,
            // Set explicitly: the old fields relied on the default, which is
            // white in dark mode and unreadable on a light container.
            focusedTextColor = onSurface,
            unfocusedTextColor = onSurface,
            focusedBorderColor = primary,
            unfocusedBorderColor = outline,
            focusedLabelColor = primary,
            unfocusedLabelColor = onSurfaceVariant,
            cursorColor = primary
        )
    }

    /**
     * The disabled state uses a faded primary instead of primaryContainer:
     * in dark mode primaryContainer is lighter than primary, so a disabled
     * button would look more prominent than an enabled one.
     */
    @Composable
    fun primaryButtonColors() = with(MaterialTheme.colorScheme) {
        ButtonDefaults.buttonColors(
            containerColor = primary,
            contentColor = onPrimary,
            disabledContainerColor = primary.copy(alpha = 0.38f),
            disabledContentColor = onSurfaceVariant
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun topAppBarColors() = with(MaterialTheme.colorScheme) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = primary,
            titleContentColor = onPrimary,
            navigationIconContentColor = onPrimary,
            actionIconContentColor = onPrimary
        )
    }

    /** Like [primaryButtonColors], for actions that remove or discard something. */
    @Composable
    fun destructiveButtonColors() = with(MaterialTheme.colorScheme) {
        ButtonDefaults.buttonColors(
            containerColor = error,
            contentColor = onError,
            disabledContainerColor = error.copy(alpha = 0.38f),
            disabledContentColor = onSurfaceVariant
        )
    }
}