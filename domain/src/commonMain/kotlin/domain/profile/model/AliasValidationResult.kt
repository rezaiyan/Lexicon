package domain.profile.model

sealed interface AliasValidationResult {
    data object Valid : AliasValidationResult
    data object TooShort : AliasValidationResult
    data object TooLong : AliasValidationResult
    data object InvalidCharacters : AliasValidationResult
}
