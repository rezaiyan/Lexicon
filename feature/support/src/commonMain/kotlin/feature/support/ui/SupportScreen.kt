package feature.support.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import components.EmptyScreen
import components.LoadingScreen
import components.scaffold.LexiconColumn
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import feature.support.SupportViewModel
import feature.support.model.SupportState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.content_description_close
import lexicon.resources.generated.resources.support_empty_subtitle
import lexicon.resources.generated.resources.support_empty_title
import lexicon.resources.generated.resources.support_input_placeholder
import lexicon.resources.generated.resources.support_not_sent_retry
import lexicon.resources.generated.resources.support_offline
import lexicon.resources.generated.resources.support_send
import lexicon.resources.generated.resources.support_sending
import lexicon.resources.generated.resources.support_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun SupportScreen(onDismiss: () -> Unit) {
    val viewModel = koinViewModel<SupportViewModel>()
    val state by viewModel.state()

    LexiconColumn(
        title = stringResource(Res.string.support_title),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        navigationIconContentDescription = stringResource(Res.string.content_description_close),
        onNavigationClick = onDismiss,
        scrollable = false,
    ) {
        SupportContent(
            state = state,
            onDraftChanged = viewModel::onDraftChanged,
            onSend = viewModel::send,
            onRetry = viewModel::retry,
        )
    }
}

@Composable
private fun SupportContent(
    state: SupportState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        if (state.isOffline) OfflineBanner()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> LoadingScreen()
                state.messages.isEmpty() -> EmptyScreen(
                    title = stringResource(Res.string.support_empty_title),
                    subtitle = stringResource(Res.string.support_empty_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                else -> MessageList(messages = state.messages, onRetry = onRetry)
            }
        }
        Composer(draft = state.draft, canSend = state.canSend, onDraftChanged = onDraftChanged, onSend = onSend)
    }
}

@Composable
private fun MessageList(messages: List<SupportMessage>, onRetry: (String) -> Unit) {
    // Newest at the bottom; reverseLayout keeps the list pinned there as messages arrive.
    val newestFirst = remember(messages) { messages.asReversed() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        items(newestFirst, key = { it.id }) { message ->
            MessageBubble(
                body = message.body,
                fromUser = message.sender == SupportSender.USER,
                status = message.status,
                onRetry = { onRetry(message.id) },
            )
        }
    }
}

@Composable
private fun MessageBubble(
    body: String,
    fromUser: Boolean,
    status: SupportDeliveryStatus,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val corner = Theme.shapes.large
    val tail = Theme.shapes.extraSmall
    // The gap on the far side keeps bubbles from spanning the whole row.
    val farSide = Theme.spacing.xxxl
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (fromUser) farSide else Theme.spacing.none,
                end = if (fromUser) Theme.spacing.none else farSide,
            ),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = if (fromUser) colors.onPrimaryContainer else colors.onSurfaceVariant,
            modifier = Modifier
                .background(
                    color = if (fromUser) colors.primaryContainer else colors.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = corner,
                        topEnd = corner,
                        bottomStart = if (fromUser) corner else tail,
                        bottomEnd = if (fromUser) tail else corner,
                    ),
                )
                .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
        )
        when (status) {
            SupportDeliveryStatus.SENDING -> StatusLabel(stringResource(Res.string.support_sending))
            SupportDeliveryStatus.FAILED -> StatusLabel(
                text = stringResource(Res.string.support_not_sent_retry),
                isError = true,
                modifier = Modifier.clickable(onClick = onRetry),
            )
            SupportDeliveryStatus.SENT -> Unit
        }
    }
}

@Composable
private fun StatusLabel(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = Theme.spacing.xxs, start = Theme.spacing.xxs, end = Theme.spacing.xxs),
    )
}

@Composable
private fun OfflineBanner() {
    Text(
        text = stringResource(Res.string.support_offline),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = Theme.spacing.xs, horizontal = Theme.spacing.screenGutter),
    )
}

@Composable
private fun Composer(
    draft: String,
    canSend: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.xs),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(Res.string.support_input_placeholder)) },
            maxLines = COMPOSER_MAX_LINES,
            shape = RoundedCornerShape(Theme.shapes.extraLarge),
        )
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.size(Theme.dimensions.inputFieldHeight),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(Res.string.support_send),
            )
        }
    }
}

private const val COMPOSER_MAX_LINES = 5
