/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.nimbus.messaging.Message
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.shouldShowRecentSyncedTabs
import org.mozilla.fenix.ext.shouldShowRecentTabs
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.messaging.FenixMessageSurfaceId
import org.mozilla.fenix.onboarding.HomeCFRPresenter
import org.mozilla.fenix.utils.Settings

// This method got a little complex with the addition of the tab tray feature flag
// When we remove the tabs from the home screen this will get much simpler again.
@Suppress("ComplexMethod", "LongParameterList")
@VisibleForTesting
internal fun normalModeAdapterItems(
    settings: Settings,
    topSites: List<TopSite>,
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    showCollectionsPlaceholder: Boolean,
    nimbusMessageCard: Message? = null,
    showRecentTab: Boolean,
    showRecentSyncedTab: Boolean,
    recentVisits: List<RecentlyVisitedItem>,
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()

    // Add a synchronous, unconditional and invisible placeholder so home is anchored to the top when created.
    items.add(AdapterItem.TopPlaceholderItem)

    nimbusMessageCard?.let {
        items.add(AdapterItem.NimbusMessageCard(it))
    }

    if (settings.showTopSitesFeature && topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSites(topSites))
    }

    if (showRecentTab) {
        items.add(AdapterItem.RecentTabsHeader)
        items.add(AdapterItem.RecentTabItem)
        if (showRecentSyncedTab) {
            items.add(AdapterItem.RecentSyncedTabItem)
        }
    }

    if (settings.historyMetadataUIFeature && recentVisits.isNotEmpty()) {
        items.add(AdapterItem.RecentVisitsHeader)
        items.add(AdapterItem.RecentVisitsItems)
    }

    if (collections.isEmpty()) {
        if (showCollectionsPlaceholder) {
            items.add(AdapterItem.NoCollectionsMessage)
        }
    } else {
        showCollections(collections, expandedCollections, items)
    }

    items.add(AdapterItem.BottomSpacer)

    return items
}

private fun showCollections(
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    items: MutableList<AdapterItem>,
) {
    // If the collection is expanded, we want to add all of its tabs beneath it in the adapter
    items.add(AdapterItem.CollectionHeader)
    collections.map {
        AdapterItem.CollectionItem(it, expandedCollections.contains(it.id))
    }.forEach {
        items.add(it)
        if (it.expanded) {
            items.addAll(collectionTabItems(it.collection))
        }
    }
}

private fun privateModeAdapterItems() = listOf(AdapterItem.PrivateBrowsingDescription)

private fun AppState.toAdapterList(settings: Settings): List<AdapterItem> = when (mode) {
    BrowsingMode.Normal -> normalModeAdapterItems(
        settings,
        topSites,
        collections,
        expandedCollections,
        showCollectionPlaceholder,
        messaging.messageToShow[FenixMessageSurfaceId.HOMESCREEN],
        shouldShowRecentTabs(settings),
        shouldShowRecentSyncedTabs(),
        recentHistory,
    )
    BrowsingMode.Private -> privateModeAdapterItems()
}

private fun collectionTabItems(collection: TabCollection) =
    collection.tabs.mapIndexed { index, tab ->
        AdapterItem.TabInCollectionItem(collection, tab, index == collection.tabs.lastIndex)
    }

/**
 * Shows a list of Home screen views.
 *
 * @param containerView The [View] that is used to initialize the Home recycler view.
 * @param viewLifecycleOwner [LifecycleOwner] for the view.
 * @param interactor [SessionControlInteractor] which will have delegated to all user
 * interactions.
 */
class SessionControlView(
    containerView: View,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: SessionControlInteractor,
) {

    val view: RecyclerView = containerView as RecyclerView

    // We want to limit feature recommendations to one per HomePage visit.
    var featureRecommended = false

    private val sessionControlAdapter = SessionControlAdapter(
        interactor,
        viewLifecycleOwner,
        containerView.context.components,
    )

    init {
        @Suppress("NestedBlockDepth")
        view.apply {
            adapter = sessionControlAdapter
            layoutManager = object : LinearLayoutManager(containerView.context) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)

                    if (!featureRecommended && !context.settings().showHomeOnboardingDialog) {
                        if (!context.settings().showHomeOnboardingDialog && (
                                context.settings().showSyncCFR ||
                                    context.settings().shouldShowJumpBackInCFR
                                )
                        ) {
                            featureRecommended = HomeCFRPresenter(
                                context = context,
                                recyclerView = view,
                            ).show()
                        }

                        if (!context.settings().shouldShowJumpBackInCFR &&
                            context.settings().showWallpaperOnboarding &&
                            !featureRecommended
                        ) {
                            featureRecommended = interactor.showWallpapersOnboardingDialog(
                                context.components.appStore.state.wallpaperState,
                            )
                        }
                    }
                }
            }
        }
    }

    fun update(state: AppState, shouldReportMetrics: Boolean = false) {
        if (shouldReportMetrics) interactor.reportSessionMetrics(state)

        sessionControlAdapter.submitList(state.toAdapterList(view.context.settings()))
    }
}
