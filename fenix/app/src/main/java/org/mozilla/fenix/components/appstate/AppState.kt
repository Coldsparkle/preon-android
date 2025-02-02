/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.Crash.NativeCodeCrash
import mozilla.components.lib.state.State
import org.mozilla.fenix.browser.StandardSnackbarError
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.appstate.shopping.ShoppingState
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTabState
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.library.history.PendingDeletionHistory
import org.mozilla.fenix.search.SearchDialogFragment
import org.mozilla.fenix.wallpapers.WallpaperState

/**
 * Value type that represents the state of the tabs tray.
 *
 * @property isForeground Whether or not the app is in the foreground.
 * @property inactiveTabsExpanded A flag to know if the Inactive Tabs section of the Tabs Tray
 * should be expanded when the tray is opened.
 * @property isSearchDialogVisible Flag indicating whether the user is interacting with the [SearchDialogFragment].
 * @property nonFatalCrashes List of non-fatal crashes that allow the app to continue being used.
 * @property collections The list of [TabCollection] to display in the [HomeFragment].
 * @property expandedCollections A set containing the ids of the [TabCollection] that are expanded
 * in the [HomeFragment].
 * @property mode Whether the app is in private browsing mode.
 * @property selectedTabId The currently selected tab ID. This should be bound to [BrowserStore].
 * @property topSites The list of [TopSite] in the [HomeFragment].
 * @property showCollectionPlaceholder If true, shows a placeholder when there are no collections.
 * @property recentTabs The list of recent [RecentTab] in the [HomeFragment].
 * @property recentSyncedTabState The [RecentSyncedTabState] in the [HomeFragment].
 * @property recentHistory The list of [RecentlyVisitedItem]s.
 * @property pocketStories The list of currently shown [PocketRecommendedStory]s.
 * @property pocketStoriesCategories All [PocketRecommendedStory] categories.
 * @property pocketStoriesCategoriesSelections Current Pocket recommended stories categories selected by the user.
 * @property pocketSponsoredStories All [PocketSponsoredStory]s.
 * @property messaging State related messages.
 * @property pendingDeletionHistoryItems The set of History items marked for removal in the UI,
 * awaiting to be removed once the Undo snackbar hides away.
 * Also serves as an in memory cache of all stories mapped by category allowing for quick stories filtering.
 * @property wallpaperState The [WallpaperState] to display in the [HomeFragment].
 * @property standardSnackbarError A snackbar error message to display.
 * @property shoppingState Holds state for shopping feature that's required to live the lifetime of a session.
 * @property wasLastTabClosedPrivate Whether the last remaining tab that was closed in private mode. This is used to
 * display an undo snackbar message relevant to the browsing mode. If null, no snackbar is shown.
 */
data class AppState(
    val isForeground: Boolean = true,
    val inactiveTabsExpanded: Boolean = false,
    val isSearchDialogVisible: Boolean = false,
    val nonFatalCrashes: List<NativeCodeCrash> = emptyList(),
    val mode: BrowsingMode = BrowsingMode.Normal,
    val selectedTabId: String? = null,
    val topSites: List<TopSite> = emptyList(),
    val recentTabs: List<RecentTab> = emptyList(),
    val recentSyncedTabState: RecentSyncedTabState = RecentSyncedTabState.None,
    val recentHistory: List<RecentlyVisitedItem> = emptyList(),
    val pendingDeletionHistoryItems: Set<PendingDeletionHistory> = emptySet(),
    val wallpaperState: WallpaperState = WallpaperState.default,
    val standardSnackbarError: StandardSnackbarError? = null,
    val shoppingState: ShoppingState = ShoppingState(),
    val wasLastTabClosedPrivate: Boolean? = null,
) : State
