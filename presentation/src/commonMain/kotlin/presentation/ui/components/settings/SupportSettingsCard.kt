package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.support_settings_subtitle
import lexicon.resources.generated.resources.support_title
import lexicon.resources.generated.resources.support_unread_reply
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import theme.AppColors

/** @param hasUnreadReply Shows a red dot until the user reads support's reply in the chat. */
@Composable
fun SupportSettingsCard(hasUnreadReply: Boolean, onClick: () -> Unit) {
    val unreadDescription = stringResource(Res.string.support_unread_reply)
    SettingsCard(
        icon = Icons.Rounded.SupportAgent,
        title = stringResource(Res.string.support_title),
        subtitle = stringResource(Res.string.support_settings_subtitle),
        iconBackgroundColor = AppColors.settingsSupportIcon,
        onClick = onClick,
        trailingContent = if (hasUnreadReply) {
            { Badge(modifier = Modifier.semantics { contentDescription = unreadDescription }) }
        } else {
            null
        },
    )
}
