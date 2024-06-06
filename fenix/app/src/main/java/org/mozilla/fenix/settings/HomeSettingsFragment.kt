/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.mozilla.fenix.GleanMetrics.CustomizeHome
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.navigateWithBreadcrumb
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Lets the user customize the home screen.
 */
class HomeSettingsFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
        setupPreferences()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_home_2))
    }

    private fun setupPreferences() {
        requirePreference<SwitchPreference>(R.string.pref_key_show_top_sites).apply {
            isChecked = context.settings().showTopSitesFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    CustomizeHome.preferenceToggled.record(
                        CustomizeHome.PreferenceToggledExtra(
                            newValue as Boolean,
                            "most_visited_sites",
                        ),
                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<CheckBoxPreference>(R.string.pref_key_enable_contile).apply {
            isChecked = context.settings().showContileFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    CustomizeHome.preferenceToggled.record(
                        CustomizeHome.PreferenceToggledExtra(
                            newValue as Boolean,
                            "contile",
                        ),
                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_recent_tabs).apply {
            isChecked = context.settings().showRecentTabsFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    CustomizeHome.preferenceToggled.record(
                        CustomizeHome.PreferenceToggledExtra(
                            newValue as Boolean,
                            "jump_back_in",
                        ),
                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_history_metadata_feature).apply {
            isChecked = context.settings().historyMetadataUIFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    CustomizeHome.preferenceToggled.record(
                        CustomizeHome.PreferenceToggledExtra(
                            newValue as Boolean,
                            "recently_visited",
                        ),
                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        val openingScreenRadioHomepage =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_always)
        val openingScreenLastTab =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_never)
        val openingScreenAfterFourHours =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_after_four_hours)

        requirePreference<Preference>(R.string.pref_key_wallpapers).apply {
            setOnPreferenceClickListener {
                view?.findNavController()?.navigateWithBreadcrumb(
                    directions = HomeSettingsFragmentDirections.actionHomeSettingsFragmentToWallpaperSettingsFragment(),
                    navigateFrom = "HomeSettingsFragment",
                    navigateTo = "ActionHomeSettingsFragmentToWallpaperSettingsFragment",
                    crashReporter = context.components.analytics.crashReporter,
                )
                true
            }
        }

        addToRadioGroup(
            openingScreenRadioHomepage,
            openingScreenLastTab,
            openingScreenAfterFourHours,
        )
    }
}
