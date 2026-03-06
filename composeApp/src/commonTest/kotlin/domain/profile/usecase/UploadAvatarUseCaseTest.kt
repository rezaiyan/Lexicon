package domain.profile.usecase

import core.common.Try
import core.common.getOrThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadAvatarUseCaseTest {

    private val repository = FakeProfileRepository()
    private val useCase = UploadAvatarUseCase(repository)

    @Test
    fun `uploads avatar and returns url`() = runTest {
        repository.uploadResult = Try.success("https://cdn.example.com/avatar.png")

        val result = useCase(byteArrayOf(1, 2, 3), "image/png")

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.example.com/avatar.png", result.getOrThrow())
    }

    @Test
    fun `uploads avatar via Params`() = runTest {
        repository.uploadResult = Try.success("https://cdn.example.com/avatar.jpg")

        val result = useCase(UploadAvatarUseCase.Params(byteArrayOf(1, 2, 3), "image/jpeg"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure on upload error`() = runTest {
        repository.uploadResult = Try.failure(RuntimeException("Upload failed"))

        val result = useCase(byteArrayOf(1, 2, 3), "image/png")

        assertTrue(result.isFailure)
    }
}
