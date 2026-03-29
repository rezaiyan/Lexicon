package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Switch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import components.EmptyScreen
import components.animation.staggeredFadeSlide
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.DialogIconState
import components.dialog.LexiconDialogContent
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import domain.tag.model.Tag
import events.OnEvents
import feature.words.TagManagerViewModel
import feature.words.model.TagManagerEffect
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.content_description_close
import lexicon.resources.generated.resources.create_tag
import lexicon.resources.generated.resources.delete_tag
import lexicon.resources.generated.resources.delete_tag_confirm
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.no_tags
import lexicon.resources.generated.resources.no_tags_subtitle
import lexicon.resources.generated.resources.rename_tag
import lexicon.resources.generated.resources.tag_manager
import lexicon.resources.generated.resources.tag_name_hint
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import overlay.LocalOverlayHost
import overlay.OverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import presentation.ui.LocalSnackbarHostState
import theme.AppColors
import theme.Theme

fun OverlayHost.showTagManagerScreen() {
    showFullScreen(
        tag = "tag-manager",
        properties = FullScreenProperties(dismissOnBackPress = false)
    ) { nav ->
        TagManagerContent(onDismiss = { nav.dismiss() })
    }
}

@Composable
private fun TagManagerContent(onDismiss: () -> Unit) {
    val viewModel = koinViewModel<TagManagerViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is TagManagerEffect.TagCreated -> Unit
            is TagManagerEffect.TagRenamed -> Unit
            is TagManagerEffect.TagDeleted -> Unit
            is TagManagerEffect.Error -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    LexiconColumn(
        title = stringResource(Res.string.tag_manager),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        navigationIconContentDescription = stringResource(Res.string.content_description_close),
        onNavigationClick = onDismiss,
        scrollable = false,
        topBarColor = TopBarColor.Background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingView()

                state.errorMessage != null -> ErrorView(message = state.errorMessage ?: "")

                state.tags.isEmpty() -> EmptyScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    title = stringResource(Res.string.no_tags),
                    subtitle = stringResource(Res.string.no_tags_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Label,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                            tint = AppColors.settingsTagManagerIcon
                        )
                    },
                    actionLabel = stringResource(Res.string.new_tag),
                    onAction = {
                        overlayHost.showSizeToFitBottomSheet(tag = "create-tag") { nav ->
                            CreateTagContent(
                                onConfirm = { name ->
                                    viewModel.createTag(name)
                                    nav.dismiss()
                                },
                                onDismiss = { nav.dismiss() }
                            )
                        }
                    }
                )

                else -> Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
                    ) {
                        itemsIndexed(state.tags, key = { _, tag -> tag.id }) { index, tag ->
                            TagListItem(
                                tag = tag,
                                onRename = {
                                    overlayHost.showSizeToFitBottomSheet(tag = "rename-tag") { nav ->
                                        RenameTagContent(
                                            currentName = tag.name,
                                            onConfirm = { name ->
                                                viewModel.renameTag(tag.id, name)
                                                nav.dismiss()
                                            },
                                            onDismiss = { nav.dismiss() }
                                        )
                                    }
                                },
                                onDelete = {
                                    overlayHost.showSizeToFitBottomSheet(tag = "delete-tag") { nav ->
                                        DeleteTagConfirmContent(
                                            onConfirm = {
                                                viewModel.deleteTag(tag.id)
                                                nav.dismiss()
                                            },
                                            onDismiss = { nav.dismiss() }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredFadeSlide(index)
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Theme.spacing.sm)
                                    .navigationBarsPadding()
                                    .padding(bottom = 80.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Switch(
                                    checked = !state.skipTagSelector,
                                    onCheckedChange = { viewModel.setSkipTagSelector(!it) },
                                )
                                Spacer(Modifier.width(Theme.spacing.md))
                                Column {
                                    Text(
                                        text = "Ask which tag to review",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(Theme.spacing.xxxs))
                                    Text(
                                        text = "Show tag selector before starting a review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            overlayHost.showSizeToFitBottomSheet(tag = "create-tag") { nav ->
                                CreateTagContent(
                                    onConfirm = { name ->
                                        viewModel.createTag(name)
                                        nav.dismiss()
                                    },
                                    onDismiss = { nav.dismiss() }
                                )
                            }
                        },
                        icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(Res.string.new_tag)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(Theme.spacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagListItem(
    tag: Tag,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.touchTargetSmall)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Label,
                    contentDescription = null,
                    tint = AppColors.settingsTagManagerIcon,
                    modifier = Modifier.size(Theme.dimensions.iconSize)
                )
            }

            Spacer(modifier = Modifier.width(Theme.spacing.sm))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.word_count_label, tag.wordCount.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.rename_tag),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_tag),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CreateTagContent(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LexiconDialogContent(
        modifier = Modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
        title = stringResource(Res.string.new_tag),
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.tag_name_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Theme.spacing.sm),
                singleLine = true,
                shape = RoundedCornerShape(Theme.shapes.medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.create_tag),
            onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
            enabled = name.isNotBlank()
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun RenameTagContent(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val focusManager = LocalFocusManager.current

    LexiconDialogContent(
        modifier = Modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
        title = stringResource(Res.string.rename_tag),
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.tag_name_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Theme.spacing.sm),
                singleLine = true,
                shape = RoundedCornerShape(Theme.shapes.medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.rename_tag),
            onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
            enabled = name.isNotBlank() && name != currentName
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun DeleteTagConfirmContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        iconState = DialogIconState.Icon(
            imageVector = Icons.Default.Warning,
            tint = MaterialTheme.colorScheme.error
        ),
        title = stringResource(Res.string.delete_tag),
        message = stringResource(Res.string.delete_tag_confirm),
        primaryButton = ButtonState(
            text = stringResource(Res.string.delete_tag),
            onClick = onConfirm,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}
