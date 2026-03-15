package presentation.ui.components.imports

import androidx.lifecycle.viewModelScope
import domain.ai.usecase.ImportFromImageUseCase
import domain.ai.usecase.ImportImageResult
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import core.common.fold
import core.common.getOrDefault
import domain.settings.usecase.GetCurrentLanguageUseCase
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

class ImportViewModel(
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val importWordsUseCase: ImportWordsUseCase,
    private val importViaFileUseCase: ImportViaFileUseCase,
    private val importFromImageUseCase: ImportFromImageUseCase,
    private val userManager: IUserManager,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
    private val getSourceLanguageUseCase: GetSourceLanguageUseCase,
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
                currentState.sourceLanguage,
                currentState.targetLanguage
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

    fun importImage() {
        val imageTab = when (val selected = currentState.selectedTab) {
            is ImportTabV2.Image -> selected
            else -> currentState.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        } ?: return

        val imageBytes = imageTab.selectedImage ?: return

        viewModelScope.launch {
            updateState { copy(imageImportState = ImageImportState.Loading) }
            val trace = performanceTracer.startTrace("import_image_processing")

            withContext(Dispatchers.Default) {
                importFromImageUseCase(
                    imageBytes = imageBytes,
                    extractWords = true,
                    extractSentences = true
                ).collect { result ->
                    when (result) {
                        is ImportImageResult.Loading -> {
                            updateState { copy(imageImportState = ImageImportState.Loading) }
                        }

                        is ImportImageResult.Success -> {
                            clearSelectedImage()
                            updateState {
                                copy(imageImportState = ImageImportState.Success(result.count))
                            }
                            performanceTracer.putMetric(trace, "words_imported", result.count.toLong())
                            performanceTracer.stopTrace(trace)
                            emitEffect(ImportEffect.ImageImportSuccessful(result.count))
                        }

                        is ImportImageResult.Error -> {
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
                            sourceLanguage,
                            targetLanguage
                        ).fold(
                            onSuccess = { count ->
                                if (count == 0) {
                                    val message = "No words found in this file. Use the format: word,translation (one pair per line)."
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
}
