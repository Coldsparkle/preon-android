/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.contextmenu

import android.view.HapticFeedbackConstants
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.HitResult
import mozilla.components.feature.contextmenu.facts.emitCancelMenuFact
import mozilla.components.feature.contextmenu.facts.emitClickFact
import mozilla.components.feature.contextmenu.facts.emitDisplayFact
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature


/**
 * Feature for displaying a context menu after long-pressing web content.
 *
 * This feature will subscribe to the currently selected tab and display a context menu based on
 * the [HitResult] in its `ContentState`. Once the context menu is closed or the user selects an
 * item from the context menu the related [HitResult] will be consumed.
 *
 * @property fragmentManager The [FragmentManager] to be used when displaying a context menu (fragment).
 * @property store The [BrowserStore] this feature should subscribe to.
 * @property candidates A list of [ContextMenuCandidate] objects. For every observed [HitResult] this feature will query
 * all candidates ([ContextMenuCandidate.showFor]) in order to determine which candidates want to show up in the context
 * menu. If a context menu item was selected by the user the feature will invoke the [ContextMenuCandidate.action]
 * method of the related candidate.
 * @property engineView The [EngineView]] this feature component should show context menus for.
 * @param tabId Optional id of a tab. Instead of showing context menus for the currently selected tab this feature will
 * show only context menus for this tab if an id is provided.
 * @param additionalNote which it will be attached to the bottom of context menu but for a specific [HitResult]
 */
class ContextMenuFeature(
    private val store: BrowserStore,
    private val candidates: List<ContextMenuCandidate>,
    private val engineView: EngineView,
    private val useCases: ContextMenuUseCases,
    private val tabId: String? = null,
    private val additionalNote: (HitResult) -> String? = { null },
) : LifecycleAwareFeature {
    private var scope: CoroutineScope? = null

    /**
     * Start observing the selected session and when needed show a context menu.
     */
    override fun start() {
        scope = store.flowScoped { flow ->
            flow.map { state -> state.findTabOrCustomTabOrSelectedTab(tabId) }
                .distinctUntilChangedBy { it?.content?.hitResult }
                .collect { state ->
                    val hitResult = state?.content?.hitResult
                    if (hitResult != null) {
                        showContextMenu(state, hitResult)
                    } else {
                        hideContextMenu()
                    }
                }
        }
    }

    /**
     * Stop observing the selected session and do not show any context menus anymore.
     */
    override fun stop() {
        scope?.cancel()
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun showContextMenu(tab: SessionState, hitResult: HitResult) {
        val (ids, labels) = candidates
            .filter { candidate -> candidate.showFor(tab, hitResult) }
            .fold(Pair(mutableListOf<String>(), mutableListOf<String>())) { items, candidate ->
                items.first.add(candidate.id)
                items.second.add(candidate.label)
                items
            }

        // We have no context menu items to show for this HitResult. Let's consume it to remove it from the Session.
        if (ids.isEmpty()) {
            useCases.consumeHitResult(tab.id)
            return
        }

        // We know that we are going to show a context menu. Now is the time to perform the haptic feedback.
        engineView.asView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        emitDisplayFact(labels.joinToString())
        val popup = ContextMenuPopup(ids, labels, tab.id, hitResult.getLink())
        popup.feature = this
        popup.show(engineView.asView(), hitResult.screenX, hitResult.screenY)
    }

    private fun hideContextMenu() {
        emitCancelMenuFact()
    }

    internal fun onMenuItemSelected(tabId: String, itemId: String) {
        val tab = store.state.findTabOrCustomTab(tabId) ?: return
        val candidate = candidates.find { it.id == itemId } ?: return

        useCases.consumeHitResult(tab.id)

        tab.content.hitResult?.let { hitResult ->
            candidate.action.invoke(tab, hitResult)
            emitClickFact(candidate)
        }
    }

    internal fun onMenuCancelled(tabId: String) {
        useCases.consumeHitResult(tabId)
    }
}
