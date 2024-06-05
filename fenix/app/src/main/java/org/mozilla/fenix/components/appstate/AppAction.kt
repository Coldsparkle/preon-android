/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.Crash.NativeCodeCrash
import mozilla.components.lib.state.Action
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.MessageSurfaceId
import org.mozilla.fenix.browser.StandardSnackbarError
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.shopping.ShoppingState
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTab
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTabState
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.library.history.PendingDeletionHistory
import org.mozilla.fenix.messaging.MessagingState
import org.mozilla.fenix.search.SearchDialogFragment
import org.mozilla.fenix.wallpapers.Wallpaper

/**
 * [Action] implementation related to [AppStore].
 */
sealed class AppAction : Action {
    data class UpdateInactiveExpanded(val expanded: Boolean) : AppAction()

    /**
     * Updates whether the [SearchDialogFragment] is visible.
     */
    data class UpdateSearchDialogVisibility(val isVisible: Boolean) : AppAction()
    data class AddNonFatalCrash(val crash: NativeCodeCrash) : AppAction()
    data class RemoveNonFatalCrash(val crash: NativeCodeCrash) : AppAction()
    object RemoveAllNonFatalCrashes : AppAction()

    data class Change(
        val topSites: List<TopSite>,
        val mode: BrowsingMode,
        val collections: List<TabCollection>,
        val showCollectionPlaceholder: Boolean,
        val recentTabs: List<RecentTab>,
        val recentHistory: List<RecentlyVisitedItem>,
        val recentSyncedTabState: RecentSyncedTabState,
    ) :
        AppAction()

    data class CollectionExpanded(val collection: TabCollection, val expand: Boolean) :
        AppAction()

    data class CollectionsChange(val collections: List<TabCollection>) : AppAction()
    data class ModeChange(val mode: BrowsingMode) : AppAction()
    data class TopSitesChange(val topSites: List<TopSite>) : AppAction()
    data class RecentTabsChange(val recentTabs: List<RecentTab>) : AppAction()
    data class RemoveRecentTab(val recentTab: RecentTab) : AppAction()
    data class RecentHistoryChange(val recentHistory: List<RecentlyVisitedItem>) : AppAction()
    data class RemoveRecentHistoryHighlight(val highlightUrl: String) : AppAction()
    data class DisbandSearchGroupAction(val searchTerm: String) : AppAction()

    /**
     * Adds a set of items marked for removal to the app state, to be hidden in the UI.
     */
    data class AddPendingDeletionSet(val historyItems: Set<PendingDeletionHistory>) : AppAction()

    /**
     * Removes a set of items, previously marked for removal, to be displayed again in the UI.
     */
    data class UndoPendingDeletionSet(val historyItems: Set<PendingDeletionHistory>) : AppAction()


    object RemoveCollectionsPlaceholder : AppAction()

    /**
     * Updates the [RecentSyncedTabState] with the given [state].
     */
    data class RecentSyncedTabStateChange(val state: RecentSyncedTabState) : AppAction()

    /**
     * Add a [RecentSyncedTab] url to the homescreen blocklist and remove it
     * from the recent synced tabs list.
     */
    data class RemoveRecentSyncedTab(val syncedTab: RecentSyncedTab) : AppAction()

    /**
     * Action indicating that the selected tab has been changed.
     *
     * @property tab The tab that has been selected.
     */
    data class SelectedTabChanged(val tab: TabSessionState) : AppAction()

    /**
     * [Action]s related to interactions with the Messaging Framework.
     */
    sealed class MessagingAction : AppAction() {
        /**
         * Restores the [Message] state from the storage.
         */
        object Restore : MessagingAction()

        /**
         * Evaluates if a new messages should be shown to users.
         */
        data class Evaluate(val surface: MessageSurfaceId) : MessagingAction()

        /**
         * Updates [MessagingState.messageToShow] with the given [message].
         */
        data class UpdateMessageToShow(val message: Message) : MessagingAction()

        /**
         * Updates [MessagingState.messageToShow] with the given [message].
         */
        data class ConsumeMessageToShow(val surface: MessageSurfaceId) : MessagingAction()

        /**
         * Updates [MessagingState.messages] with the given [messages].
         */
        data class UpdateMessages(val messages: List<Message>) : MessagingAction()

        /**
         * Indicates the given [message] was clicked.
         */
        data class MessageClicked(val message: Message) : MessagingAction()

        /**
         * Indicates the given [message] was dismissed.
         */
        data class MessageDismissed(val message: Message) : MessagingAction()
    }

    /**
     * [Action]s related to interactions with the wallpapers feature.
     */
    sealed class WallpaperAction : AppAction() {
        /**
         * Indicates that a different [wallpaper] was selected.
         */
        data class UpdateCurrentWallpaper(val wallpaper: Wallpaper) : WallpaperAction()

        /**
         * Indicates that the list of potential wallpapers has changed.
         */
        data class UpdateAvailableWallpapers(val wallpapers: List<Wallpaper>) : WallpaperAction()

        /**
         * Indicates a change in the download state of a wallpaper. Note that this is meant to be
         * used for full size images, not thumbnails.
         *
         * @property wallpaper The wallpaper that is being updated.
         * @property imageState The updated image state for the wallpaper.
         */
        data class UpdateWallpaperDownloadState(
            val wallpaper: Wallpaper,
            val imageState: Wallpaper.ImageFileState,
        ) : WallpaperAction()
    }

    /**
     * [AppAction] implementations related to the application lifecycle.
     */
    sealed class AppLifecycleAction : AppAction() {

        /**
         * The application has received an ON_RESUME event.
         */
        object ResumeAction : AppLifecycleAction()

        /**
         * The application has received an ON_PAUSE event.
         */
        object PauseAction : AppLifecycleAction()
    }

    /**
     * State of standard error snackBar has changed.
     */
    data class UpdateStandardSnackbarErrorAction(
        val standardSnackbarError: StandardSnackbarError?,
    ) : AppAction()

    /**
     * [AppAction]s related to shopping sheet state.
     */
    sealed class ShoppingAction : AppAction() {

        /**
         * [ShoppingAction] used to update the expansion state of the shopping sheet.
         */
        data class ShoppingSheetStateUpdated(val expanded: Boolean) : ShoppingAction()

        /**
         * [ShoppingAction] used to update the expansion state of the highlights card.
         */
        data class HighlightsCardExpanded(
            val productPageUrl: String,
            val expanded: Boolean,
        ) : ShoppingAction()

        /**
         * [ShoppingAction] used to update the expansion state of the info card.
         */
        data class InfoCardExpanded(
            val productPageUrl: String,
            val expanded: Boolean,
        ) : ShoppingAction()

        /**
         * [ShoppingAction] used to update the expansion state of the settings card.
         */
        data class SettingsCardExpanded(
            val productPageUrl: String,
            val expanded: Boolean,
        ) : ShoppingAction()

        /**
         * [ShoppingAction] used to update the recorded product recommendation impressions set.
         */
        data class ProductRecommendationImpression(
            val key: ShoppingState.ProductRecommendationImpressionKey,
        ) : ShoppingAction()
    }

    /**
     * [AppAction]s related to the tab strip.
     */
    sealed class TabStripAction : AppAction() {

        /**
         * [TabStripAction] used to update whether the last remaining tab that was closed was private.
         * Null means the state should reset and no snackbar should be shown.
         */
        data class UpdateLastTabClosed(val private: Boolean?) : TabStripAction()
    }
}
