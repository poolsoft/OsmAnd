package net.osmand.plus.plugins.musiccontrol;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;

import java.util.ArrayList;
import java.util.List;

public class MusicControlPlugin extends OsmandPlugin {

    private final OsmandSettings settings;

    public MusicControlPlugin(@NonNull OsmandApplication app) {
        super(app);
        settings = app.getSettings();
    }

    @Override
    public String getId() {
        return "music_control";
    }

    @Override
    public String getName() {
        return app.getString(R.string.shared_string_music);
    }

    @Override
    public CharSequence getDescription(boolean linksEnabled) {
        return "Control music playback from OsmAnd";
    }

    @Override
    public int getLogoResourceId() {
        return R.drawable.ic_action_music;
    }

    @Override
    public Drawable getAssetResourceImage() {
        return app.getUIUtilities().getIcon(R.drawable.ic_action_music);
    }

    @Nullable
    @Override
    public SettingsScreenType getSettingsScreenType() {
        return null; // For now, no settings
    }

    @Override
    public void registerOptionsMenuItems(@NonNull net.osmand.plus.activities.MapActivity mapActivity,
                                       @NonNull ContextMenuAdapter helper) {
        // Add music control options to context menu if needed
    }

    @Override
    protected List<QuickActionType> getQuickActionTypes() {
        List<QuickActionType> quickActionTypes = new ArrayList<>();
        // Add quick actions for play/pause, next, previous
        return quickActionTypes;
    }

    // Music control methods
    public void playPauseMusic() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    public void nextTrack() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public void previousTrack() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    private void sendMediaKey(int keyCode) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        app.sendBroadcast(intent);

        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        app.sendBroadcast(intent);
    }
}
