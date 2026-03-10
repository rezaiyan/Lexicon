package presentation.ui.components.imports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import feature.onboarding.ui.components.LanguageGrid
import feature.onboarding.ui.components.LevelCards
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_wizard_continue
import lexicon.resources.generated.resources.ai_wizard_level_highlight
import lexicon.resources.generated.resources.ai_wizard_level_subtitle
import lexicon.resources.generated.resources.ai_wizard_level_title
import org.jetbrains.compose.resources.stringResource
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

@Composable
internal fun AiLanguageStep(
    title: String,
    highlight: String,
    subtitle: String,
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            AiStepHeader(
                title = title,
                highlight = highlight,
                subtitle = subtitle,
                spacing = spacing
            )

            LanguageGrid(
                languages = languages,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}

@Composable
internal fun AiLevelStep(
    selectedLevel: String?,
    error: String?,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = spacing.xs)
        ) {
            AiStepHeader(
                title = stringResource(Res.string.ai_wizard_level_title),
                highlight = stringResource(Res.string.ai_wizard_level_highlight),
                subtitle = stringResource(Res.string.ai_wizard_level_subtitle),
                spacing = spacing
            )

            LevelCards(
                selectedLevel = selectedLevel,
                onLevelSelected = onLevelSelected
            )

            error?.let {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth),
                enabled = selectedLevel != null,
                contentPadding = PaddingValues(vertical = spacing.md, horizontal = spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(Theme.shapes.pill)
            ) {
                Text(stringResource(Res.string.ai_wizard_continue), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.size(spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
        }
    }
}

@Composable
internal fun AiStepHeader(
    title: String,
    highlight: String,
    subtitle: String,
    spacing: AppSpacing,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = highlight,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(spacing.xxs))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(spacing.md))
}
