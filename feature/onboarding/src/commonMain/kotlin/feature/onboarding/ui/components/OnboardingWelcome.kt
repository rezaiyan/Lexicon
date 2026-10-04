package feature.onboarding.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import components.sheet.SheetTitle
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.onboarding_get_started
import lexicon.resources.generated.resources.onboarding_skip_to_empty
import lexicon.resources.generated.resources.onboarding_welcome_highlight
import lexicon.resources.generated.resources.onboarding_welcome_languages
import lexicon.resources.generated.resources.onboarding_welcome_languages_desc
import lexicon.resources.generated.resources.onboarding_welcome_level
import lexicon.resources.generated.resources.onboarding_welcome_level_desc
import lexicon.resources.generated.resources.onboarding_welcome_starter
import lexicon.resources.generated.resources.onboarding_welcome_starter_desc
import lexicon.resources.generated.resources.onboarding_welcome_subtitle
import lexicon.resources.generated.resources.onboarding_welcome_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private val LogoTileSize = 64.dp
private val LogoTileCorner = 20.dp
private val LogoLetterSize = 38.sp
private const val GradientDarkAlpha = 0.18f

private data class WelcomeItem(val title: StringResource, val description: StringResource)

private val WelcomeItems = listOf(
    WelcomeItem(Res.string.onboarding_welcome_languages, Res.string.onboarding_welcome_languages_desc),
    WelcomeItem(Res.string.onboarding_welcome_level, Res.string.onboarding_welcome_level_desc),
    WelcomeItem(Res.string.onboarding_welcome_starter, Res.string.onboarding_welcome_starter_desc),
)

/** First screen after sign-in: what setup involves, with a way out to an empty list. */
@Composable
internal fun OnboardingWelcome(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(welcomeBackdrop())) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .onboardingContentWidth()
                    .padding(top = Theme.spacing.xxxl + Theme.spacing.lg, bottom = Theme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
                    LogoTile()
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
                        SheetTitle(
                            title = stringResource(Res.string.onboarding_welcome_title),
                            highlight = stringResource(Res.string.onboarding_welcome_highlight),
                        )
                        Text(
                            stringResource(Res.string.onboarding_welcome_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                WelcomeChecklist()
            }
        }

        OnboardingFooter(showDivider = false) {
            SheetPrimaryButton(text = stringResource(Res.string.onboarding_get_started), onClick = onGetStarted)
            SheetTextButton(text = stringResource(Res.string.onboarding_skip_to_empty), onClick = onSkip)
        }
    }
}

@Composable
private fun LogoTile() {
    val shape = RoundedCornerShape(LogoTileCorner)
    Box(
        modifier = Modifier
            .size(LogoTileSize)
            .shadow(elevation = Theme.elevation.modal, shape = shape, spotColor = MaterialTheme.colorScheme.primary)
            .background(MaterialTheme.colorScheme.primary, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.app_name).take(1).uppercase(),
            fontSize = LogoLetterSize,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Numbered card listing the three parts of setup. */
@Composable
private fun WelcomeChecklist() {
    Surface(
        shape = RoundedCornerShape(Theme.shapes.large + Theme.spacing.xxs),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Theme.dimensions.borderWidth,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xxs)) {
            WelcomeItems.forEachIndexed { index, item ->
                WelcomeRow(
                    number = index + 1,
                    title = stringResource(item.title),
                    description = stringResource(item.description),
                )
                if (index < WelcomeItems.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeRow(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.lg)
            .padding(vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm + Theme.spacing.xxxs),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTargetSmall)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                    RoundedCornerShape(Theme.shapes.medium),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Login's brand wash fading into the page, so sign-in flows straight into setup. */
@Composable
private fun welcomeBackdrop(): Brush {
    val background = MaterialTheme.colorScheme.background
    val top = if (background.luminance() < 0.5f) {
        AppColors.primary.copy(alpha = GradientDarkAlpha).compositeOver(background)
    } else {
        AppColors.loginGradientTop
    }
    return Brush.verticalGradient(0f to top, 0.45f to background)
}
