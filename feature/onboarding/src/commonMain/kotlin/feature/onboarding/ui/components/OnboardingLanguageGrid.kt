package feature.onboarding.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import theme.LanguageColors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.flag_cn
import lexicon.resources.generated.resources.flag_de
import lexicon.resources.generated.resources.flag_es
import lexicon.resources.generated.resources.flag_fr
import lexicon.resources.generated.resources.flag_gb
import lexicon.resources.generated.resources.flag_ir
import lexicon.resources.generated.resources.flag_it
import lexicon.resources.generated.resources.flag_jp
import lexicon.resources.generated.resources.flag_kr
import lexicon.resources.generated.resources.flag_nl
import lexicon.resources.generated.resources.flag_pt
import lexicon.resources.generated.resources.flag_ru
import lexicon.resources.generated.resources.flag_sa
import lexicon.resources.generated.resources.flag_tr
import lexicon.resources.generated.resources.language_arabic
import lexicon.resources.generated.resources.language_chinese
import lexicon.resources.generated.resources.language_dutch
import lexicon.resources.generated.resources.language_english
import lexicon.resources.generated.resources.language_french
import lexicon.resources.generated.resources.language_german
import lexicon.resources.generated.resources.language_italian
import lexicon.resources.generated.resources.language_japanese
import lexicon.resources.generated.resources.language_korean
import lexicon.resources.generated.resources.language_persian
import lexicon.resources.generated.resources.language_portuguese
import lexicon.resources.generated.resources.language_russian
import lexicon.resources.generated.resources.language_spanish
import lexicon.resources.generated.resources.language_turkish
import lexicon.resources.generated.resources.onboarding_language_flag
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

internal val languageNativeNames = mapOf(
    "English" to "ENGLISH",
    "German" to "DEUTSCH",
    "French" to "FRANÇAIS",
    "Spanish" to "ESPAÑOL",
    "Italian" to "ITALIANO",
    "Portuguese" to "PORTUGUÊS",
    "Dutch" to "NEDERLANDS",
    "Russian" to "РУССКИЙ",
    "Chinese" to "中文",
    "Japanese" to "日本語",
    "Korean" to "한국어",
    "Arabic" to "العربية",
    "Turkish" to "TÜRKÇE",
    "Persian" to "فارسی"
)

internal val languageFlags = mapOf(
    "English" to Res.drawable.flag_gb,
    "German" to Res.drawable.flag_de,
    "French" to Res.drawable.flag_fr,
    "Spanish" to Res.drawable.flag_es,
    "Italian" to Res.drawable.flag_it,
    "Portuguese" to Res.drawable.flag_pt,
    "Dutch" to Res.drawable.flag_nl,
    "Russian" to Res.drawable.flag_ru,
    "Chinese" to Res.drawable.flag_cn,
    "Japanese" to Res.drawable.flag_jp,
    "Korean" to Res.drawable.flag_kr,
    "Arabic" to Res.drawable.flag_sa,
    "Turkish" to Res.drawable.flag_tr,
    "Persian" to Res.drawable.flag_ir
)

private val languageColorsLight = LanguageColors.light
private val languageColorsDark = LanguageColors.dark

private val languageDisplayNames: Map<String, StringResource> = mapOf(
    "English" to Res.string.language_english,
    "German" to Res.string.language_german,
    "French" to Res.string.language_french,
    "Spanish" to Res.string.language_spanish,
    "Italian" to Res.string.language_italian,
    "Portuguese" to Res.string.language_portuguese,
    "Dutch" to Res.string.language_dutch,
    "Russian" to Res.string.language_russian,
    "Chinese" to Res.string.language_chinese,
    "Japanese" to Res.string.language_japanese,
    "Korean" to Res.string.language_korean,
    "Arabic" to Res.string.language_arabic,
    "Turkish" to Res.string.language_turkish,
    "Persian" to Res.string.language_persian,
)

private val SelectionBadgeSize = 22.dp
private val CheckIconSize = 14.dp
private val FlagContainerSize = 56.dp
private val FlagImageSize = 44.dp

@Composable
fun LanguageGrid(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    val spacing = Theme.spacing
    val isDark = !MaterialTheme.colorScheme.surface.luminance().let { it > 0.5f }
    val colorMap = if (isDark) languageColorsDark else languageColorsLight
    val fallback = MaterialTheme.colorScheme.surfaceVariant

    val rows = (languages.size + 1) / 2
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                val firstIndex = rowIndex * 2
                val firstKey = languages[firstIndex]
                val firstDisplayName = languageDisplayNames[firstKey]?.let { stringResource(it) } ?: firstKey
                LanguageGridCard(
                    language = firstDisplayName,
                    nativeName = languageNativeNames[firstKey] ?: firstKey.uppercase(),
                    flag = languageFlags[firstKey],
                    selected = selectedLanguage == firstKey,
                    onClick = { onLanguageSelected(firstKey) },
                    tileColor = colorMap[firstKey] ?: fallback,
                    modifier = Modifier.weight(1f)
                )
                val secondIndex = firstIndex + 1
                if (secondIndex < languages.size) {
                    val secondKey = languages[secondIndex]
                    val secondDisplayName = languageDisplayNames[secondKey]?.let { stringResource(it) } ?: secondKey
                    LanguageGridCard(
                        language = secondDisplayName,
                        nativeName = languageNativeNames[secondKey] ?: secondKey.uppercase(),
                        flag = languageFlags[secondKey],
                        selected = selectedLanguage == secondKey,
                        onClick = { onLanguageSelected(secondKey) },
                        tileColor = colorMap[secondKey] ?: fallback,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun LanguageGridCard(
    language: String,
    nativeName: String,
    flag: DrawableResource?,
    selected: Boolean,
    onClick: () -> Unit,
    tileColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f).compositeOver(tileColor)
        else tileColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bg_$language"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "border_$language"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Theme.elevation.medium else Theme.elevation.none
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Theme.spacing.xs)
                        .size(SelectionBadgeSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(CheckIconSize),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.md, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
            ) {
                if (flag != null) {
                    Box(
                        modifier = Modifier
                            .size(FlagContainerSize)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(flag),
                            contentDescription = stringResource(Res.string.onboarding_language_flag, language),
                            modifier = Modifier
                                .size(FlagImageSize)
                                .clip(RoundedCornerShape(Theme.shapes.small)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Text(
                    text = language,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
