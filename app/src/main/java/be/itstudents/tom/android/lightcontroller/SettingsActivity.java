package be.itstudents.tom.android.lightcontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import be.itstudents.tom.android.lightcontroller.Remote.RemoteConstants;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private EditTextPreference mEditTextPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.preference);
        PreferenceManager.setDefaultValues(SettingsActivity.this, R.xml.preference,
                false);
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        mEditTextPref = (EditTextPreference) findPreference("pref_remote_server_port");
        mEditTextPref.setSummary(String.format(getResources().getString(R.string.pref_remote_server_port_summary), sharedPreferences.getInt("pref_remote_server_port", RemoteConstants.DEFAULT_PORT)));
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        if (key.equals("pref_remote_server_port")) {
            mEditTextPref.setSummary(String.format(getResources().getString(R.string.pref_remote_server_port_summary), sharedPreferences.getInt("pref_remote_server_port", RemoteConstants.DEFAULT_PORT)));
        }
    }
}