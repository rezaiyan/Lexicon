package domain.profile.usecase

import domain.profile.model.AliasValidationResult

class ValidateDisplayAliasUseCase {
    operator fun invoke(alias: String): AliasValidationResult = when {
        alias.isBlank() -> AliasValidationResult.Valid
        alias.length < 2 -> AliasValidationResult.TooShort
        alias.length > 30 -> AliasValidationResult.TooLong
        !alias.matches(Regex("^[a-zA-Z0-9 _-]+$")) -> AliasValidationResult.InvalidCharacters
        else -> AliasValidationResult.Valid
    }
}
