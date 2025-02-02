/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.os.Bundle
import androidx.annotation.LayoutRes
import org.mozilla.fenix.ext.removeSecure
import org.mozilla.fenix.ext.secure
import org.mozilla.fenix.toobar.BaseToolbarFragment

/**
 * A [BaseToolbarFragment] implementation that can be used to secure screens displaying sensitive information
 * by not allowing taking screenshots of their content.
 *
 * Fragments displaying such screens should extend [SecureFragment] instead of [BaseToolbarFragment] class.
 */
open class SecureFragment(@LayoutRes contentLayoutId: Int = 0) : BaseToolbarFragment(contentLayoutId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        this.secure()
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        this.removeSecure()
        super.onDestroy()
    }
}
