package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.accept
import lexicon.resources.generated.resources.deny
import lexicon.resources.generated.resources.starter_vocabulary
import lexicon.resources.generated.resources.words
import org.jetbrains.compose.resources.stringResource
import presentation.model.VocabularyPreviewUiState
import theme.Theme

private val ButtonContentPaddingVertical = 14.dp
private val ButtonContentPaddingHorizontal = 24.dp

@Composable
fun VocabularyPreviewScreen(
    state: VocabularyPreviewUiState,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(top = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.starter_vocabulary),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(Res.string.words, state.words.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        // Word list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = PaddingValues(bottom = spacing.md)
        ) {
            items(state.words) { word ->
                VocabularyWordItem(
                    word = word.originalWord,
                    translation = word.translation
                )
            }
        }

        // Buttons — pill style matching onboarding intro
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .padding(top = spacing.md)
                    .fillMaxWidth()
                    .widthIn(max = dimensions.contentMaxWidth),
                contentPadding = PaddingValues(
                    vertical = ButtonContentPaddingVertical,
                    horizontal = ButtonContentPaddingHorizontal
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(Theme.shapes.pill)
            ) {
                Text(stringResource(Res.string.accept), style = MaterialTheme.typography.labelLarge)
            }

            TextButton(
                onClick = onDeny,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = dimensions.contentMaxWidth)
            ) {
                Text(
                    stringResource(Res.string.deny),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun VocabularyWordItem(
    word: String,
    translation: String
) {
    val spacing = Theme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.xxxs))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
