/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.privatebrowsing.interactor

import org.mozilla.fenix.browser.browsingmode.BrowsingMode

/**
 * Interface for private browsing mode related actions.
 */
interface PrivateBrowsingInteractor {

    /**
     * Called when a user clicks on the Private Mode button on the homescreen.
     */
    fun onPrivateModeButtonClicked(newMode: BrowsingMode)
}
