package domain.word.usecase

import core.common.Try
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for DeleteWordsUseCase
 * 
 * Tests cover:
 * - Empty word list handling
 * - Successful deletion flow
 * - Error handling
 * - Flow state progression
 * - Repository interaction
 */
class DeleteWordsUseCaseTest {
    
    private val fakeRepository = FakeWordRepositoryForDelete()
    private val useCase = DeleteWordsUseCase(fakeRepository)
    
    @Test
    fun `empty word list should return error`() = runTest {
        // Given: Empty word list
        val wordIds = emptyList<Int>()
        
        // When: Attempting to delete
        val result = useCase(wordIds).first()
        
        // Then: Should return error
        assertTrue(result is DeleteWordsResult.Error)
        assertEquals("No words selected", result.message)
        assertEquals(0, fakeRepository.deleteWordsCallCount)
    }
    
    @Test
    fun `successful deletion should emit correct flow states`() = runTest {
        // Given: Valid word IDs
        val wordIds = listOf(1, 2, 3)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(3),
            DeleteWordsProgress.DeletingFromLocal(3),
            DeleteWordsProgress.Completed(3)
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should emit all expected states
        assertEquals(4, results.size) // Initial + 3 repository states
        
        // Initial deleting state
        assertTrue(results[0] is DeleteWordsResult.Deleting)
        assertEquals(3, (results[0] as DeleteWordsResult.Deleting).count)
        
        // Backend deletion
        assertTrue(results[1] is DeleteWordsResult.DeletingBackend)
        assertEquals(3, (results[1] as DeleteWordsResult.DeletingBackend).count)
        
        // Local deletion
        assertTrue(results[2] is DeleteWordsResult.DeletingLocal)
        assertEquals(3, (results[2] as DeleteWordsResult.DeletingLocal).count)
        
        // Success
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(3, (results[3] as DeleteWordsResult.Success).count)
        
        assertEquals(1, fakeRepository.deleteWordsCallCount)
        assertEquals(wordIds, fakeRepository.lastDeletedIds)
    }
    
    @Test
    fun `repository error should be mapped to error result`() = runTest {
        // Given: Valid word IDs but repository fails
        val wordIds = listOf(1, 2)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(2),
            DeleteWordsProgress.Failed("Network error")
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should emit error state
        assertEquals(3, results.size)
        
        // Initial deleting state
        assertTrue(results[0] is DeleteWordsResult.Deleting)
        
        // Backend deletion
        assertTrue(results[1] is DeleteWordsResult.DeletingBackend)
        
        // Error
        assertTrue(results[2] is DeleteWordsResult.Error)
        assertEquals("Network error", (results[2] as DeleteWordsResult.Error).message)
    }
    
    @Test
    fun `single word deletion should work`() = runTest {
        // Given: Single word ID
        val wordIds = listOf(42)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(1),
            DeleteWordsProgress.DeletingFromLocal(1),
            DeleteWordsProgress.Completed(1)
        )
        
        // When: Deleting word
        val results = useCase(wordIds).toList()
        
        // Then: Should succeed
        assertEquals(4, results.size)
        assertTrue(results[0] is DeleteWordsResult.Deleting)
        assertTrue(results[1] is DeleteWordsResult.DeletingBackend)
        assertTrue(results[2] is DeleteWordsResult.DeletingLocal)
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(1, (results[3] as DeleteWordsResult.Success).count)
    }
    
    @Test
    fun `large word list deletion should work`() = runTest {
        // Given: Large list of word IDs
        val wordIds = (1..100).toList()
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(100),
            DeleteWordsProgress.DeletingFromLocal(100),
            DeleteWordsProgress.Completed(100)
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should succeed
        assertEquals(4, results.size)
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(100, (results[3] as DeleteWordsResult.Success).count)
        assertEquals(wordIds, fakeRepository.lastDeletedIds)
    }
    
    @Test
    fun `backend deletion failure should be handled`() = runTest {
        // Given: Word IDs but backend fails immediately
        val wordIds = listOf(1, 2, 3)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.Failed("Backend unavailable")
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should emit error
        assertEquals(2, results.size)
        assertTrue(results[0] is DeleteWordsResult.Deleting)
        assertTrue(results[1] is DeleteWordsResult.Error)
        assertEquals("Backend unavailable", (results[1] as DeleteWordsResult.Error).message)
    }
    
    @Test
    fun `local deletion failure should be handled`() = runTest {
        // Given: Word IDs but local deletion fails
        val wordIds = listOf(1, 2)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(2),
            DeleteWordsProgress.DeletingFromLocal(2),
            DeleteWordsProgress.Failed("Database error")
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should emit error
        assertEquals(4, results.size)
        assertTrue(results[3] is DeleteWordsResult.Error)
        assertEquals("Database error", (results[3] as DeleteWordsResult.Error).message)
    }
    
    @Test
    fun `partial deletion should be handled`() = runTest {
        // Given: Word IDs but only some are deleted
        val wordIds = listOf(1, 2, 3, 4, 5)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(5),
            DeleteWordsProgress.DeletingFromLocal(3), // Only 3 deleted locally
            DeleteWordsProgress.Completed(3)
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should succeed with partial count
        assertEquals(4, results.size)
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(3, (results[3] as DeleteWordsResult.Success).count)
    }
    
    @Test
    fun `duplicate word IDs should be handled`() = runTest {
        // Given: Word IDs with duplicates
        val wordIds = listOf(1, 2, 1, 3, 2)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(5),
            DeleteWordsProgress.DeletingFromLocal(5),
            DeleteWordsProgress.Completed(5)
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should succeed (repository handles deduplication)
        assertEquals(4, results.size)
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(5, (results[3] as DeleteWordsResult.Success).count)
        assertEquals(wordIds, fakeRepository.lastDeletedIds)
    }
    
    @Test
    fun `negative word IDs should be handled`() = runTest {
        // Given: Word IDs with negative values
        val wordIds = listOf(-1, 0, 1)
        fakeRepository.setDeleteFlow(
            DeleteWordsProgress.DeletingFromBackend(3),
            DeleteWordsProgress.DeletingFromLocal(3),
            DeleteWordsProgress.Completed(3)
        )
        
        // When: Deleting words
        val results = useCase(wordIds).toList()
        
        // Then: Should succeed (repository handles validation)
        assertEquals(4, results.size)
        assertTrue(results[3] is DeleteWordsResult.Success)
        assertEquals(wordIds, fakeRepository.lastDeletedIds)
    }
}

/**
 * Fake repository for testing DeleteWordsUseCase
 */
internal class FakeWordRepositoryForDelete : IWordRepository {
    var deleteWordsCallCount = 0
    var lastDeletedIds: List<Int>? = null
    private var deleteFlow: kotlinx.coroutines.flow.Flow<DeleteWordsProgress> = 
        kotlinx.coroutines.flow.flowOf(DeleteWordsProgress.Completed(0))
    
    fun setDeleteFlow(vararg progress: DeleteWordsProgress) {
        deleteFlow = kotlinx.coroutines.flow.flowOf(*progress)
    }
    
    override fun deleteWords(ids: List<Int>): kotlinx.coroutines.flow.Flow<DeleteWordsProgress> {
        deleteWordsCallCount++
        lastDeletedIds = ids
        return deleteFlow
    }
    
    // Other methods not needed for this test
    override suspend fun getAllWordsAsync(): List<domain.word.model.Word> = emptyList()
    override fun getAllWords() = kotlinx.coroutines.flow.flowOf<List<domain.word.model.Word>>(emptyList())
    override fun getDueCards() = kotlinx.coroutines.flow.flowOf<List<domain.word.model.Word>>(emptyList())
    override fun getWordsByStage(stage: domain.word.model.LearningStage) = kotlinx.coroutines.flow.flowOf<List<domain.word.model.Word>>(emptyList())
    override suspend fun getWordById(id: Int) = null
    override suspend fun insertWords(words: List<domain.word.model.Word>): Int = words.size
    override suspend fun updateWord(word: domain.word.model.Word) {}
    override suspend fun deleteWord(id: Int) {}
    override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
    override suspend fun syncWithRemote() = Try.success(Unit)
    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
    override fun getProgressStats() = kotlinx.coroutines.flow.flowOf(domain.word.model.ProgressStats())
    override suspend fun getTotalCount() = 0
    override suspend fun getDueCount() = 0
    override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): kotlinx.coroutines.flow.Flow<UpdateWordsLanguagesProgress> = kotlinx.coroutines.flow.flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
}
