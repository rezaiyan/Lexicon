package domain.profile.usecase

import domain.profile.model.AliasValidationResult

class ValidateDisplayAliasUseCase {
    operator fun invoke(alias: String): AliasValidationResult {
        if (alias.isBlank()) return AliasValidationResult.Valid
        if (alias.length < 2) return AliasValidationResult.TooShort
        if (alias.length > 30) return AliasValidationResult.TooLong
        if (!alias.matches(Regex("^[a-zA-Z0-9 _-]+$"))) return AliasValidationResult.InvalidCharacters
        return AliasValidationResult.Valid
    }
}
