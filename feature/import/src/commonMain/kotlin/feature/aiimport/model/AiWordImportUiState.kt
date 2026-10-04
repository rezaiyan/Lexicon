package feature.aiimport.model

import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.ProficiencyLevel
import domain.onboarding.model.SuggestedVocabulary
import domain.tag.model.Tag
import utils.Language

enum class AiWordImportStep { TARGET_LANG, NATIVE_LANG, LEVEL, TOPICS, PREVIEW }

data class AiWordImportUiState(
    val step: AiWordImportStep = AiWordImportStep.TARGET_LANG,
    val availableLanguages: List<Language> = OnboardingPreferences.SupportedLanguages,
    val selectedTargetLanguage: Language? = null,
    val selectedNativeLanguage: Language? = null,
    val selectedLevel: ProficiencyLevel? = null,
    val selectedTopics: Set<String> = emptySet(),
    val availableTopics: List<String> = listOf(
        "Daily Life", "Travel", "Business", "Food", "Technology",
        "Sports", "Health", "Arts", "Nature", "Academic"
    ),
    val suggestedWords: List<SuggestedVocabulary> = emptyList(),
    val selectedWordIndices: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
)
