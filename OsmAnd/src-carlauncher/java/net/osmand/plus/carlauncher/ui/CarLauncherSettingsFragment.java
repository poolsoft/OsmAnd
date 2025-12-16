package net.osmand.plus.carlauncher.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.carlauncher.CarLauncherSettings;

public class CarLauncherSettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "CarLauncherSettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(net.osmand.plus.R.xml.carlauncher_prefs, rootKey);

        setupStatusBarPref();
        setupMusicAppPref();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(0xFF111111); // Dark background
    }

    private void setupStatusBarPref() {
        SwitchPreferenceCompat statusBarPref = findPreference("car_launcher_status_bar");
        if (statusBarPref != null) {
            statusBarPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean show = (Boolean) newValue;
                // TODO: Apply immediately via Broadcast or Callback
                if (getActivity() != null) {
                    // Temporary: Restart activity or apply insets
                }
                return true;
            });
        }
    }

    private void setupMusicAppPref() {
        ListPreference musicPref = findPreference("car_launcher_music_app");
        if (musicPref != null) {
            // Populate with installed music apps logic here in future
            musicPref.setEntries(new CharSequence[] { "Dahili Player", "Spotify", "YouTube Music" });
            musicPref.setEntryValues(
                    new CharSequence[] { "internal", "com.spotify.music", "com.google.android.apps.youtube.music" });
        }
    }
}
