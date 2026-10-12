package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormViewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ── Shared Product Image ───────────────────────────────────────────────
@Composable
fun ProductImagePlaceholder(
    imageFile: File?,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SageGreenLight)
            .border(
                width = 2.dp,
                color = SageGreen,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                color = SageGreenDark,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )

            imageFile != null -> {
                AsyncImage(
                    model = imageFile,
                    contentDescription = stringResource(R.string.product_form_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Small edit affordance so it's clear the image is tappable
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.product_form_image_placeholder),
                    fontSize = 11.sp,
                    color = SageGreenDark
                )
            }
        }
    }
}

// ── Shared Description Field ───────────────────────────────────────────
@Composable
fun ProductDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(stringResource(R.string.product_form_description))
        },
        placeholder = {
            Text(
                stringResource(R.string.product_form_description_placeholder),
                color = TextSecondary
            )
        },
        modifier = modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SageGreen,
            unfocusedBorderColor = TextSecondary,
            focusedLabelColor = SageGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = SageGreen
        )
    )
}

private val CURRENCY_SYMBOL: String = ProductFormViewModel.CURRENCY.symbol

// ── Shared Price Input Field ───────────────────────────────────────────
@Composable
fun ProductPriceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        suffix = {
            Text(
                text = CURRENCY_SYMBOL,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SageGreen,
            unfocusedBorderColor = TextSecondary,
            focusedLabelColor = SageGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = SageGreen
        ),
        singleLine = true
    )
}

// ── Read Only Date Field ───────────────────────────────────────────────
@Composable
fun ProductReadOnlyDateField(
    label: String,
    date: LocalDate?,
    modifier: Modifier = Modifier
) {
    val formattedDate = date?.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    ) ?: "-"

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = TextSecondary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

// ── Section Divider ────────────────────────────────────────────────────
@Composable
fun ProductFormDivider(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SageGreen.copy(alpha = 0.3f)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SageGreen.copy(alpha = 0.3f)
        )
    }
}

// ── Product Status Badge ───────────────────────────────────────────────
@Composable
fun ProductStatusBadge(
    status: ProductStatus,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        ProductStatus.AVAILABLE -> Icons.Default.RadioButtonUnchecked
        ProductStatus.SOLD -> Icons.Default.CheckCircle
        ProductStatus.REMOVED -> Icons.Default.Cancel
    }

    val tint = when (status) {
        ProductStatus.AVAILABLE -> TextSecondary
        ProductStatus.SOLD -> SageGreen
        ProductStatus.REMOVED -> MaterialTheme.colorScheme.error
    }

    val label = when (status) {
        ProductStatus.AVAILABLE -> stringResource(R.string.product_status_badge_available)
        ProductStatus.SOLD -> stringResource(R.string.product_status_badge_sold)
        ProductStatus.REMOVED -> stringResource(R.string.product_status_badge_removed)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = tint.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}