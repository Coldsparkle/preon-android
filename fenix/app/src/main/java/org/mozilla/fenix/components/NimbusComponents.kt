/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.service.nimbus.NimbusApi
import org.mozilla.experiments.nimbus.NimbusEventStore
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.experiments.createNimbus
import org.mozilla.fenix.perf.lazyMonitored

/**
 * Component group for access to Nimbus and other Nimbus services.
 */
class NimbusComponents(private val context: Context) {

    /**
     * The main entry point for the Nimbus SDK. Note that almost all access to feature configuration
     * should be mediated through a FML generated class, e.g. [FxNimbus].
     */
    val sdk: NimbusApi by lazyMonitored {
        createNimbus(context, BuildConfig.NIMBUS_ENDPOINT)
    }

    /**
     * Convenience method for getting the event store from the SDK.
     *
     * Before EXP-4354, this is the main write API for recording events to drive
     * messaging, experiments and onboarding.
     *
     * Following EXP-4354, clients will not need to write these events
     * themselves.
     *
     * Read access to the event store should be done through
     * the JEXL helper available from [createJexlHelper].
     */
    val events: NimbusEventStore by lazyMonitored {
        sdk.events
    }

}
