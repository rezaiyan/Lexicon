package feature.onboarding.model

data class OnboardingUiState(
    val currentStep: Int = 0,
    val availableLanguages: List<String> = listOf(
        "English", "German", "French", "Spanish", "Italian",
        "Portuguese", "Dutch", "Russian", "Chinese", "Japanese",
        "Korean", "Arabic", "Turkish", "Persian"
    ),
    val selectedTargetLanguage: String? = null,
    val selectedNativeLanguage: String? = null,
    val selectedLevel: String? = null,
    val selectedDailyGoal: Int = 10,
    val interests: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalSteps: Int get() = 4
}
