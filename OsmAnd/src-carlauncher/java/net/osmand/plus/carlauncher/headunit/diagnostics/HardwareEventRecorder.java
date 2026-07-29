package net.osmand.plus.carlauncher.headunit.diagnostics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Launcher-only recorder for head-unit, steering-wheel and media events.
 * The log stays in app-private storage until the user explicitly exports it.
 */
public final class HardwareEventRecorder {

    private static final String PREFS_NAME = "car_hardware_event_recorder";
    private static final String KEY_RECORDING = "recording";
    private static final String LOG_FILE_NAME = "hardware_events.log";
    private static final String OLD_LOG_FILE_NAME = "hardware_events.previous.log";
    private static final long MAX_LOG_BYTES = 4L * 1024L * 1024L;

    private static final String[] DIAGNOSTIC_ACTIONS = {
            Intent.ACTION_MEDIA_BUTTON,
            "xy.android.playpause",
            "xy.android.nextmedia",
            "xy.android.previousmedia",
            "xy.android.forceplay",
            "xy.android.forcepause",
            "update.widget.playbtnstate",
            "update.widget.songname",
            "update.widget.update_proBar",
            "com.acloud.intent.play_status",
            "com.android.radio.widget.freq_volue",
            "com.auto.apimediaplayer.notification.NEXT",
            "com.auto.apimediaplayer.notification.PREV",
            "com.auto.apimediaplayer.notification.PLAYPAUSE",
            "com.txznet.extra.next",
            "com.txznet.extra.pre",
            "com.txznet.extra.play",
            "com.txznet.extra.pause"
    };

    private static volatile HardwareEventRecorder instance;

    private final Context appContext;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final File logFile;
    private final File oldLogFile;
    private volatile boolean recording;
    private BroadcastReceiver diagnosticReceiver;

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String errorMessage);
    }

    private HardwareEventRecorder(@NonNull Context context) {
        appContext = context.getApplicationContext();
        File directory = new File(appContext.getFilesDir(), "carlauncher_diagnostics");
        logFile = new File(directory, LOG_FILE_NAME);
        oldLogFile = new File(directory, OLD_LOG_FILE_NAME);
        recording = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_RECORDING, false);
        if (recording) {
            registerDiagnosticReceiver();
            appendSessionHeader("RECORDER_RESTORED");
        }
    }

    public static HardwareEventRecorder getInstance(@NonNull Context context) {
        HardwareEventRecorder result = instance;
        if (result == null) {
            synchronized (HardwareEventRecorder.class) {
                result = instance;
                if (result == null) {
                    result = new HardwareEventRecorder(context);
                    instance = result;
                }
            }
        }
        return result;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean enabled) {
        if (recording == enabled) {
            return;
        }
        recording = enabled;
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_RECORDING, enabled)
                .apply();
        if (enabled) {
            registerDiagnosticReceiver();
            appendSessionHeader("RECORDING_STARTED");
        } else {
            recordForced("RECORDER", "RECORDING_STOPPED");
            unregisterDiagnosticReceiver();
        }
    }

    public void recordKeyEvent(@NonNull String source, @Nullable KeyEvent event) {
        if (!recording || event == null) {
            return;
        }
        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        String deviceName = device != null ? device.getName() : "unknown";
        String message = "action=" + keyActionToString(event.getAction())
                + " keyCode=" + event.getKeyCode()
                + " keyName=" + KeyEvent.keyCodeToString(event.getKeyCode())
                + " scanCode=" + event.getScanCode()
                + " deviceId=" + event.getDeviceId()
                + " device=\"" + sanitize(deviceName) + "\""
                + " source=0x" + Integer.toHexString(event.getSource())
                + " repeat=" + event.getRepeatCount()
                + " flags=0x" + Integer.toHexString(event.getFlags());
        record(source, message);
    }

    private String keyActionToString(int action) {
        if (action == KeyEvent.ACTION_DOWN) {
            return "ACTION_DOWN";
        } else if (action == KeyEvent.ACTION_UP) {
            return "ACTION_UP";
        } else if (action == KeyEvent.ACTION_MULTIPLE) {
            return "ACTION_MULTIPLE";
        }
        return Integer.toString(action);
    }

    public void recordIntent(@NonNull String source, @Nullable Intent intent) {
        if (!recording || intent == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append("action=").append(sanitize(intent.getAction()));
        if (intent.getPackage() != null) {
            message.append(" package=").append(sanitize(intent.getPackage()));
        }
        if (intent.getComponent() != null) {
            message.append(" component=").append(sanitize(intent.getComponent().flattenToShortString()));
        }
        Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {
            message.append(" extras={").append(bundleToString(extras)).append('}');
        }
        record(source, message.toString());
    }

    public void record(@NonNull String source, @NonNull String message) {
        if (recording) {
            recordForced(source, message);
        }
    }

    public long getLogSizeBytes() {
        return (logFile.exists() ? logFile.length() : 0)
                + (oldLogFile.exists() ? oldLogFile.length() : 0);
    }

    public boolean hasLogs() {
        return getLogSizeBytes() > 0;
    }

    public void clear(@Nullable CompletionCallback callback) {
        writer.execute(() -> {
            boolean success = (!logFile.exists() || logFile.delete())
                    & (!oldLogFile.exists() || oldLogFile.delete());
            postResult(callback, success, success ? null : "Log files could not be deleted");
        });
    }

    public void exportToUri(@NonNull Uri uri, @Nullable CompletionCallback callback) {
        writer.execute(() -> {
            try (OutputStream rawOutput = appContext.getContentResolver().openOutputStream(uri, "wt")) {
                if (rawOutput == null) {
                    throw new IOException("Output stream is unavailable");
                }
                try (BufferedOutputStream output = new BufferedOutputStream(rawOutput)) {
                    writeExportHeader(output);
                    copyIfExists(oldLogFile, output);
                    copyIfExists(logFile, output);
                    output.flush();
                }
                postResult(callback, true, null);
            } catch (Exception e) {
                postResult(callback, false, e.getMessage());
            }
        });
    }

    private void appendSessionHeader(@NonNull String reason) {
        String version = "unknown";
        try {
            version = appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        recordForced("SESSION", reason
                + " app=" + sanitize(version)
                + " manufacturer=" + sanitize(Build.MANUFACTURER)
                + " brand=" + sanitize(Build.BRAND)
                + " model=" + sanitize(Build.MODEL)
                + " device=" + sanitize(Build.DEVICE)
                + " product=" + sanitize(Build.PRODUCT)
                + " sdk=" + Build.VERSION.SDK_INT
                + " release=" + sanitize(Build.VERSION.RELEASE));
        for (int deviceId : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null) {
                recordForced("INPUT_DEVICE", "id=" + deviceId
                        + " name=\"" + sanitize(device.getName()) + "\""
                        + " descriptor=\"" + sanitize(device.getDescriptor()) + "\""
                        + " sources=0x" + Integer.toHexString(device.getSources())
                        + " keyboardType=" + device.getKeyboardType()
                        + " virtual=" + device.isVirtual());
            }
        }
    }

    private void recordForced(@NonNull String source, @NonNull String message) {
        String timestamp = String.format(Locale.US, "%1$tF %1$tT.%1$tL", new Date());
        String line = timestamp + " [" + sanitize(source) + "] " + sanitize(message) + "\n";
        writer.execute(() -> appendLine(line));
    }

    private void appendLine(@NonNull String line) {
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            rotateIfNeeded();
            try (FileOutputStream output = new FileOutputStream(logFile, true)) {
                output.write(line.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private void rotateIfNeeded() {
        if (!logFile.exists() || logFile.length() < MAX_LOG_BYTES) {
            return;
        }
        if (oldLogFile.exists()) {
            oldLogFile.delete();
        }
        logFile.renameTo(oldLogFile);
    }

    private void registerDiagnosticReceiver() {
        if (diagnosticReceiver != null) {
            return;
        }
        diagnosticReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                recordIntent("BROADCAST", intent);
            }
        };
        IntentFilter filter = new IntentFilter();
        for (String action : DIAGNOSTIC_ACTIONS) {
            filter.addAction(action);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(diagnosticReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(diagnosticReceiver, filter);
            }
        } catch (Exception e) {
            recordForced("RECORDER", "receiver_registration_failed=" + e.getClass().getSimpleName()
                    + ":" + e.getMessage());
            diagnosticReceiver = null;
        }
    }

    private void unregisterDiagnosticReceiver() {
        if (diagnosticReceiver == null) {
            return;
        }
        try {
            appContext.unregisterReceiver(diagnosticReceiver);
        } catch (Exception ignored) {
        }
        diagnosticReceiver = null;
    }

    private void writeExportHeader(@NonNull OutputStream output) throws IOException {
        String header = "# OsmAnd Car Launcher Hardware Event Diagnostic\n"
                + "# Exported: " + String.format(Locale.US, "%1$tF %1$tT.%1$tL", new Date()) + "\n"
                + "# Sources: Activity KeyEvent, MediaSession, known head-unit broadcasts and adapters\n\n";
        output.write(header.getBytes(StandardCharsets.UTF_8));
    }

    private static void copyIfExists(@NonNull File source, @NonNull OutputStream output)
            throws IOException {
        if (!source.exists()) {
            return;
        }
        String section = "\n# FILE: " + source.getName() + "\n";
        output.write(section.getBytes(StandardCharsets.UTF_8));
        byte[] buffer = new byte[8192];
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(source))) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    @NonNull
    private static String bundleToString(@NonNull Bundle bundle) {
        Set<String> keys = new TreeSet<>(bundle.keySet());
        StringBuilder result = new StringBuilder();
        for (String key : keys) {
            if (result.length() > 0) {
                result.append(", ");
            }
            Object value;
            try {
                value = bundle.get(key);
            } catch (Exception e) {
                value = "<unreadable>";
            }
            result.append(sanitize(key)).append('=').append(valueToString(value));
            if (result.length() > 2000) {
                result.append("...");
                break;
            }
        }
        return result.toString();
    }

    @NonNull
    private static String valueToString(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Object[]) {
            return sanitize(Arrays.deepToString((Object[]) value));
        }
        return sanitize(String.valueOf(value));
    }

    @NonNull
    private static String sanitize(@Nullable String value) {
        if (value == null) {
            return "null";
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ');
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) + "..." : sanitized;
    }

    private void postResult(@Nullable CompletionCallback callback, boolean success,
            @Nullable String errorMessage) {
        if (callback != null) {
            mainHandler.post(() -> callback.onComplete(success, errorMessage));
        }
    }
}
