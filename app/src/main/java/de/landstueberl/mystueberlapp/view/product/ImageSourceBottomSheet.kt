package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceBottomSheet(
    canRemove: Boolean,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    /** Animates the sheet away, then runs [action]. */
    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.image_source_sheet_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = SageGreen.copy(alpha = 0.3f)
            )

            ImageSourceItem(
                icon = Icons.Default.PhotoCamera,
                iconTint = SageGreenDark,
                label = stringResource(R.string.image_source_sheet_camera),
                onClick = { dismissThen(onTakePhoto) }
            )

            ImageSourceItem(
                icon = Icons.Default.PhotoLibrary,
                iconTint = SageGreenDark,
                label = stringResource(R.string.image_source_sheet_gallery),
                onClick = { dismissThen(onPickFromGallery) }
            )

            if (canRemove) {
                ImageSourceItem(
                    icon = Icons.Default.Delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    label = stringResource(R.string.image_source_sheet_remove),
                    onClick = { dismissThen(onRemovePhoto) }
                )
            }
        }
    }
}

@Composable
private fun ImageSourceItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}