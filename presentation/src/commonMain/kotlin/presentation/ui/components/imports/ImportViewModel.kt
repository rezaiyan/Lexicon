package presentation.ui.components.imports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import domain.ai.usecase.ImportFromImageUseCase
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.ai.usecase.ImportImageResult
import domain.auth.manager.IUserManager
import domain.common.fold
import domain.auth.model.AuthUser
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.usecase.ImportViaFileUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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

    private var _state by mutableStateOf(ImportUiState())

    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val targetLanguage = getCurrentLanguageUseCase()
            _state = _state.copy(targetLanguage = targetLanguage)
        }
    }

    @Composable
    private fun featureAccessState(user: AuthUser?): State<List<ImportTabV2>> {
        return produceState(
            initialValue = _state.tabs,
            keys = arrayOf(_state.fileImportState, user)
        ) {
            val currentTabs = _state.tabs.toMutableList()

            if (user == null) {
                value = currentTabs.filter { it !is ImportTabV2.Image }
                return@produceState
            }
            getFeatureAccessUseCase.invoke()
                .map { featureAccess ->
                    val hasPremiumAccess = featureAccess.userAccess.hasPremiumAccess

                    val currentImageTab = _state.tabs.firstOrNull { it is ImportTabV2.Image }
                    if (hasPremiumAccess && currentImageTab == null) {
                        currentTabs.add(ImportTabV2.Image())
                    } else if (!hasPremiumAccess && currentImageTab != null) {
                        currentTabs.remove(currentImageTab)
                    }
                    value = currentTabs
                }.catch {
                    value = _state.tabs
                }.collect()
        }
    }

    @Composable
    fun state(): ImportUiState {
        val user by userManager.observeUser().collectAsStateWithLifecycle(null)
        val importTabs by featureAccessState(user)

        LaunchedEffect(importTabs) {
            _state = _state.copy(tabs = importTabs)
        }

        return _state
    }


    fun selectTab(selectedTab: ImportTabV2) {
        val matchingTab = when (selectedTab) {
            is ImportTabV2.Text -> _state.tabs.filterIsInstance<ImportTabV2.Text>().firstOrNull()
            is ImportTabV2.File -> _state.tabs.filterIsInstance<ImportTabV2.File>().firstOrNull()
            is ImportTabV2.Image -> _state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        }
        if (matchingTab != null) {
            _state = _state.copy(selectedTab = matchingTab)
        }
    }

    fun updateWord(word: String) {
        _state = _state.copy(
            textInputState = _state.textInputState.copy(
                word = word,
                errorMessage = null
            )
        )
    }

    fun updateTranslation(translation: String) {
        _state = _state.copy(
            textInputState = _state.textInputState.copy(
                translation = translation,
                errorMessage = null
            )
        )
    }

    fun updateDescription(description: String) {
        _state = _state.copy(
            textInputState = _state.textInputState.copy(
                description = description,
                errorMessage = null
            )
        )
    }

    fun addWord() {
        val textState = _state.textInputState
        val word = textState.word.trim().replace(",", " ")
        val translation = textState.translation.trim().replace(",", " ")
        val description = textState.description.trim().replace(",", " ")

        if (word.isBlank() || translation.isBlank()) return

        val csvLine = if (description.isNotBlank()) {
            "$word,$translation,$description"
        } else {
            "$word,$translation"
        }

        _state = _state.copy(
            textInputState = textState.copy(isEnabled = false, errorMessage = null)
        )

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                importWordsUseCase.execute(
                    csvLine,
                    _state.sourceLanguage,
                    _state.targetLanguage
                ).fold(
                    onSuccess = { count ->
                        val newCount = _state.textInputState.wordsAddedCount + count
                        _state = _state.copy(
                            textInputState = TextInputState(
                                wordsAddedCount = newCount,
                                showSuccessIndicator = true,
                            )
                        )
                        _events.send(ImportEvent.WordAddedSuccessfully(count))
                        delay(1500)
                        _state = _state.copy(
                            textInputState = _state.textInputState.copy(
                                showSuccessIndicator = false
                            )
                        )
                    },
                    onFailure = { error ->
                        _state = _state.copy(
                            textInputState = _state.textInputState.copy(
                                isEnabled = true,
                                errorMessage = error.message ?: "Failed to add word"
                            )
                        )
                    }
                )
            }
        }
    }

    fun selectImage(imageBytes: ByteArray) {
        val updatedTabs = _state.tabs.map { tab ->
            if (tab is ImportTabV2.Image) {
                tab.copy(selectedImage = imageBytes)
            } else tab
        }
        val updatedSelected = when (val selected = _state.selectedTab) {
            is ImportTabV2.Image -> selected.copy(selectedImage = imageBytes)
            else -> _state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull() ?: selected
        }
        _state = _state.copy(tabs = updatedTabs, selectedTab = updatedSelected)
    }

    fun updateExtractionOptions(options: List<ExtractionOption>) {
        val updatedTabs = _state.tabs.map { tab ->
            if (tab is ImportTabV2.Image) {
                tab.copy(extractionOption = options)
            } else tab
        }
        val updatedSelected = when (val selected = _state.selectedTab) {
            is ImportTabV2.Image -> selected.copy(extractionOption = options)
            else -> _state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull() ?: selected
        }
        _state = _state.copy(tabs = updatedTabs, selectedTab = updatedSelected)
    }

    fun importImage() {
        val imageTab = when (val selected = _state.selectedTab) {
            is ImportTabV2.Image -> selected
            else -> _state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        } ?: return

        val imageBytes = imageTab.selectedImage ?: return
        val extractWords = imageTab.extractionOption.contains(ExtractionOption.Word)
        val extractSentences = imageTab.extractionOption.contains(ExtractionOption.Sentence)
        if (!extractWords && !extractSentences) return

        viewModelScope.launch {
            // set loading
            _state = _state.copy(imageImportState = ImageImportState.Loading)

            withContext(Dispatchers.Default) {
                importFromImageUseCase(
                    imageBytes = imageBytes,
                    extractWords = extractWords,
                    extractSentences = extractSentences
                ).collect { result ->
                    when (result) {
                        is ImportImageResult.Loading -> {
                            _state = _state.copy(imageImportState = ImageImportState.Loading)
                        }

                        is ImportImageResult.Success -> {
                            clearSelectedImage()
                            val imageImportState = ImageImportState.Success(result.count)
                            _state = _state.copy(imageImportState = imageImportState)
                            _events.send(ImportEvent.ImageImportSuccessful(result.count))
                        }

                        is ImportImageResult.Error -> {
                            clearSelectedImage()
                            val imageImportState = ImageImportState.Error(result.message)
                            _state = _state.copy(imageImportState = imageImportState)
                            _events.send(ImportEvent.Error(result.message))
                        }
                    }
                }
            }
        }
    }

    fun importFile(fileContent: String, fileName: String? = null) {
        _state = _state.copy(
            showLanguageConfirmation = true,
            pendingImportAction = PendingImportAction.File(fileContent, fileName)
        )
    }

    fun selectSourceLanguage(language: Language) {
        _state = _state.copy(sourceLanguage = language)
    }

    fun selectTargetLanguage(language: Language) {
        _state = _state.copy(targetLanguage = language)
    }

    fun confirmImport() {
        val pendingAction = _state.pendingImportAction ?: return
        val sourceLanguage = _state.sourceLanguage
        val targetLanguage = _state.targetLanguage

        _state = _state.copy(
            showLanguageConfirmation = false,
            pendingImportAction = null
        )

        when (pendingAction) {
            is PendingImportAction.File -> {
                viewModelScope.launch {
                    _state = _state.copy(fileImportState = ImportFileState.Loading)
                    delay(1500)
                    withContext(Dispatchers.Default) {
                        importViaFileUseCase(
                            pendingAction.content,
                            pendingAction.fileName,
                            sourceLanguage,
                            targetLanguage
                        ).fold(
                            onSuccess = { count ->
                                _state = _state.copy(
                                    fileImportState = ImportFileState.Success(count)
                                )
                                _events.send(ImportEvent.FileImportSuccessful(count))
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Import failed"
                                _state = _state.copy(
                                    fileImportState = ImportFileState.Error(message)
                                )
                                _events.send(ImportEvent.Error(message))
                            }
                        )
                    }
                }
            }
        }
    }

    fun dismissLanguageConfirmation() {
        _state = _state.copy(
            showLanguageConfirmation = false,
            pendingImportAction = null
        )
    }

    fun clearSelectedImage() {
        val updatedTabs = _state.tabs.map { tab ->
            if (tab is ImportTabV2.Image) {
                tab.copy(selectedImage = null)
            } else tab
        }
        val updatedSelected = when (val selected = _state.selectedTab) {
            is ImportTabV2.Image -> selected.copy(selectedImage = null)
            else -> _state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull() ?: selected
        }
        _state = _state.copy(tabs = updatedTabs, selectedTab = updatedSelected)
    }
}
