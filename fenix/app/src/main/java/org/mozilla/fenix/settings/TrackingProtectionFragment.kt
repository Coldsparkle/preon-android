/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.trackingprotection.TrackingProtectionMode
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Displays the toggle for tracking protection, options for tracking protection policy and a button
 * to open info about the tracking protection [org.mozilla.fenix.settings.TrackingProtectionFragment].
 */
class TrackingProtectionFragment : BasePreferenceFragment() {

    private val exceptionsClickListener = Preference.OnPreferenceClickListener {
        val directions =
            TrackingProtectionFragmentDirections.actionTrackingProtectionFragmentToExceptionsFragment()
        requireView().findNavController().navigate(directions)
        true
    }

    @VisibleForTesting
    internal lateinit var customCookies: CheckBoxPreference

    @VisibleForTesting
    internal lateinit var customCookiesSelect: DropDownPreference

    @VisibleForTesting
    internal lateinit var customTracking: CheckBoxPreference

    @VisibleForTesting
    internal lateinit var customTrackingSelect: DropDownPreference

    @VisibleForTesting
    internal lateinit var customCryptominers: CheckBoxPreference

    @VisibleForTesting
    internal lateinit var customFingerprinters: CheckBoxPreference

    @VisibleForTesting
    internal lateinit var customRedirectTrackers: CheckBoxPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tracking_protection_preferences, rootKey)
        val radioStrict = bindTrackingProtectionRadio(TrackingProtectionMode.STRICT)
        val radioStandard = bindTrackingProtectionRadio(TrackingProtectionMode.STANDARD)
        val radioCustom = bindCustom()
        addToRadioGroup(radioStrict, radioStandard, radioCustom)
        updateCustomOptionsVisibility()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preference_enhanced_tracking_protection))

        // Tracking Protection Switch
        val preferenceTP =
            requirePreference<FenixSwitchPreference>(R.string.pref_key_tracking_protection)

        preferenceTP.isChecked = requireContext().settings().shouldUseTrackingProtection
        preferenceTP.setOnPreferenceChangeListener<Boolean> { preference, trackingProtectionOn ->
            preference.context.settings().shouldUseTrackingProtection =
                trackingProtectionOn
            with(preference.context.components) {
                val policy = core.trackingProtectionPolicyFactory.createTrackingProtectionPolicy()
                useCases.settingsUseCases.updateTrackingProtection(policy)
                useCases.sessionUseCases.reload()
            }
            true
        }

        val learnMorePreference = requirePreference<Preference>(R.string.pref_key_etp_learn_more)
        learnMorePreference.setOnPreferenceClickListener {
            (activity as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getGenericSumoURLForTopic
                    (SupportUtils.SumoTopic.TRACKING_PROTECTION),
                newTab = true,
                from = BrowserDirection.FromTrackingProtection,
            )
            true
        }
        learnMorePreference.summary = getString(
            R.string.preference_enhanced_tracking_protection_explanation_2,
            getString(R.string.app_name),
        )

        val preferenceExceptions =
            requirePreference<Preference>(R.string.pref_key_tracking_protection_exceptions)
        preferenceExceptions.onPreferenceClickListener = exceptionsClickListener

        requirePreference<SwitchPreference>(R.string.pref_key_privacy_enable_global_privacy_control).apply {
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    context.components.core.engine.settings.globalPrivacyControlEnabled = newValue as Boolean
                    context.components.useCases.sessionUseCases.reload.invoke()
                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }
    }

    private fun bindTrackingProtectionRadio(
        mode: TrackingProtectionMode,
    ): RadioButtonInfoPreference {
        val radio = requirePreference<RadioButtonInfoPreference>(mode.preferenceKey)
        radio.contentDescription = getString(mode.contentDescriptionRes)

        radio.onClickListener {
            updateCustomOptionsVisibility()
            updateTrackingProtectionPolicy()
            TrackingProtection.etpSettingChanged.record(TrackingProtection.EtpSettingChangedExtra(mode.name))
        }

        radio.onInfoClickListener {
            nav(
                R.id.trackingProtectionFragment,
                TrackingProtectionFragmentDirections
                    .actionTrackingProtectionFragmentToTrackingProtectionBlockingFragment(mode),
            )
        }

        return radio
    }

    private fun bindCustom(): RadioButtonInfoPreference {
        val radio = bindTrackingProtectionRadio(TrackingProtectionMode.CUSTOM)

        customCookies =
            requirePreference(R.string.pref_key_tracking_protection_custom_cookies)

        customCookiesSelect =
            requirePreference(R.string.pref_key_tracking_protection_custom_cookies_select)

        customTracking =
            requirePreference(R.string.pref_key_tracking_protection_custom_tracking_content)

        customTrackingSelect =
            requirePreference(R.string.pref_key_tracking_protection_custom_tracking_content_select)

        customCryptominers =
            requirePreference(R.string.pref_key_tracking_protection_custom_cryptominers)

        customFingerprinters =
            requirePreference(R.string.pref_key_tracking_protection_custom_fingerprinters)

        customRedirectTrackers =
            requirePreference(R.string.pref_key_tracking_protection_redirect_trackers)

        customCookies.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                customCookiesSelect.isVisible = !customCookies.isChecked
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customTracking.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                customTrackingSelect.isVisible = !customTracking.isChecked
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customCookiesSelect.onPreferenceChangeListener = object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customTrackingSelect.onPreferenceChangeListener = object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customCryptominers.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customFingerprinters.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customRedirectTrackers.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        updateCustomOptionsVisibility()

        return radio
    }

    private fun updateTrackingProtectionPolicy() {
        context?.components?.let {
            val policy = it.core.trackingProtectionPolicyFactory
                .createTrackingProtectionPolicy()
            it.useCases.settingsUseCases.updateTrackingProtection.invoke(policy)
            it.useCases.sessionUseCases.reload.invoke()
        }
    }

    private fun updateCustomOptionsVisibility() {
        val isCustomSelected = requireContext().settings().useCustomTrackingProtection
        customCookies.isVisible = isCustomSelected
        customCookiesSelect.isVisible = isCustomSelected && customCookies.isChecked
        customTracking.isVisible = isCustomSelected
        customTrackingSelect.isVisible = isCustomSelected && customTracking.isChecked
        customCryptominers.isVisible = isCustomSelected
        customFingerprinters.isVisible = isCustomSelected
        customRedirectTrackers.isVisible = isCustomSelected
    }
}
