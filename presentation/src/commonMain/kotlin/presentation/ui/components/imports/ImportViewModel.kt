package presentation.ui.components.imports

import androidx.lifecycle.viewModelScope
import domain.ai.usecase.ExtractVocabularyFromImageUseCase
import domain.ai.usecase.ExtractVocabularyResult
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import core.common.fold
import core.common.getOrDefault
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.word.usecase.GetSourceLanguageUseCase
import domain.word.usecase.ImportViaFileUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import core.base.BaseViewModel
import performance.IPerformanceTracer
import presentation.model.ImageImportState
import utils.Language

data class ImportTagUseCases(
    val getTags: GetTagsUseCase,
    val createTag: CreateTagUseCase,
)

class ImportViewModel(
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val importWordsUseCase: ImportWordsUseCase,
    private val importViaFileUseCase: ImportViaFileUseCase,
    private val extractVocabularyFromImageUseCase: ExtractVocabularyFromImageUseCase,
    private val userManager: IUserManager,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
    private val getSourceLanguageUseCase: GetSourceLanguageUseCase,
    private val tagUseCases: ImportTagUseCases,
    private val performanceTracer: IPerformanceTracer,
) : BaseViewModel<ImportUiState, ImportEffect>() {

    override fun initialState() = ImportUiState()

    init {
        viewModelScope.launch {
            val targetLanguage = getCurrentLanguageUseCase().getOrDefault(Language.ENGLISH)
            val sourceLanguage = getSourceLanguageUseCase().getOrDefault(Language.ENGLISH)
            updateState { copy(targetLanguage = targetLanguage, sourceLanguage = sourceLanguage) }
        }
        observeFeatureAccess()
        observeTags()
    }

    private fun observeTags() {
        viewModelScope.launch {
            tagUseCases.getTags()
                .catch { }
                .collect { tags -> updateState { copy(tags = tags) } }
        }
    }

    fun selectTag(tagId: Long?) {
        updateState { copy(selectedTagId = tagId) }
    }

    fun showCreateTagDialog() {
        updateState { copy(showCreateTagDialog = true) }
    }

    fun dismissCreateTagDialog() {
        updateState { copy(showCreateTagDialog = false) }
    }

    fun createTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            tagUseCases.createTag(trimmed).fold(
                onSuccess = { tag ->
                    updateState { copy(showCreateTagDialog = false, selectedTagId = tag.id) }
                },
                onFailure = { }
            )
        }
    }

    @Suppress("OPT_IN_USAGE")
    private fun observeFeatureAccess() {
        viewModelScope.launch {
            userManager.observeUser()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(currentState.tabs.filter { it !is ImportTabV2.Image })
                    } else {
                        getFeatureAccessUseCase.invoke()
                            .map { featureAccess ->
                                val hasPremiumAccess = featureAccess.userAccess.hasPremiumAccess
                                val currentTabs = currentState.tabs.toMutableList()
                                val currentImageTab =
                                    currentTabs.firstOrNull { it is ImportTabV2.Image }
                                if (hasPremiumAccess && currentImageTab == null) {
                                    currentTabs.add(ImportTabV2.Image())
                                } else if (!hasPremiumAccess && currentImageTab != null) {
                                    currentTabs.remove(currentImageTab)
                                }
                                currentTabs.toList()
                            }
                            .catch { emit(currentState.tabs) }
                    }
                }
                .collect { tabs ->
                    updateState { copy(tabs = tabs) }
                }
        }
    }

    fun selectTab(selectedTab: ImportTabV2) {
        val matchingTab = when (selectedTab) {
            is ImportTabV2.Text -> currentState.tabs.filterIsInstance<ImportTabV2.Text>().firstOrNull()
            is ImportTabV2.File -> currentState.tabs.filterIsInstance<ImportTabV2.File>().firstOrNull()
            is ImportTabV2.Image -> currentState.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        }
        if (matchingTab != null) {
            updateState { copy(selectedTab = matchingTab) }
        }
    }

    fun updateWord(word: String) {
        updateState {
            copy(
                textInputState = textInputState.copy(
                    word = word,
                    errorMessage = null
                )
            )
        }
    }

    fun updateTranslation(translation: String) {
        updateState {
            copy(
                textInputState = textInputState.copy(
                    translation = translation,
                    errorMessage = null
                )
            )
        }
    }

    fun updateDescription(description: String) {
        updateState {
            copy(
                textInputState = textInputState.copy(
                    description = description,
                    errorMessage = null
                )
            )
        }
    }

    @Suppress("LongMethod")
    fun addWord() {
        val textState = currentState.textInputState
        val word = textState.word.trim().replace(",", " ")
        val translation = textState.translation.trim().replace(",", " ")
        val description = textState.description.trim().replace(",", " ")

        if (word.isBlank() || translation.isBlank()) return

        val csvLine = if (description.isNotBlank()) {
            "$word,$translation,$description"
        } else {
            "$word,$translation"
        }

        updateState {
            copy(
                textInputState = textState.copy(isEnabled = false, errorMessage = null)
            )
        }

        viewModelScope.launch {
            importWordsUseCase(
                csvLine,
                currentState.targetLanguage,
                currentState.sourceLanguage,
                currentState.selectedTagId
            ).fold(
                onSuccess = { count ->
                    val newCount = currentState.textInputState.wordsAddedCount + count
                    updateState {
                        copy(
                            textInputState = TextInputState(
                                wordsAddedCount = newCount,
                                showSuccessIndicator = true,
                            )
                        )
                    }
                    emitEffect(ImportEffect.WordAddedSuccessfully(count))
                    delay(1500)
                    updateState {
                        copy(
                            textInputState = textInputState.copy(
                                showSuccessIndicator = false
                            )
                        )
                    }
                },
                onFailure = { error ->
                    val raw = error.message.orEmpty()
                    val isNetwork = raw.contains("timeout", ignoreCase = true) ||
                        raw.contains("connect", ignoreCase = true) ||
                        raw.contains("network", ignoreCase = true)

                    val friendlyMessage = when {
                        isNetwork -> "You're offline -- the word will be saved when you reconnect."
                        raw.isNotEmpty() -> raw
                        else -> "Failed to add word. Please try again."
                    }

                    updateState {
                        copy(
                            textInputState = textInputState.copy(
                                isEnabled = true,
                                errorMessage = friendlyMessage
                            )
                        )
                    }
                }
            )
        }
    }

    fun selectImage(imageBytes: ByteArray) {
        updateState {
            val updatedTabs = tabs.map { tab ->
                if (tab is ImportTabV2.Image) tab.copy(selectedImage = imageBytes) else tab
            }
            val updatedSelected = when (val selected = selectedTab) {
                is ImportTabV2.Image -> selected.copy(selectedImage = imageBytes)
                else -> tabs.filterIsInstance<ImportTabV2.Image>()
                    .firstOrNull() ?: selected
            }
            copy(tabs = updatedTabs, selectedTab = updatedSelected)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun importImage() {
        val imageTab = when (val selected = currentState.selectedTab) {
            is ImportTabV2.Image -> selected
            else -> currentState.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        } ?: return

        val imageBytes = imageTab.selectedImage ?: return

        viewModelScope.launch {
            updateState { copy(imageImportState = ImageImportState.Loading) }
            val trace = performanceTracer.startTrace("import_image_extraction")

            withContext(Dispatchers.Default) {
                extractVocabularyFromImageUseCase(
                    imageBytes = imageBytes,
                    extractWords = true,
                    extractSentences = true,
                ).collect { result ->
                    when (result) {
                        is ExtractVocabularyResult.Loading -> {
                            updateState { copy(imageImportState = ImageImportState.Loading) }
                        }

                        is ExtractVocabularyResult.Success -> {
                            val wordItems = parseCsvToWordItems(result.csvText)
                            clearSelectedImage()
                            updateState {
                                copy(
                                    imageImportState = ImageImportState.Idle,
                                    imageReviewState = ImageReviewState.Review(words = wordItems),
                                )
                            }
                            performanceTracer.putMetric(trace, "words_extracted", wordItems.size.toLong())
                            performanceTracer.stopTrace(trace)
                        }

                        is ExtractVocabularyResult.Error -> {
                            clearSelectedImage()
                            val raw = result.message
                            val isNetwork = raw.contains("timeout", ignoreCase = true) ||
                                raw.contains("connect", ignoreCase = true) ||
                                raw.contains("network", ignoreCase = true)

                            val friendlyMessage = when {
                                isNetwork -> "You're offline -- please check your connection and try again."
                                raw.contains("empty", ignoreCase = true) ||
                                    raw.contains("no words", ignoreCase = true) ||
                                    raw.contains("no text", ignoreCase = true) ->
                                    "No vocabulary found in this image. Try a photo with clearer, larger text."
                                else -> "Image extraction failed -- try a clearer photo with visible text."
                            }

                            updateState {
                                copy(imageImportState = ImageImportState.Error(friendlyMessage))
                            }
                            performanceTracer.putAttribute(trace, "error", raw)
                            performanceTracer.stopTrace(trace)
                            emitEffect(ImportEffect.Error(friendlyMessage))
                        }
                    }
                }
            }
        }
    }

    fun removeExtractedWord(id: Int) {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        updateState {
            copy(imageReviewState = reviewState.copy(words = reviewState.words.filter { it.id != id }))
        }
    }

    fun startEditingWord(id: Int) {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        updateState { copy(imageReviewState = reviewState.copy(editingWordId = id)) }
    }

    fun cancelEditingWord() {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        updateState { copy(imageReviewState = reviewState.copy(editingWordId = null)) }
    }

    fun saveEditedWord(id: Int, word: String, translation: String, description: String) {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        val updatedWords = reviewState.words.map { item ->
            if (item.id == id) item.copy(word = word, translation = translation, description = description)
            else item
        }
        updateState {
            copy(imageReviewState = reviewState.copy(words = updatedWords, editingWordId = null))
        }
    }

    fun confirmImageImport() {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        val words = reviewState.words
        if (words.isEmpty()) return

        val csvText = words.joinToString("\n") { item ->
            if (item.description.isNotBlank()) "${item.word},${item.translation},${item.description}"
            else "${item.word},${item.translation}"
        }

        updateState { copy(imageReviewState = reviewState.copy(isImporting = true)) }

        viewModelScope.launch {
            val trace = performanceTracer.startTrace("import_image_processing")
            importWordsUseCase(
                csvText,
                currentState.targetLanguage,
                currentState.sourceLanguage,
                currentState.selectedTagId,
            ).fold(
                onSuccess = { count ->
                    updateState { copy(imageReviewState = ImageReviewState.None) }
                    performanceTracer.putMetric(trace, "words_imported", count.toLong())
                    performanceTracer.stopTrace(trace)
                    emitEffect(ImportEffect.ImageImportSuccessful(count))
                },
                onFailure = { error ->
                    val message = error.message ?: "Import failed"
                    updateState { copy(imageReviewState = reviewState.copy(isImporting = false)) }
                    performanceTracer.putAttribute(trace, "error", message)
                    performanceTracer.stopTrace(trace)
                    emitEffect(ImportEffect.Error(message))
                }
            )
        }
    }

    fun requestCancelImageReview() {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        updateState { copy(imageReviewState = reviewState.copy(showCancelConfirmation = true)) }
    }

    fun dismissCancelConfirmation() {
        val reviewState = currentState.imageReviewState as? ImageReviewState.Review ?: return
        updateState { copy(imageReviewState = reviewState.copy(showCancelConfirmation = false)) }
    }

    fun cancelImageReview() {
        updateState { copy(imageReviewState = ImageReviewState.None) }
    }

    fun importFile(fileContent: String, fileName: String? = null) {
        updateState {
            copy(
                showLanguageConfirmation = true,
                pendingImportAction = PendingImportAction.File(fileContent, fileName)
            )
        }
    }

    fun selectSourceLanguage(language: Language) {
        updateState { copy(sourceLanguage = language) }
    }

    fun selectTargetLanguage(language: Language) {
        updateState { copy(targetLanguage = language) }
    }

    fun confirmImport() {
        val pendingAction = currentState.pendingImportAction ?: return
        val sourceLanguage = currentState.sourceLanguage
        val targetLanguage = currentState.targetLanguage

        updateState {
            copy(
                showLanguageConfirmation = false,
                pendingImportAction = null
            )
        }

        when (pendingAction) {
            is PendingImportAction.File -> {
                viewModelScope.launch {
                    updateState { copy(fileImportState = ImportFileState.Loading) }
                    val trace = performanceTracer.startTrace("import_file_processing")
                    delay(1500)
                    withContext(Dispatchers.Default) {
                        importViaFileUseCase(
                            pendingAction.content,
                            pendingAction.fileName,
                            targetLanguage,
                            sourceLanguage,
                            currentState.selectedTagId
                        ).fold(
                            onSuccess = { count ->
                                if (count == 0) {
                                    val message = "No words found in this file. " +
                                        "Use the format: word,translation (one pair per line)."
                                    updateState {
                                        copy(fileImportState = ImportFileState.Error(message))
                                    }
                                    performanceTracer.putAttribute(trace, "error", "empty_result")
                                    performanceTracer.stopTrace(trace)
                                    emitEffect(ImportEffect.Error(message))
                                } else {
                                    updateState {
                                        copy(fileImportState = ImportFileState.Success(count))
                                    }
                                    performanceTracer.putMetric(trace, "words_imported", count.toLong())
                                    performanceTracer.stopTrace(trace)
                                    emitEffect(ImportEffect.FileImportSuccessful(count))
                                }
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Import failed"
                                updateState {
                                    copy(fileImportState = ImportFileState.Error(message))
                                }
                                performanceTracer.putAttribute(trace, "error", message)
                                performanceTracer.stopTrace(trace)
                                emitEffect(ImportEffect.Error(message))
                            }
                        )
                    }
                }
            }
        }
    }

    fun dismissLanguageConfirmation() {
        updateState {
            copy(
                showLanguageConfirmation = false,
                pendingImportAction = null
            )
        }
    }

    fun clearSelectedImage() {
        updateState {
            val updatedTabs = tabs.map { tab ->
                if (tab is ImportTabV2.Image) tab.copy(selectedImage = null) else tab
            }
            val updatedSelected = when (val selected = selectedTab) {
                is ImportTabV2.Image -> selected.copy(selectedImage = null)
                else -> tabs.filterIsInstance<ImportTabV2.Image>()
                    .firstOrNull() ?: selected
            }
            copy(tabs = updatedTabs, selectedTab = updatedSelected)
        }
    }

    private fun parseCsvToWordItems(csvText: String): List<ExtractedWordItem> {
        return csvText.trim()
            .split(Regex("[;\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapIndexedNotNull { index, line ->
                val parts = line.split(",", limit = 3).map { it.trim() }
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    ExtractedWordItem(
                        id = index,
                        word = parts[0],
                        translation = parts[1],
                        description = if (parts.size > 2) parts[2] else "",
                    )
                } else null
            }
    }
}
