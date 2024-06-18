/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.support.ktx.android.view.showKeyboard
import mozilla.components.ui.widgets.withCenterAlignedButtons
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.HomeScreen
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.Pocket
import org.mozilla.fenix.GleanMetrics.RecentTabs
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.metrics.MetricsUtils
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.wallpapers.WallpaperState

/**
 * [HomeFragment] controller. An interface that handles the view manipulation of the Tabs triggered
 * by the Interactor.
 */
@Suppress("TooManyFunctions")
interface SessionControlController {

    /**
     * @see [TopSiteInteractor.onOpenInPrivateTabClicked]
     */
    fun handleOpenInPrivateTabClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onRenameTopSiteClicked]
     */
    fun handleRenameTopSiteClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onRemoveTopSiteClicked]
     */
    fun handleRemoveTopSiteClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onSelectTopSite]
     */
    fun handleSelectTopSite(topSite: TopSite, position: Int)

    /**
     * @see [TopSiteInteractor.onSettingsClicked]
     */
    fun handleTopSiteSettingsClicked()

    /**
     * @see [TopSiteInteractor.onSponsorPrivacyClicked]
     */
    fun handleSponsorPrivacyClicked()

    /**
     * @see [TopSiteInteractor.onTopSiteLongClicked]
     */
    fun handleTopSiteLongClicked(topSite: TopSite)

    /**
     * @see [CollectionInteractor.onAddTabsToCollectionTapped]
     */
    fun handleCreateCollection()

    /**
     * @see [MessageCardInteractor.onMessageClicked]
     */
    fun handleMessageClicked(message: Message)

    /**
     * @see [MessageCardInteractor.onMessageClosedClicked]
     */
    fun handleMessageClosed(message: Message)

    /**
     * @see [CustomizeHomeIteractor.openCustomizeHomePage]
     */
    fun handleCustomizeHomeTapped()

    /**
     * @see [WallpaperInteractor.showWallpapersOnboardingDialog]
     */
    fun handleShowWallpapersOnboardingDialog(state: WallpaperState): Boolean

    /**
     * @see [SessionControlInteractor.reportSessionMetrics]
     */
    fun handleReportSessionMetrics(state: AppState)
}

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class DefaultSessionControlController(
    private val activity: HomeActivity,
    private val settings: Settings,
    private val engine: Engine,
    private val store: BrowserStore,
    private val addTabUseCase: TabsUseCases.AddNewTabUseCase,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
    private val appStore: AppStore,
    private val navController: NavController,
    private val viewLifecycleScope: CoroutineScope,
    private val showUndoSnackbarForTopSite: (topSite: TopSite) -> Unit,
) : SessionControlController {

    override fun handleOpenInPrivateTabClicked(topSite: TopSite) {
        if (topSite is TopSite.Provided) {
            TopSites.openContileInPrivateTab.record(NoExtras())
        } else {
            TopSites.openInPrivateTab.record(NoExtras())
        }
        with(activity) {
            browsingModeManager.mode = BrowsingMode.Private
            openToBrowserAndLoad(
                searchTermOrURL = topSite.url,
                newTab = true,
                from = BrowserDirection.FromHome,
            )
        }
    }

    @SuppressLint("InflateParams")
    override fun handleRenameTopSiteClicked(topSite: TopSite) {
        activity.let {
            val customLayout =
                LayoutInflater.from(it).inflate(R.layout.top_sites_rename_dialog, null)
            val topSiteLabelEditText: EditText =
                customLayout.findViewById(R.id.top_site_title)
            topSiteLabelEditText.setText(topSite.title)

            AlertDialog.Builder(it).apply {
                setTitle(R.string.rename_top_site)
                setView(customLayout)
                setPositiveButton(R.string.top_sites_rename_dialog_ok) { dialog, _ ->
                    viewLifecycleScope.launch(Dispatchers.IO) {
                        with(activity.components.useCases.topSitesUseCase) {
                            updateTopSites(
                                topSite,
                                topSiteLabelEditText.text.toString(),
                                topSite.url,
                            )
                        }
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.top_sites_rename_dialog_cancel) { dialog, _ ->
                    dialog.cancel()
                }
            }.show().withCenterAlignedButtons().also {
                topSiteLabelEditText.setSelection(0, topSiteLabelEditText.text.length)
                topSiteLabelEditText.showKeyboard()
            }
        }
    }

    override fun handleRemoveTopSiteClicked(topSite: TopSite) {
        TopSites.remove.record(NoExtras())
        when (topSite.url) {
            SupportUtils.POCKET_TRENDING_URL -> Pocket.pocketTopSiteRemoved.record(NoExtras())
            SupportUtils.GOOGLE_URL -> TopSites.googleTopSiteRemoved.record(NoExtras())
            SupportUtils.BAIDU_URL -> TopSites.baiduTopSiteRemoved.record(NoExtras())
        }

        viewLifecycleScope.launch(Dispatchers.IO) {
            with(activity.components.useCases.topSitesUseCase) {
                removeTopSites(topSite)
            }
        }

        showUndoSnackbarForTopSite(topSite)
    }

    override fun handleSelectTopSite(topSite: TopSite, position: Int) {
        val isPrivateMode = appStore.state.mode.isPrivate
        when (topSite) {
            is TopSite.Default -> TopSites.openDefault.record(NoExtras())
            is TopSite.Frecent -> TopSites.openFrecency.record(NoExtras())
            is TopSite.Pinned -> TopSites.openPinned.record(NoExtras())
            is TopSite.Provided -> TopSites.openContileTopSite.record(NoExtras()).also {
                submitTopSitesImpressionPing(topSite, position)
            }
        }

        when (topSite.url) {
            SupportUtils.GOOGLE_URL -> TopSites.openGoogleSearchAttribution.record(NoExtras())
            SupportUtils.BAIDU_URL -> TopSites.openBaiduSearchAttribution.record(NoExtras())
            SupportUtils.POCKET_TRENDING_URL -> Pocket.pocketTopSiteClicked.record(NoExtras())
        }

        val availableEngines: List<SearchEngine> = getAvailableSearchEngines()
        val searchAccessPoint = MetricsUtils.Source.TOPSITE

        availableEngines.firstOrNull { engine ->
            engine.resultUrls.firstOrNull { it.contains(topSite.url) } != null
        }?.let { searchEngine ->
            MetricsUtils.recordSearchMetrics(
                searchEngine,
                searchEngine == store.state.search.selectedOrDefaultSearchEngine,
                searchAccessPoint,
            )
        }

        val existingTabForUrl = when (topSite) {
            is TopSite.Frecent, is TopSite.Pinned -> {
                val tabs = if (isPrivateMode) {
                    store.state.privateTabs
                } else {
                    store.state.tabs
                }
                tabs.firstOrNull { topSite.url == it.content.url }
            }

            else -> null
        }

        if (existingTabForUrl == null) {
            if (isPrivateMode) {
                TopSites.openInPrivateTab.record(NoExtras())
            } else {
                TopSites.openInNewTab.record(NoExtras())
            }

            val tabId = addTabUseCase.invoke(
                url = appendSearchAttributionToUrlIfNeeded(topSite.url),
                selectTab = true,
                startLoading = true,
                private = isPrivateMode
            )

            if (settings.openNextTabInDesktopMode) {
                activity.handleRequestDesktopMode(tabId)
            }
        } else {
            selectTabUseCase.invoke(existingTabForUrl.id)
        }
        navController.navigate(R.id.browserFragment)
    }

    @VisibleForTesting
    internal fun submitTopSitesImpressionPing(topSite: TopSite.Provided, position: Int) {
        TopSites.contileClick.record(
            TopSites.ContileClickExtra(
                position = position + 1,
                source = "newtab",
            ),
        )

        topSite.id?.let { TopSites.contileTileId.set(it) }
        topSite.title?.let { TopSites.contileAdvertiser.set(it.lowercase()) }
        TopSites.contileReportingUrl.set(topSite.clickUrl)
        Pings.topsitesImpression.submit()
    }

    override fun handleTopSiteSettingsClicked() {
        TopSites.contileSettings.record(NoExtras())
        navController.nav(
            R.id.homeFragment,
            HomeFragmentDirections.actionGlobalHomeSettingsFragment(),
        )
    }

    override fun handleSponsorPrivacyClicked() {
        TopSites.contileSponsorsAndPrivacy.record(NoExtras())
        activity.openToBrowserAndLoad(
            searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SPONSOR_PRIVACY),
            newTab = true,
            from = BrowserDirection.FromHome,
        )
    }

    override fun handleTopSiteLongClicked(topSite: TopSite) {
        TopSites.longPress.record(TopSites.LongPressExtra(topSite.type))
    }

    @VisibleForTesting
    internal fun getAvailableSearchEngines() =
        activity.components.core.store.state.search.searchEngines +
            activity.components.core.store.state.search.availableSearchEngines

    /**
     * Append a search attribution query to any provided search engine URL based on the
     * user's current region.
     */
    private fun appendSearchAttributionToUrlIfNeeded(url: String): String {
        if (url == SupportUtils.GOOGLE_URL) {
            store.state.search.region?.let { region ->
                return when (region.current) {
                    "US" -> SupportUtils.GOOGLE_US_URL
                    else -> SupportUtils.GOOGLE_XX_URL
                }
            }
        }

        return url
    }

    override fun handleCustomizeHomeTapped() {
        val directions = HomeFragmentDirections.actionGlobalHomeSettingsFragment()
        navController.nav(navController.currentDestination?.id, directions)
        HomeScreen.customizeHomeClicked.record(NoExtras())
    }

    override fun handleShowWallpapersOnboardingDialog(state: WallpaperState): Boolean {
        return false
    }

    private fun showTabTrayCollectionCreation() {
        val directions = HomeFragmentDirections.actionGlobalTabsTrayFragment(
            enterMultiselect = true,
        )
        navController.nav(R.id.homeFragment, directions)
    }

    override fun handleCreateCollection() {
        showTabTrayCollectionCreation()
    }

    override fun handleMessageClicked(message: Message) {
    }

    override fun handleMessageClosed(message: Message) {
    }

    override fun handleReportSessionMetrics(state: AppState) {
        if (state.recentTabs.isEmpty()) {
            RecentTabs.sectionVisible.set(false)
        } else {
            RecentTabs.sectionVisible.set(true)
        }

    }
}
