/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.IncompleteRedesignToolbarFeature
import org.mozilla.fenix.databinding.FragmentHomeBinding
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.toolbar.ToolbarInteractor
import org.mozilla.fenix.utils.ToolbarPopupWindow
import java.lang.ref.WeakReference

/**
 * View class for setting up the home screen toolbar.
 */
class ToolbarView(
    private val binding: FragmentHomeBinding,
    private val context: Context,
    private val interactor: ToolbarInteractor,
) {
    init {
        updateLayout(binding.root)
    }

    /**
     * Setups the home screen toolbar.
     */
    fun build() {
        binding.toolbar.compoundDrawablePadding =
            context.resources.getDimensionPixelSize(R.dimen.search_bar_search_engine_icon_padding)

        binding.toolbar.apply {
            compoundDrawablePadding =
                context.resources.getDimensionPixelSize(R.dimen.search_bar_search_engine_icon_padding)
            setOnClickListener {
                interactor.onNavigateSearch()
            }
            setOnLongClickListener {
                ToolbarPopupWindow.show(
                    WeakReference(it),
                    handlePasteAndGo = interactor::onPasteAndGo,
                    handlePaste = interactor::onPaste,
                    copyVisible = false,
                )
                true
            }
        }
    }

    private fun updateLayout(view: View) {
        val redesignEnabled = IncompleteRedesignToolbarFeature(context.settings()).isEnabled
        binding.menuButton.isVisible = !redesignEnabled
        binding.tabButton.isVisible = !redesignEnabled
    }
}
