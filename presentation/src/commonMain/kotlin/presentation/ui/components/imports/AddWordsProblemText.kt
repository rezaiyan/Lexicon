package presentation.ui.components.imports

import androidx.compose.runtime.Composable
import feature.addwords.model.AddWordsProblem
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_problem_empty_file
import lexicon.resources.generated.resources.add_words_problem_file_too_large
import lexicon.resources.generated.resources.add_words_problem_generic
import lexicon.resources.generated.resources.add_words_problem_image_too_large
import lexicon.resources.generated.resources.add_words_problem_image_unreadable
import lexicon.resources.generated.resources.add_words_problem_missing_term
import lexicon.resources.generated.resources.add_words_problem_missing_translation
import lexicon.resources.generated.resources.add_words_problem_nothing_in_photo
import lexicon.resources.generated.resources.add_words_problem_nothing_suggested
import lexicon.resources.generated.resources.add_words_problem_premium
import lexicon.resources.generated.resources.add_words_problem_rate_limited
import lexicon.resources.generated.resources.add_words_problem_too_long
import lexicon.resources.generated.resources.add_words_problem_unsupported_file
import lexicon.resources.generated.resources.import_error_file_empty
import lexicon.resources.generated.resources.import_error_network
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val AddWordsProblem.message: StringResource
    get() = when (this) {
        AddWordsProblem.Offline -> Res.string.import_error_network
        AddWordsProblem.PremiumRequired -> Res.string.add_words_problem_premium
        AddWordsProblem.RateLimited -> Res.string.add_words_problem_rate_limited
        AddWordsProblem.FileTooLarge -> Res.string.add_words_problem_file_too_large
        AddWordsProblem.UnsupportedFile -> Res.string.add_words_problem_unsupported_file
        AddWordsProblem.EmptyFile -> Res.string.add_words_problem_empty_file
        AddWordsProblem.NothingInFile -> Res.string.import_error_file_empty
        AddWordsProblem.NothingInPhoto -> Res.string.add_words_problem_nothing_in_photo
        AddWordsProblem.NothingSuggested -> Res.string.add_words_problem_nothing_suggested
        AddWordsProblem.ImageUnreadable -> Res.string.add_words_problem_image_unreadable
        AddWordsProblem.ImageTooLarge -> Res.string.add_words_problem_image_too_large
        AddWordsProblem.MissingTerm -> Res.string.add_words_problem_missing_term
        AddWordsProblem.MissingTranslation -> Res.string.add_words_problem_missing_translation
        AddWordsProblem.TooLong -> Res.string.add_words_problem_too_long
        AddWordsProblem.Generic -> Res.string.add_words_problem_generic
    }

@Composable
internal fun AddWordsProblem?.text(): String? = this?.let { stringResource(it.message) }
