package presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import components.snackbar.showError
import components.snackbar.showSuccess
import events.VocabularyEffect
import org.jetbrains.compose.resources.stringResource
import feature.words.VocabularyViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.image_import_failed_generic
import lexicon.resources.generated.resources.import_failed_generic
import lexicon.resources.generated.resources.please_login_for_ai
import lexicon.resources.generated.resources.review_complete
import lexicon.resources.generated.resources.review_complete_message
import lexicon.resources.generated.resources.success_imported_words
import lexicon.resources.generated.resources.word_deleted

@Composable
internal fun HandleVocabularyEffects(
    vocabularyViewModel: VocabularyViewModel,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val importFailedGeneric = stringResource(Res.string.import_failed_generic)
    val imageImportFailed = stringResource(Res.string.image_import_failed_generic)
    val pleaseLoginForAi = stringResource(Res.string.please_login_for_ai)
    val successImportedWordsFormat = stringResource(Res.string.success_imported_words)

    val reviewComplete = stringResource(Res.string.review_complete)
    val reviewCompleteMessage = stringResource(Res.string.review_complete_message)
    val wordDeleted = stringResource(Res.string.word_deleted)

    LaunchedEffect(Unit) {
        vocabularyViewModel.effects.collect { event ->
            when (event) {
                is VocabularyEffect.ImportSuccess -> {
                    val pattern = "%1" + '$' + "d"
                    val message =
                        successImportedWordsFormat.replace(pattern, event.count.toString())
                    snackbarHostState.showSuccess(message)
                }

                is VocabularyEffect.ImportError -> {
                    snackbarHostState.showError(event.message.ifEmpty { importFailedGeneric })
                }

                is VocabularyEffect.ImageImportSuccess -> {}

                is VocabularyEffect.ImageImportError -> {
                    snackbarHostState.showError(imageImportFailed)
                }

                is VocabularyEffect.ImageImportRequiresLogin -> {
                    snackbarHostState.showSnackbar(pleaseLoginForAi)
                }

                is VocabularyEffect.ReviewSessionComplete -> {
                    snackbarHostState.showSuccess("$reviewComplete\n$reviewCompleteMessage")
                }

                is VocabularyEffect.WordDeleted -> {
                    snackbarHostState.showSuccess(wordDeleted)
                }
            }
        }
    }
}
