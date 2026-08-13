package me.ash.reader.ui.page.home.feeds

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.domain.model.group.Group
import me.ash.reader.ui.component.base.RYSelectionChip
import me.ash.reader.ui.component.base.Subtitle

@Composable
fun FeedOptionView(
    modifier: Modifier = Modifier,
    link: String = "",
    groups: List<Group> = emptyList(),
    selectedAllowNotificationPreset: Boolean = false,
    selectedParseFullContentPreset: Boolean = false,
    selectedOpenInBrowserPreset: Boolean = false,
    isMoveToGroup: Boolean = false,
    showGroup: Boolean = true,
    showUnsubscribe: Boolean = true,
    notSubscribeMode: Boolean = false,
    selectedGroupId: String = "",
    allowNotificationPresetOnClick: () -> Unit = {},
    parseFullContentPresetOnClick: () -> Unit = {},
    openInBrowserPresetOnClick: () -> Unit = {},
    clearArticlesOnClick: () -> Unit = {},
    unsubscribeOnClick: () -> Unit = {},
    onGroupClick: (groupId: String) -> Unit = {},
    onAddNewGroup: () -> Unit = {},
    onFeedUrlClick: () -> Unit = {},
    onFeedUrlLongClick: () -> Unit = {},
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        EditableUrl(text = link, onClick = onFeedUrlClick, onLongClick = onFeedUrlLongClick)
        Spacer(modifier = Modifier.height(26.dp))

        Preset(
            showUnsubscribe = showUnsubscribe,
            notSubscribeMode = notSubscribeMode,
            clearArticlesOnClick = clearArticlesOnClick,
            unsubscribeOnClick = unsubscribeOnClick,
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditableUrl(text: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            modifier =
                Modifier.clip(MaterialTheme.shapes.small)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            text = text,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Preset(
    showUnsubscribe: Boolean = true,
    notSubscribeMode: Boolean = false,
    clearArticlesOnClick: () -> Unit = {},
    unsubscribeOnClick: () -> Unit = {},
) {
    if (notSubscribeMode) {
        Subtitle(text = stringResource(R.string.preset))
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            RYSelectionChip(
                modifier = Modifier,
                content = stringResource(R.string.clear_articles),
                selected = false,
            ) {
                clearArticlesOnClick()
            }
            if (showUnsubscribe) {
                RYSelectionChip(
                    modifier = Modifier,
                    content = stringResource(R.string.unsubscribe),
                    selected = false,
                ) {
                    unsubscribeOnClick()
                }
            }
        }
    }
}
