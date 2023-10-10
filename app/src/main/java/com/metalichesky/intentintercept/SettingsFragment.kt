package com.metalichesky.intentintercept;

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle;
import androidx.core.app.ShareCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.enable_disable_settings)
        addPreferencesFromResource(R.xml.settings)
        setupSettings(requireActivity(), preferenceManager)
    }

    private fun setupSettings(activity: Activity, preferenceManager: PreferenceManager) {
        val interceptEnabledKey = activity.getString(R.string.pref_intercept_enabled)
        val interceptEnabledPreference =
            preferenceManager.findPreference<Preference>(interceptEnabledKey)
        interceptEnabledPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (preference == interceptEnabledPreference) {
                switchInterceptEnabled(activity, newValue as Boolean)
            }
            true
        }

        val sendTestIntentPreference = preferenceManager.findPreference<Preference>(
            activity.getString(R.string.pref_send_test_intent)
        )
        sendTestIntentPreference?.setOnPreferenceClickListener {
            sendTestIntent(activity)
            true
        }

        val sourceCodePreference = preferenceManager.findPreference<Preference>(
            activity.getString(R.string.pref_view_source_code)
        )
        sourceCodePreference?.setOnPreferenceClickListener {
            openLink(activity, activity.getString(R.string.source_code_link))
            true
        }

        val issueTrackerPreference = preferenceManager.findPreference<Preference>(
            activity.getString(R.string.pref_view_issue_tracker)
        )
        issueTrackerPreference?.setOnPreferenceClickListener {
            openLink(activity, activity.getString(R.string.issue_tracker_link))
            true
        }

        val licensePreference = preferenceManager.findPreference<Preference>(
            activity.getString(R.string.pref_license)
        )
        licensePreference?.setOnPreferenceClickListener {
            openLink(activity, activity.getString(R.string.license_link))
            true
        }
    }

    private fun switchInterceptEnabled(context: Context, isEnabled: Boolean) {
        val flag = if (isEnabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val component = ComponentName(context, InterceptActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            component, flag,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun sendTestIntent(activity: Activity) {
        val intent = ShareCompat.IntentBuilder
            .from(activity)
            .setChooserTitle(activity.getString(R.string.send_test_intent_chooser_title))
            .setType(activity.getString(R.string.mime_type_text_plain))
            .setText(activity.getString(R.string.send_test_intent_content))
            .createChooserIntent()
        activity.startActivity(intent)
    }

    private fun openLink(activity: Activity, link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link)
        activity.startActivity(intent)
    }
}