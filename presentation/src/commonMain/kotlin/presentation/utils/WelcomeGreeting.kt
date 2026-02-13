@file:OptIn(ExperimentalTime::class, InternalResourceApi::class)

package presentation.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.greeting_afternoon
import lexicon.resources.generated.resources.greeting_evening
import lexicon.resources.generated.resources.greeting_midnight
import lexicon.resources.generated.resources.greeting_morning
import lexicon.resources.generated.resources.greeting_night
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun getGreetingStringResource(): StringResource {
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = now.toLocalDateTime(timeZone)
    val hour = localDateTime.hour
    
    return when (hour) {
        in 0..4 -> Res.string.greeting_midnight
        in 5..11 -> Res.string.greeting_morning
        in 12..16 -> Res.string.greeting_afternoon
        in 17..20 -> Res.string.greeting_evening
        else -> Res.string.greeting_night
    }
}

@Composable
fun getTimeBasedGreeting(): String {
    return stringResource(getGreetingStringResource())
}

