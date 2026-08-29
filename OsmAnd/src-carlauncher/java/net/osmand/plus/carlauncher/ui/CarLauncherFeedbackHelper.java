package net.osmand.plus.carlauncher.ui;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;

/** Car-launcher crash sharing with an explicit ClipData URI permission grant. */
public final class CarLauncherFeedbackHelper extends FeedbackHelper {
    private final OsmandApplication app;

    public CarLauncherFeedbackHelper(@NonNull OsmandApplication app) {
        super(app);
        this.app = app;
    }

    @Override
    public void sendCrashLog() {
        File crashLog = getCrashLog();
        if (crashLog == null) {
            app.showToastMessage(R.string.data_is_not_available);
            return;
        }
        sendCrashLog(crashLog);
    }

    @Override
    public void sendCrashLog(@NonNull File file) {
        Uri uri = AndroidUtils.getUriForFile(app, file);
        Intent sendIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_EMAIL, new String[] {"crash@osmand.net"})
                .putExtra(Intent.EXTRA_SUBJECT, "OsmAnd Auto bug")
                .putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
        sendIntent.setClipData(ClipData.newRawUri("OsmAnd Auto crash log", uri));
        Intent chooser = Intent.createChooser(sendIntent, app.getString(R.string.send_report))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        AndroidUtils.startActivityIfSafe(app, sendIntent, chooser);
    }
}
