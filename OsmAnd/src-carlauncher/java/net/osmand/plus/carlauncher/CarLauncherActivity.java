package net.osmand.plus.carlauncher;

import android.os.Bundle;
import android.util.Log;

import net.osmand.plus.activities.MapActivity;

/**
 * CarLauncher variant'ı için özelleştirilmiş MapActivity.
 * Şu an minimal - gelecekte özel UI/widget eklenebilir.
 */
public class CarLauncherActivity extends MapActivity {

    private static final String TAG = "CarLauncherActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "CarLauncher activity started");
        
        // Gelecekte: Özel panel, widget veya UI değişiklikleri buraya eklenebilir
        // initCarLauncherUI();
    }
}
