package net.osmand.plus.carlauncher.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Persists low-overhead startup milestones where they can be copied without ADB. */
public final class StartupPerformanceRecorder {

    private static final StartupPerformanceRecorder INSTANCE = new StartupPerformanceRecorder();
    private static final long WRITE_DEBOUNCE_MS = 400L;

    private final Object lock = new Object();
    private final List<String> lines = new ArrayList<>();
    private final ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "CarStartupReportWriter");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private Context appContext;
    private long sessionStartElapsed;
    private String fileName;
    private Uri mediaStoreUri;
    private ScheduledFuture<?> pendingWrite;

    private StartupPerformanceRecorder() {
    }

    public static StartupPerformanceRecorder getInstance() {
        return INSTANCE;
    }

    public void startSession(@NonNull Context context, boolean lowRamProfile) {
        synchronized (lock) {
            if (sessionStartElapsed != 0L) {
                return;
            }
            appContext = context.getApplicationContext();
            sessionStartElapsed = SystemClock.elapsedRealtime();
            fileName = "osmand_startup_" + new SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
            lines.add("OsmAnd Auto startup performance report");
            lines.add("Created: " + new Date());
            lines.add("Device: " + Build.MANUFACTURER + " " + Build.MODEL
                    + " | Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
            lines.add("Profile: " + (lowRamProfile ? "LOW_RAM" : "STANDARD"));
            lines.add("File: Download/OsmAndAuto/" + fileName);
            lines.add("");
            appendLocked("SESSION_START");
            scheduleWriteLocked(false);
        }
    }

    public void record(@NonNull Context context, @NonNull String event) {
        ensureSession(context);
        synchronized (lock) {
            appendLocked(event);
            scheduleWriteLocked(false);
        }
    }

    public void recordAndFlush(@NonNull Context context, @NonNull String event) {
        ensureSession(context);
        synchronized (lock) {
            appendLocked(event);
            scheduleWriteLocked(true);
        }
    }

    private void ensureSession(Context context) {
        if (sessionStartElapsed == 0L) {
            startSession(context, false);
        }
    }

    private void appendLocked(String event) {
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - sessionStartElapsed);
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        lines.add(String.format(Locale.US, "+%6d ms | heap=%4d MB | %s",
                elapsed, usedMb, event));
    }

    private void scheduleWriteLocked(boolean immediate) {
        if (appContext == null || fileName == null) {
            return;
        }
        if (pendingWrite != null) {
            pendingWrite.cancel(false);
        }
        pendingWrite = writer.schedule(this::writeSnapshot,
                immediate ? 0L : WRITE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private void writeSnapshot() {
        Context context;
        String name;
        String text;
        synchronized (lock) {
            context = appContext;
            name = fileName;
            text = String.join("\n", lines) + "\n";
        }
        if (context == null || name == null) {
            return;
        }
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeWithMediaStore(context, name, bytes);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "OsmAndAuto");
                if (!directory.exists() && !directory.mkdirs()) {
                    return;
                }
                try (OutputStream output = new FileOutputStream(new File(directory, name), false)) {
                    output.write(bytes);
                }
            }
        } catch (Exception ignored) {
            // Startup reporting must never affect launcher availability.
        }
    }

    private void writeWithMediaStore(Context context, String name, byte[] bytes) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        if (mediaStoreUri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/OsmAndAuto");
            mediaStoreUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        if (mediaStoreUri != null) {
            try (OutputStream output = resolver.openOutputStream(mediaStoreUri, "wt")) {
                if (output != null) {
                    output.write(bytes);
                }
            }
        }
    }
}
