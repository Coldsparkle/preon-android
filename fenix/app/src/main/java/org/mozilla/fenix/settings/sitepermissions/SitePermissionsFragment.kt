/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.sitepermissions

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.Config
import org.mozilla.fenix.GleanMetrics.Autoplay
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.navigateWithBreadcrumb
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.BasePreferenceFragment
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.requirePreference

@SuppressWarnings("TooManyFunctions")
class SitePermissionsFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.site_permissions_preferences, rootKey)

        val preferenceDescription = requirePreference<Preference>(R.string.pref_key_site_permissions_description)
        preferenceDescription.isVisible = Config.channel.isMozillaOnline
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_site_permissions))
        setupPreferences()
    }

    private fun setupPreferences() {
        bindCategoryPhoneFeatures()
        bindExceptions()
    }

    private fun bindExceptions() {
        val keyExceptions = getPreferenceKey(R.string.pref_key_show_site_exceptions)
        val exceptionsCategory = requireNotNull(findPreference(keyExceptions))

        exceptionsCategory.onPreferenceClickListener = OnPreferenceClickListener {
            val directions = SitePermissionsFragmentDirections.actionSitePermissionsToExceptions()
            Navigation.findNavController(requireView()).navigate(directions)
            true
        }
    }

    private fun bindCategoryPhoneFeatures() {
        PhoneFeature.values()
            // Autoplay inaudible should be set in the same menu as autoplay audible, so it does
            // not need to be bound
            .filter { it != PhoneFeature.AUTOPLAY_INAUDIBLE }
            .forEach(::initPhoneFeature)
    }

    private fun initPhoneFeature(phoneFeature: PhoneFeature) {
        val context = requireContext()
        val settings = context.settings()

        val cameraPhoneFeatures = requirePreference<Preference>(phoneFeature.getPreferenceId())
        cameraPhoneFeatures.summary = phoneFeature.getActionLabel(context, settings = settings)

        cameraPhoneFeatures.onPreferenceClickListener = OnPreferenceClickListener {
            navigateToPhoneFeature(phoneFeature)
            true
        }
    }

    private fun navigateToPhoneFeature(phoneFeature: PhoneFeature) {
        val directions = SitePermissionsFragmentDirections
            .actionSitePermissionsToManagePhoneFeatures(phoneFeature)

        if (phoneFeature == PhoneFeature.AUTOPLAY_AUDIBLE) {
            Autoplay.visitedSetting.record(NoExtras())
        }
        context?.let {
            Navigation.findNavController(requireView()).navigateWithBreadcrumb(
                directions = directions,
                navigateFrom = "SitePermissionsFragment",
                navigateTo = "ActionSitePermissionsToManagePhoneFeatures",
                crashReporter = it.components.analytics.crashReporter,
            )
        }
    }
}
