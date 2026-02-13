package presentation.model

data class OnboardingUiState(
    val currentStep: Int = 1,
    val availableLanguages: List<String> = listOf(
        "English", "German", "French", "Spanish", "Italian",
        "Portuguese", "Dutch", "Russian", "Chinese", "Japanese",
        "Korean", "Arabic", "Turkish", "Persian"
    ),
    val selectedTargetLanguage: String? = null,
    val selectedNativeLanguage: String? = null,
    val selectedLevel: String? = null,
    val interests: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalSteps: Int get() = 3
    val canGoNext: Boolean
        get() = when (currentStep) {
            1 -> selectedTargetLanguage != null
            2 -> selectedNativeLanguage != null
            3 -> selectedLevel != null
            else -> false
        }
}
