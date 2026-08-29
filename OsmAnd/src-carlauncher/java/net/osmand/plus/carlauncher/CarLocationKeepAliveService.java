package net.osmand.plus.carlauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps GPS delivery alive for the floating speed button without claiming AIS usage. */
public final class CarLocationKeepAliveService extends Service implements LocationListener {
    private static final String CHANNEL_ID = "car_launcher_location";
    private static final int NOTIFICATION_ID = 20761;
    private static final AtomicBoolean START_REQUESTED = new AtomicBoolean();
    private LocationManager locationManager;

    public static void start(Context context) {
        if (!START_REQUESTED.compareAndSet(false, true)) {
            return;
        }
        try {
            // This request is made while the launcher activity is visible. A foreground-service
            // start would impose a five-second deadline, but on low-end units the map can block
            // the main thread longer than that before Service.onCreate() can run. Start normally
            // and promote immediately from onCreate(); if Android considers the app background,
            // skip forced GPS instead of crashing the launcher.
            context.startService(new Intent(context, CarLocationKeepAliveService.class));
        } catch (IllegalStateException | SecurityException e) {
            START_REQUESTED.set(false);
        }
    }

    public static void stop(Context context) {
        if (!START_REQUESTED.compareAndSet(true, false)) {
            return;
        }
        // A pending normal service start may safely be cancelled without triggering Android's
        // foreground-service deadline exception.
        context.stopService(new Intent(context, CarLocationKeepAliveService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.car_location_service_channel), NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(this, MapActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(getString(R.string.car_location_service_title))
                .setContentText(getString(R.string.car_location_service_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this);
        } catch (SecurityException | IllegalArgumentException ignored) {
            START_REQUESTED.set(false);
            stopForeground(true);
            stopSelfResult(startId);
        }
        return START_NOT_STICKY;
    }

    @Override public void onLocationChanged(Location location) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }

    @Override
    public void onDestroy() {
        START_REQUESTED.set(false);
        removeLocationUpdates();
        super.onDestroy();
    }

    private void removeLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) { }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
