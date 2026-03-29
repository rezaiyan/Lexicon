package feature.leaderboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.common.UiState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import feature.leaderboard.LeaderboardViewModel
import feature.leaderboard.model.LeaderboardUiData
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.content_description_back
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun LeaderboardScreen(
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<LeaderboardViewModel>()
    val uiState by viewModel.state()

    LexiconColumn(
        title = stringResource(Res.string.leaderboard),
        showNavigationIcon = true,
        navigationIconContentDescription = stringResource(Res.string.content_description_back),
        onNavigationClick = onDismiss,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(Res.string.refresh),
            onClick = viewModel::refresh,
            size = Theme.dimensions.iconSize
        ),
        scrollable = false
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Loaded -> {
                LeaderboardContent(data = state.value)
            }
        }
    }
}

@Composable
private fun LeaderboardContent(data: LeaderboardUiData) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            itemsIndexed(data.entries, key = { _, entry -> entry.rank }) { _, entry ->
                LeaderboardEntryCard(entry = entry)
            }
        }

        if (data.userEntry != null) {
            UserRankingFooter(entry = data.userEntry)
        }
    }
}
