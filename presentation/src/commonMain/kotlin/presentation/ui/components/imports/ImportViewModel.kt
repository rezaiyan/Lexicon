package presentation.ui.components.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.ai.usecase.ImportFromImageUseCase
import domain.ai.usecase.ImportImageResult
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import core.common.fold
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.usecase.ImportViaFileUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import presentation.model.ImageImportState
import utils.Language

class ImportViewModel(
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val importWordsUseCase: ImportWordsUseCase,
    private val importViaFileUseCase: ImportViaFileUseCase,
    private val importFromImageUseCase: ImportFromImageUseCase,
    private val userManager: IUserManager,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val targetLanguage = getCurrentLanguageUseCase()
            _state.update { it.copy(targetLanguage = targetLanguage) }
        }
        observeFeatureAccess()
    }

    @Suppress("OPT_IN_USAGE")
    private fun observeFeatureAccess() {
        viewModelScope.launch {
            userManager.observeUser()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(_state.value.tabs.filter { it !is ImportTabV2.Image })
                    } else {
                        getFeatureAccessUseCase.invoke()
                            .map { featureAccess ->
                                val hasPremiumAccess = featureAccess.userAccess.hasPremiumAccess
                                val currentTabs = _state.value.tabs.toMutableList()
                                val currentImageTab =
                                    currentTabs.firstOrNull { it is ImportTabV2.Image }
                                if (hasPremiumAccess && currentImageTab == null) {
                                    currentTabs.add(ImportTabV2.Image())
                                } else if (!hasPremiumAccess && currentImageTab != null) {
                                    currentTabs.remove(currentImageTab)
                                }
                                currentTabs.toList()
                            }
                            .catch { emit(_state.value.tabs) }
                    }
                }
                .collect { tabs ->
                    _state.update { it.copy(tabs = tabs) }
                }
        }
    }

    fun selectTab(selectedTab: ImportTabV2) {
        val matchingTab = when (selectedTab) {
            is ImportTabV2.Text -> _state.value.tabs.filterIsInstance<ImportTabV2.Text>().firstOrNull()
            is ImportTabV2.File -> _state.value.tabs.filterIsInstance<ImportTabV2.File>().firstOrNull()
            is ImportTabV2.Image -> _state.value.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        }
        if (matchingTab != null) {
            _state.update { it.copy(selectedTab = matchingTab) }
        }
    }

    fun updateWord(word: String) {
        _state.update {
            it.copy(
                textInputState = it.textInputState.copy(
                    word = word,
                    errorMessage = null
                )
            )
        }
    }

    fun updateTranslation(translation: String) {
        _state.update {
            it.copy(
                textInputState = it.textInputState.copy(
                    translation = translation,
                    errorMessage = null
                )
            )
        }
    }

    fun updateDescription(description: String) {
        _state.update {
            it.copy(
                textInputState = it.textInputState.copy(
                    description = description,
                    errorMessage = null
                )
            )
        }
    }

    fun addWord() {
        val textState = _state.value.textInputState
        val word = textState.word.trim().replace(",", " ")
        val translation = textState.translation.trim().replace(",", " ")
        val description = textState.description.trim().replace(",", " ")

        if (word.isBlank() || translation.isBlank()) return

        val csvLine = if (description.isNotBlank()) {
            "$word,$translation,$description"
        } else {
            "$word,$translation"
        }

        _state.update {
            it.copy(
                textInputState = textState.copy(isEnabled = false, errorMessage = null)
            )
        }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                importWordsUseCase.execute(
                    csvLine,
                    _state.value.sourceLanguage,
                    _state.value.targetLanguage
                ).fold(
                    onSuccess = { count ->
                        val newCount = _state.value.textInputState.wordsAddedCount + count
                        _state.update {
                            it.copy(
                                textInputState = TextInputState(
                                    wordsAddedCount = newCount,
                                    showSuccessIndicator = true,
                                )
                            )
                        }
                        _events.send(ImportEvent.WordAddedSuccessfully(count))
                        delay(1500)
                        _state.update {
                            it.copy(
                                textInputState = it.textInputState.copy(
                                    showSuccessIndicator = false
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                textInputState = it.textInputState.copy(
                                    isEnabled = true,
                                    errorMessage = error.message ?: "Failed to add word"
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    fun selectImage(imageBytes: ByteArray) {
        _state.update { currentState ->
            val updatedTabs = currentState.tabs.map { tab ->
                if (tab is ImportTabV2.Image) tab.copy(selectedImage = imageBytes) else tab
            }
            val updatedSelected = when (val selected = currentState.selectedTab) {
                is ImportTabV2.Image -> selected.copy(selectedImage = imageBytes)
                else -> currentState.tabs.filterIsInstance<ImportTabV2.Image>()
                    .firstOrNull() ?: selected
            }
            currentState.copy(tabs = updatedTabs, selectedTab = updatedSelected)
        }
    }

    fun updateExtractionOptions(options: List<ExtractionOption>) {
        _state.update { currentState ->
            val updatedTabs = currentState.tabs.map { tab ->
                if (tab is ImportTabV2.Image) tab.copy(extractionOption = options) else tab
            }
            val updatedSelected = when (val selected = currentState.selectedTab) {
                is ImportTabV2.Image -> selected.copy(extractionOption = options)
                else -> currentState.tabs.filterIsInstance<ImportTabV2.Image>()
                    .firstOrNull() ?: selected
            }
            currentState.copy(tabs = updatedTabs, selectedTab = updatedSelected)
        }
    }

    fun importImage() {
        val imageTab = when (val selected = _state.value.selectedTab) {
            is ImportTabV2.Image -> selected
            else -> _state.value.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        } ?: return

        val imageBytes = imageTab.selectedImage ?: return
        val extractWords = imageTab.extractionOption.contains(ExtractionOption.Word)
        val extractSentences = imageTab.extractionOption.contains(ExtractionOption.Sentence)
        if (!extractWords && !extractSentences) return

        viewModelScope.launch {
            _state.update { it.copy(imageImportState = ImageImportState.Loading) }

            withContext(Dispatchers.Default) {
                importFromImageUseCase(
                    imageBytes = imageBytes,
                    extractWords = extractWords,
                    extractSentences = extractSentences
                ).collect { result ->
                    when (result) {
                        is ImportImageResult.Loading -> {
                            _state.update { it.copy(imageImportState = ImageImportState.Loading) }
                        }

                        is ImportImageResult.Success -> {
                            clearSelectedImage()
                            _state.update {
                                it.copy(imageImportState = ImageImportState.Success(result.count))
                            }
                            _events.send(ImportEvent.ImageImportSuccessful(result.count))
                        }

                        is ImportImageResult.Error -> {
                            clearSelectedImage()
                            _state.update {
                                it.copy(imageImportState = ImageImportState.Error(result.message))
                            }
                            _events.send(ImportEvent.Error(result.message))
                        }
                    }
                }
            }
        }
    }

    fun importFile(fileContent: String, fileName: String? = null) {
        _state.update {
            it.copy(
                showLanguageConfirmation = true,
                pendingImportAction = PendingImportAction.File(fileContent, fileName)
            )
        }
    }

    fun selectSourceLanguage(language: Language) {
        _state.update { it.copy(sourceLanguage = language) }
    }

    fun selectTargetLanguage(language: Language) {
        _state.update { it.copy(targetLanguage = language) }
    }

    fun confirmImport() {
        val pendingAction = _state.value.pendingImportAction ?: return
        val sourceLanguage = _state.value.sourceLanguage
        val targetLanguage = _state.value.targetLanguage

        _state.update {
            it.copy(
                showLanguageConfirmation = false,
                pendingImportAction = null
            )
        }

        when (pendingAction) {
            is PendingImportAction.File -> {
                viewModelScope.launch {
                    _state.update { it.copy(fileImportState = ImportFileState.Loading) }
                    delay(1500)
                    withContext(Dispatchers.Default) {
                        importViaFileUseCase(
                            pendingAction.content,
                            pendingAction.fileName,
                            sourceLanguage,
                            targetLanguage
                        ).fold(
                            onSuccess = { count ->
                                _state.update {
                                    it.copy(fileImportState = ImportFileState.Success(count))
                                }
                                _events.send(ImportEvent.FileImportSuccessful(count))
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Import failed"
                                _state.update {
                                    it.copy(fileImportState = ImportFileState.Error(message))
                                }
                                _events.send(ImportEvent.Error(message))
                            }
                        )
                    }
                }
            }
        }
    }

    fun dismissLanguageConfirmation() {
        _state.update {
            it.copy(
                showLanguageConfirmation = false,
                pendingImportAction = null
            )
        }
    }

    fun clearSelectedImage() {
        _state.update { currentState ->
            val updatedTabs = currentState.tabs.map { tab ->
                if (tab is ImportTabV2.Image) tab.copy(selectedImage = null) else tab
            }
            val updatedSelected = when (val selected = currentState.selectedTab) {
                is ImportTabV2.Image -> selected.copy(selectedImage = null)
                else -> currentState.tabs.filterIsInstance<ImportTabV2.Image>()
                    .firstOrNull() ?: selected
            }
            currentState.copy(tabs = updatedTabs, selectedTab = updatedSelected)
        }
    }
}
