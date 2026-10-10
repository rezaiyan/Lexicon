package feature.insights.coach.ui

import androidx.compose.runtime.Composable
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.weekday_initial_1
import lexicon.resources.generated.resources.weekday_initial_2
import lexicon.resources.generated.resources.weekday_initial_3
import lexicon.resources.generated.resources.weekday_initial_4
import lexicon.resources.generated.resources.weekday_initial_5
import lexicon.resources.generated.resources.weekday_initial_6
import lexicon.resources.generated.resources.weekday_initial_7
import org.jetbrains.compose.resources.stringResource

/** Localized one-letter weekday label for an ISO day (1 = Monday … 7 = Sunday). */
@Composable
internal fun weekdayInitial(isoDay: Int): String = stringResource(
    when (isoDay) {
        1 -> Res.string.weekday_initial_1
        2 -> Res.string.weekday_initial_2
        3 -> Res.string.weekday_initial_3
        4 -> Res.string.weekday_initial_4
        5 -> Res.string.weekday_initial_5
        6 -> Res.string.weekday_initial_6
        else -> Res.string.weekday_initial_7
    }
)
