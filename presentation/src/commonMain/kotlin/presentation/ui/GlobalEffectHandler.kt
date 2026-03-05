package presentation.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import events.VocabularyEffect
import org.jetbrains.compose.resources.stringResource
import presentation.viewmodel.VocabularyViewModel
import lexicon.resources.generated.resources.Res
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
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImportError -> {
                    val message = if (event.message.isNotEmpty()) {
                        "[Error] ${event.message}"
                    } else {
                        importFailedGeneric
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImageImportSuccess -> {}

                is VocabularyEffect.ImageImportError -> {
                    snackbarHostState.showSnackbar(
                        message = "Something wrong happened!",
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImageImportRequiresLogin -> {
                    snackbarHostState.showSnackbar(
                        message = pleaseLoginForAi,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ReviewSessionComplete -> {
                    snackbarHostState.showSnackbar(
                        message = "$reviewComplete\n$reviewCompleteMessage",
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.WordDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = wordDeleted,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}
