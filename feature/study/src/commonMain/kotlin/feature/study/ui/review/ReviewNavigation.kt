@file:OptIn(ExperimentalMaterial3Api::class)

package feature.study.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.browse_your_words
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.exit_review
import lexicon.resources.generated.resources.exit_review_message
import lexicon.resources.generated.resources.next
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Browse-mode navigation buttons. Back is outlined, Forward is filled.
 */
@Composable
fun NavigationButtons(
    currentIndex: Int,
    totalCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
    ) {
        Text(
            text = stringResource(Res.string.browse_your_words),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f).height(Theme.dimensions.buttonHeight)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                    Spacer(Modifier.width(Theme.spacing.extraSmall2))
                    Text(stringResource(Res.string.back), fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onNavigateForward,
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier.weight(1f).height(Theme.dimensions.buttonHeight)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.next), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(Theme.spacing.extraSmall2))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.next),
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExitConfirmationBottomSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Theme.elevation.none,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        ExitConfirmationContent(
            onConfirm = {
                coroutineScope.launch {
                    sheetState.hide()
                    onConfirm()
                }
            },
            onCancel = onDismiss
        )
    }
}

@Composable
private fun ExitConfirmationContent(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Theme.dimensions.iconSizeXLarge)
        )

        Text(
            text = stringResource(Res.string.exit_review),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(Res.string.exit_review_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = stringResource(Res.string.exit_review),
                style = MaterialTheme.typography.labelLarge
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = stringResource(Res.string.cancel),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
