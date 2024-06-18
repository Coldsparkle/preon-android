/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.nimbus.messaging.Message
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.home.BottomSpacerViewHolder
import org.mozilla.fenix.home.TopPlaceholderViewHolder
import org.mozilla.fenix.home.recentsyncedtabs.view.RecentSyncedTabViewHolder
import org.mozilla.fenix.home.recenttabs.view.RecentTabViewHolder
import org.mozilla.fenix.home.recenttabs.view.RecentTabsHeaderViewHolder
import org.mozilla.fenix.home.recentvisits.view.RecentVisitsHeaderViewHolder
import org.mozilla.fenix.home.recentvisits.view.RecentlyVisitedViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.CustomizeHomeButtonViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.MessageCardViewHolder
import org.mozilla.fenix.home.topsites.TopSiteViewHolder

sealed class AdapterItem(@LayoutRes val viewType: Int) {
    object TopPlaceholderItem : AdapterItem(TopPlaceholderViewHolder.LAYOUT_ID)

    /**
     * Top sites.
     */
    data class TopSites(val topSites: List<TopSite>) : AdapterItem(TopSiteViewHolder.LAYOUT_ID) {
        override fun sameAs(other: AdapterItem): Boolean {
            return other is TopSites
        }

        override fun contentsSameAs(other: AdapterItem): Boolean {
            return topSites == (other as? TopSites)?.topSites
        }
    }

    /**
     * Contains a set of [Pair]s where [Pair.first] is the index of the changed [TopSite] and
     * [Pair.second] is the new [TopSite].
     */
    data class TopSitePagerPayload(
        val changed: Set<Pair<Int, TopSite>>,
    )

    data class NimbusMessageCard(
        val message: Message,
    ) : AdapterItem(MessageCardViewHolder.LAYOUT_ID) {
        override fun sameAs(other: AdapterItem) =
            other is NimbusMessageCard && message.id == other.message.id
    }

    object CustomizeHomeButton : AdapterItem(CustomizeHomeButtonViewHolder.LAYOUT_ID)

    object RecentTabsHeader : AdapterItem(RecentTabsHeaderViewHolder.LAYOUT_ID)
    object RecentTabItem : AdapterItem(RecentTabViewHolder.LAYOUT_ID)

    /**
     * Adapter item to hold homescreen synced tabs view.
     */
    object RecentSyncedTabItem : AdapterItem(RecentSyncedTabViewHolder.LAYOUT_ID)

    object RecentVisitsHeader : AdapterItem(RecentVisitsHeaderViewHolder.LAYOUT_ID)
    object RecentVisitsItems : AdapterItem(RecentlyVisitedViewHolder.LAYOUT_ID)

    object BottomSpacer : AdapterItem(BottomSpacerViewHolder.LAYOUT_ID)

    /**
     * True if this item represents the same value as other. Used by [AdapterItemDiffCallback].
     */
    open fun sameAs(other: AdapterItem) = this::class == other::class

    /**
     * Returns a payload if there's been a change, or null if not
     */
    open fun getChangePayload(newItem: AdapterItem): Any? = null

    open fun contentsSameAs(other: AdapterItem) = this::class == other::class
}

class AdapterItemDiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
    override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
        oldItem.sameAs(newItem)

    @Suppress("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
        oldItem.contentsSameAs(newItem)

    override fun getChangePayload(oldItem: AdapterItem, newItem: AdapterItem): Any? {
        return oldItem.getChangePayload(newItem) ?: return super.getChangePayload(oldItem, newItem)
    }
}

class SessionControlAdapter(
    private val interactor: SessionControlInteractor,
    private val viewLifecycleOwner: LifecycleOwner,
    private val components: Components,
) : ListAdapter<AdapterItem, RecyclerView.ViewHolder>(AdapterItemDiffCallback()) {

    // This method triggers the ComplexMethod lint error when in fact it's quite simple.
    @SuppressWarnings("ComplexMethod", "LongMethod", "ReturnCount")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            CustomizeHomeButtonViewHolder.LAYOUT_ID -> return CustomizeHomeButtonViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor,
            )
            MessageCardViewHolder.LAYOUT_ID -> return MessageCardViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor,
            )
            RecentTabViewHolder.LAYOUT_ID -> return RecentTabViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                recentTabInteractor = interactor,
            )
            RecentSyncedTabViewHolder.LAYOUT_ID -> return RecentSyncedTabViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                recentSyncedTabInteractor = interactor,
            )
            RecentlyVisitedViewHolder.LAYOUT_ID -> return RecentlyVisitedViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor,
            )
            RecentVisitsHeaderViewHolder.LAYOUT_ID -> return RecentVisitsHeaderViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor,
            )
            RecentTabsHeaderViewHolder.LAYOUT_ID -> return RecentTabsHeaderViewHolder(
                composeView = ComposeView(parent.context),
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor,
            )
        }

        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            TopPlaceholderViewHolder.LAYOUT_ID -> TopPlaceholderViewHolder(view)
            TopSiteViewHolder.LAYOUT_ID -> return TopSiteViewHolder(
                view = view,
                appStore = components.appStore,
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor
            )
            BottomSpacerViewHolder.LAYOUT_ID -> BottomSpacerViewHolder(view)
            else -> throw IllegalStateException()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is CustomizeHomeButtonViewHolder,
            is RecentlyVisitedViewHolder,
            is RecentVisitsHeaderViewHolder,
            is RecentTabViewHolder,
            is RecentSyncedTabViewHolder,
            is RecentTabsHeaderViewHolder,
            -> {
                // no op
                // This previously called "composeView.disposeComposition" which would have the
                // entire Composable destroyed and recreated when this View is scrolled off or on screen again.
                // This View already listens and maps store updates. Avoid creating and binding new Views.
                // The composition will live until the ViewTreeLifecycleOwner to which it's attached to is destroyed.
            }
            is MessageCardViewHolder -> {
                // Dispose the underlying composition immediately.
                // This ViewHolder can be removed / re-added and we need it to show a fresh new composition.
                holder.composeView.disposeComposition()
            }
            else -> super.onViewRecycled(holder)
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        }
    }

    @SuppressWarnings("ComplexMethod")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TopPlaceholderViewHolder -> {
                holder.bind()
            }
            is TopSiteViewHolder -> {
                holder.bind((item as AdapterItem.TopSites).topSites)
            }
            is MessageCardViewHolder -> {
                holder.bind((item as AdapterItem.NimbusMessageCard).message)
            }
            is RecentlyVisitedViewHolder,
            is RecentTabViewHolder,
            is RecentSyncedTabViewHolder,
            -> {
                // no-op. This ViewHolder receives the HomeStore as argument and will observe that
                // without the need for us to manually update from here the data to be displayed.
            }
        }
    }
}
