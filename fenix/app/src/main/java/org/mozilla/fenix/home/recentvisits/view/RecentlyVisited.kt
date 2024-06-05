/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentvisits.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.support.ktx.kotlin.trimmed
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ContextualMenu
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.MenuItem
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import org.mozilla.fenix.theme.FirefoxTheme
import kotlin.math.min

// Number of recently visited items per column.
private const val VISITS_PER_COLUMN = 3

private val itemRowHeight = 56.dp
private val contentPadding = 16.dp
private val imageSize = 24.dp
private val imageSpacer = 16.dp
private val textSpacer = 2.dp

/**
 * A list of recently visited items.
 *
 * @param recentVisits List of [RecentlyVisitedItem] to display.
 * @param menuItems List of [RecentVisitMenuItem] shown long clicking a [RecentlyVisitedItem].
 * @param backgroundColor The background [Color] of each item.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RecentlyVisited(
    recentVisits: List<RecentlyVisitedItem>,
    menuItems: List<RecentVisitMenuItem>,
    backgroundColor: Color = FirefoxTheme.colors.layer2,
    onRecentVisitClick: (RecentlyVisitedItem, Int) -> Unit = { _, _ -> },
) {
    val items = recentVisits.subList(0, min(recentVisits.size, VISITS_PER_COLUMN))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .semantics {
                testTagsAsResourceId = true
                testTag = "recent.visits"
            },
    ) {
        RecentlyVisitedCard(backgroundColor) {
            RecentlyVisitedColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = contentPadding),
                menuItems = menuItems,
                items = items,
                pageIndex = 0,
                onRecentVisitClick = onRecentVisitClick,
            )
        }
    }
}

@Composable
private fun RecentlyVisitedCard(backgroundColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = backgroundColor,
        elevation = 0.dp,
    ) {
        content()
    }
}

@Composable
private fun RecentlyVisitedColumn(
    modifier: Modifier,
    menuItems: List<RecentVisitMenuItem>,
    items: List<RecentlyVisitedItem>,
    pageIndex: Int,
    onRecentVisitClick: (RecentlyVisitedItem, Int) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, recentVisit ->
            when (recentVisit) {
                is RecentHistoryHighlight -> RecentlyVisitedHistoryHighlight(
                    recentVisit = recentVisit,
                    menuItems = menuItems,
                    onRecentVisitClick = {
                        onRecentVisitClick(it, pageIndex + 1)
                    },
                )

                is RecentHistoryGroup -> RecentlyVisitedHistoryGroup(
                    recentVisit = recentVisit,
                    menuItems = menuItems,
                    onRecentVisitClick = {
                        onRecentVisitClick(it, pageIndex + 1)
                    },
                )
            }
        }
    }
}

/**
 * A recently visited history group.
 *
 * @param recentVisit The [RecentHistoryGroup] to display.
 * @param menuItems List of [RecentVisitMenuItem] to display in a recent visit dropdown menu.
 * @param showDividerLine Whether to show a divider line at the bottom.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit.
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
private fun RecentlyVisitedHistoryGroup(
    recentVisit: RecentHistoryGroup,
    menuItems: List<RecentVisitMenuItem>,
    onRecentVisitClick: (RecentHistoryGroup) -> Unit = { _ -> },
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = { onRecentVisitClick(recentVisit) },
                onLongClick = { isMenuExpanded = true },
            )
            .height(itemRowHeight)
            .fillMaxWidth()
            .semantics {
                testTagsAsResourceId = true
                testTag = "recent.visits.group"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_multiple_tabs),
            contentDescription = null,
            modifier = Modifier.size(imageSize),
        )

        Spacer(modifier = Modifier.width(imageSpacer))

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                RecentlyVisitedTitle(
                    text = recentVisit.title,
                    modifier = Modifier
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "recent.visits.group.title"
                        },
                )

                Spacer(modifier = Modifier.height(textSpacer))

                RecentlyVisitedCaption(
                    count = recentVisit.historyMetadata.size,
                    modifier = Modifier
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "recent.visits.group.caption"
                        },
                )
            }
        }

        ContextualMenu(
            showMenu = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            menuItems = menuItems.map { MenuItem(it.title) { it.onClick(recentVisit) } },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "recent.visit.menu"
            },
        )
    }
}

/**
 * A recently visited history item.
 *
 * @param recentVisit The [RecentHistoryHighlight] to display.
 * @param menuItems List of [RecentVisitMenuItem] to display in a recent visit dropdown menu.
 * @param showDividerLine Whether to show a divider line at the bottom.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit.
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
private fun RecentlyVisitedHistoryHighlight(
    recentVisit: RecentHistoryHighlight,
    menuItems: List<RecentVisitMenuItem>,
    onRecentVisitClick: (RecentHistoryHighlight) -> Unit = { _ -> },
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = { onRecentVisitClick(recentVisit) },
                onLongClick = { isMenuExpanded = true },
            )
            .height(itemRowHeight)
            .fillMaxWidth()
            .semantics {
                testTagsAsResourceId = true
                testTag = "recent.visits.highlight"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(url = recentVisit.url, size = imageSize)

        Spacer(modifier = Modifier.width(imageSpacer))

        Box(modifier = Modifier.fillMaxSize()) {
            RecentlyVisitedTitle(
                text = recentVisit.title.trimmed(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .semantics {
                        testTagsAsResourceId = true
                        testTag = "recent.visits.highlight.title"
                    },
            )
        }

        ContextualMenu(
            showMenu = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            menuItems = menuItems.map { item -> MenuItem(item.title) { item.onClick(recentVisit) } },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "recent.visit.menu"
            },
        )
    }
}

/**
 * The title of a recent visit.
 *
 * @param text [String] that will be display. Will be ellipsized if cannot fit on one line.
 * @param modifier [Modifier] allowing to perfectly place this.
 */
@Composable
private fun RecentlyVisitedTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = FirefoxTheme.colors.textPrimary,
        fontSize = 16.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

/**
 * The caption text for a recent visit.
 *
 * @param count Number of recently visited items to display in the caption.
 * @param modifier [Modifier] allowing to perfectly place this.
 */
@Composable
private fun RecentlyVisitedCaption(
    count: Int,
    modifier: Modifier,
) {
    val stringId = if (count == 1) {
        R.string.history_search_group_site_1
    } else {
        R.string.history_search_group_sites_1
    }

    Text(
        text = String.format(LocalContext.current.getString(stringId), count),
        modifier = modifier,
        color = when (isSystemInDarkTheme()) {
            true -> FirefoxTheme.colors.textPrimary
            false -> FirefoxTheme.colors.textSecondary
        },
        fontSize = 12.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
@LightDarkPreview
private fun RecentlyVisitedMultipleColumnsPreview() {
    FirefoxTheme {
        RecentlyVisited(
            recentVisits = listOf(
                RecentHistoryGroup(title = "running shoes"),
                RecentHistoryGroup(title = "mozilla"),
                RecentHistoryGroup(title = "firefox"),
                RecentHistoryGroup(title = "pocket"),
            ),
            menuItems = emptyList(),
        )
    }
}

@Composable
@LightDarkPreview
private fun RecentlyVisitedSingleColumnPreview() {
    FirefoxTheme {
        RecentlyVisited(
            recentVisits = listOf(
                RecentHistoryGroup(title = "running shoes"),
                RecentHistoryGroup(title = "mozilla"),
                RecentHistoryGroup(title = "firefox"),
            ),
            menuItems = emptyList(),
        )
    }
}
