package feature.support.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.LoadingScreen
import components.scaffold.LexiconColumn
import domain.support.model.SupportSender
import feature.support.SupportViewModel
import feature.support.model.SupportState
import feature.support.model.SupportTimelineItem
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.content_description_close
import lexicon.resources.generated.resources.support_empty_subtitle
import lexicon.resources.generated.resources.support_empty_title
import lexicon.resources.generated.resources.support_jump_to_latest
import lexicon.resources.generated.resources.support_offline
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
    val focusManager = LocalFocusManager.current
    // The sheet already pads for the navigation bar; imePadding adds only what the keyboard covers
    // beyond it, so the composer rides on top of the keyboard and the list shrinks above it.
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        AnimatedVisibility(
            visible = state.isOffline,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            OfflineBanner()
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Tapping the conversation puts the keyboard away.
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.fillMaxSize().widthIn(max = Theme.dimensions.contentMaxWidth)
            when {
                state.isLoading -> LoadingScreen()
                state.timeline.isEmpty() -> EmptyConversation(contentModifier)
                else -> Conversation(timeline = state.timeline, onRetry = onRetry, modifier = contentModifier)
            }
        }
        Composer(
            draft = state.draft,
            canSend = state.canSend,
            remainingChars = state.remainingChars,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
        )
    }
}

@Composable
private fun Conversation(
    timeline: List<SupportTimelineItem>,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Newest at the bottom; reverseLayout keeps the list pinned there as messages arrive and as
    // the keyboard opens.
    val newestFirst = remember(timeline) { timeline.asReversed() }
    val showJumpToLatest by remember { derivedStateOf { listState.firstVisibleItemIndex > JUMP_THRESHOLD } }

    FollowNewestMessage(newest = timeline.lastOrNull(), listState = listState)

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(
                start = Theme.spacing.sm,
                end = Theme.spacing.screenGutter,
                top = Theme.spacing.xs,
                bottom = Theme.spacing.sm,
            ),
        ) {
            items(
                items = newestFirst,
                key = { it.key },
                contentType = { it::class },
            ) { item ->
                when (item) {
                    is SupportTimelineItem.DayHeader -> DayHeader(day = item.day, modifier = Modifier.animateItem())
                    is SupportTimelineItem.Bubble -> MessageRow(
                        bubble = item,
                        onRetry = onRetry,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showJumpToLatest,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(Theme.spacing.md),
        ) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.support_jump_to_latest),
                )
            }
        }
    }
}

/**
 * Scrolls to a newly arrived message when the reader is already near the bottom, and always for
 * the user's own message, so sending never leaves the new bubble off screen.
 */
@Composable
private fun FollowNewestMessage(newest: SupportTimelineItem?, listState: LazyListState) {
    val newestKey = newest?.key
    val fromUser = (newest as? SupportTimelineItem.Bubble)?.message?.sender == SupportSender.USER
    LaunchedEffect(newestKey) {
        if (newestKey != null && (fromUser || listState.firstVisibleItemIndex <= JUMP_THRESHOLD)) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun EmptyConversation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.xl, vertical = Theme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSizeMassive + Theme.spacing.xl)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SupportAgent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
            )
        }
        Text(
            text = stringResource(Res.string.support_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Theme.spacing.xs),
        )
        Text(
            text = stringResource(Res.string.support_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.xs)
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(Theme.colors.warningContainer)
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = Theme.colors.onWarningContainer,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Text(
            text = stringResource(Res.string.support_offline),
            style = MaterialTheme.typography.labelMedium,
            color = Theme.colors.onWarningContainer,
        )
    }
}

/** Items scrolled past before the list stops following new messages and offers a jump back. */
private const val JUMP_THRESHOLD = 2
