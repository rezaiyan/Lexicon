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
) : BaseViewModel<ImportUiState, ImportEvent>() {

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
                    emitEffect(ImportEvent.WordAddedSuccessfully(count))
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
                    updateState {
                        copy(
                            textInputState = textInputState.copy(
                                isEnabled = true,
                                errorMessage = error.message ?: "Failed to add word"
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
                            emitEffect(ImportEvent.ImageImportSuccessful(result.count))
                        }

                        is ImportImageResult.Error -> {
                            clearSelectedImage()
                            updateState {
                                copy(imageImportState = ImageImportState.Error(result.message))
                            }
                            emitEffect(ImportEvent.Error(result.message))
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
                    delay(1500)
                    withContext(Dispatchers.Default) {
                        importViaFileUseCase(
                            pendingAction.content,
                            pendingAction.fileName,
                            sourceLanguage,
                            targetLanguage
                        ).fold(
                            onSuccess = { count ->
                                updateState {
                                    copy(fileImportState = ImportFileState.Success(count))
                                }
                                emitEffect(ImportEvent.FileImportSuccessful(count))
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Import failed"
                                updateState {
                                    copy(fileImportState = ImportFileState.Error(message))
                                }
                                emitEffect(ImportEvent.Error(message))
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
