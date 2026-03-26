package domain.profile

import domain.profile.model.AliasValidationResult
import domain.profile.usecase.ValidateDisplayAliasUseCase
import kotlin.test.Test
import kotlin.test.assertIs

class ValidateDisplayAliasUseCaseTest {

    private val useCase = ValidateDisplayAliasUseCase()

    @Test
    fun `blank alias is valid`() {
        assertIs<AliasValidationResult.Valid>(useCase(""))
        assertIs<AliasValidationResult.Valid>(useCase("   "))
    }

    @Test
    fun `single character alias is too short`() {
        assertIs<AliasValidationResult.TooShort>(useCase("a"))
    }

    @Test
    fun `two character alias is valid`() {
        assertIs<AliasValidationResult.Valid>(useCase("ab"))
    }

    @Test
    fun `thirty character alias is valid`() {
        assertIs<AliasValidationResult.Valid>(useCase("a".repeat(30)))
    }

    @Test
    fun `thirty-one character alias is too long`() {
        assertIs<AliasValidationResult.TooLong>(useCase("a".repeat(31)))
    }

    @Test
    fun `alias with valid characters is valid`() {
        assertIs<AliasValidationResult.Valid>(useCase("hello_world"))
        assertIs<AliasValidationResult.Valid>(useCase("user-name"))
        assertIs<AliasValidationResult.Valid>(useCase("User Name"))
        assertIs<AliasValidationResult.Valid>(useCase("abc123"))
    }

    @Test
    fun `alias with special characters is invalid`() {
        assertIs<AliasValidationResult.InvalidCharacters>(useCase("bad@name"))
        assertIs<AliasValidationResult.InvalidCharacters>(useCase("no!allowed"))
        assertIs<AliasValidationResult.InvalidCharacters>(useCase("has.dot"))
    }
}
