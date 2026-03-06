package feature.aiimport.model

import domain.onboarding.model.SuggestedVocabulary

enum class AiWordImportStep { TARGET_LANG, NATIVE_LANG, LEVEL, TOPICS, PREVIEW }

data class AiWordImportUiState(
    val step: AiWordImportStep = AiWordImportStep.TARGET_LANG,
    val availableLanguages: List<String> = listOf(
        "English", "German", "French", "Spanish", "Italian",
        "Portuguese", "Dutch", "Russian", "Chinese", "Japanese",
        "Korean", "Arabic", "Turkish", "Persian"
    ),
    val selectedTargetLanguage: String? = null,
    val selectedNativeLanguage: String? = null,
    val selectedLevel: String? = null,
    val selectedTopics: Set<String> = emptySet(),
    val availableTopics: List<String> = listOf(
        "Daily Life", "Travel", "Business", "Food", "Technology",
        "Sports", "Health", "Arts", "Nature", "Academic"
    ),
    val suggestedWords: List<SuggestedVocabulary> = emptyList(),
    val selectedWordIndices: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
