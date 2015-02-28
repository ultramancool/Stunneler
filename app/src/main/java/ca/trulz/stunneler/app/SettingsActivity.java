package ca.trulz.stunneler.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

public class SettingsActivity extends ActionBarActivity
{

    public static final String CONFIG_FILE = "filePicker";

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new StunnelerPreferenceFragment()).commit();
    }

    public static class StunnelerPreferenceFragment extends PreferenceFragment
    {
        private static final int OPEN_CONFIG = 1;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            Preference filePicker = (Preference) findPreference(CONFIG_FILE);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            filePicker.setSummary(sharedPref.getString(CONFIG_FILE,""));
            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("org.openintents.action.PICK_FILE");
                    startActivityForResult(intent, OPEN_CONFIG);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Preference filePicker = (Preference) findPreference(CONFIG_FILE);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String filePath = data.getData().getPath();
            sharedPref.edit().putString(CONFIG_FILE, filePath).apply();
            filePicker.setSummary(filePath);
        }
    }
}