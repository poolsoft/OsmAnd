package net.osmand.plus.carlauncher.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.osmand.plus.carlauncher.music.MusicManager;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Car Launcher icin tamamen offline sesli komut servisi.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class VoiceCommandService extends Service implements RecognitionListener {

    private static final String CHANNEL_ID = "VoiceCommandServiceChannel";
    private static final int NOTIFICATION_ID = 5005;
    private static final String MODEL_ZIP_URL = "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip";
    
    private SpeechService speechService;
    private Model model;
    private Recognizer wakeWordRecognizer;
    private Recognizer commandRecognizer;
    
    private boolean isListeningForCommand = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MusicManager musicManager;
    
    // Servisin aktiflik durumunu tutan statik bayrak (Turkce karakter yok)
    public static boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        musicManager = MusicManager.getInstance(getApplicationContext());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Sesli kontrol sistemi yukleniyor..."));
        checkAndPrepareModel();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sesli Kontrol Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null); // Sessiz bildirim
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, net.osmand.plus.activities.MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sesli Kontrol Aktif")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private void checkAndPrepareModel() {
        File modelDir = new File(getExternalFilesDir(null), "vosk-model-tr");
        if (modelDir.exists() && isModelDirectoryValid(modelDir)) {
            loadModel(modelDir.getAbsolutePath());
        } else {
            downloadAndExtractModel(modelDir);
        }
    }

    private boolean isModelDirectoryValid(File dir) {
        // Model klasorunde gerekli dosyalar var mi kontrol ediyoruz
        File amDir = new File(dir, "am");
        File graphDir = new File(dir, "graph");
        return amDir.exists() || graphDir.exists();
    }

    private void loadModel(String modelPath) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                model = new Model(modelPath);
                
                // Wake word algilama icin hafif kistli gramer (Turkce karakter yok)
                wakeWordRecognizer = new Recognizer(model, 16000.0f, "[\"hey car\", \"hey kar\", \"hey kart\", \"hey\"]");
                
                // Genel komut cozumu icin serbest recognizer
                commandRecognizer = new Recognizer(model, 16000.0f);
                
                handler.post(() -> {
                    updateNotification("\"Hey Car\" tetikleme kelimesi bekleniyor...");
                    startSpeechService(wakeWordRecognizer);
                });
            } catch (Exception e) {
                android.util.Log.e("VoiceCommandService", "Model yukleme hatasi", e);
                handler.post(() -> {
                    Toast.makeText(VoiceCommandService.this, "Ses modeli yuklenemedi", Toast.LENGTH_LONG).show();
                    stopSelf();
                });
            }
        });
    }

    private void startSpeechService(Recognizer recognizer) {
        try {
            if (speechService != null) {
                speechService.stop();
                speechService = null;
            }
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
        } catch (Exception e) {
            android.util.Log.e("VoiceCommandService", "Speech service baslatilamadi", e);
        }
    }

    private void downloadAndExtractModel(File targetDir) {
        updateNotification("Ses modeli indiriliyor (Lutfen bekleyin)...");
        Toast.makeText(this, "Ses modeli indiriliyor, bu islem biraz zaman alabilir...", Toast.LENGTH_LONG).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            File tempZip = new File(getExternalFilesDir(null), "vosk-model-tr.zip");
            try {
                URL url = new URL(MODEL_ZIP_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Sunucu hatasi: " + connection.getResponseMessage());
                }

                int fileLength = connection.getContentLength();
                InputStream input = new BufferedInputStream(connection.getInputStream(), 8192);
                FileOutputStream output = new FileOutputStream(tempZip);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                long lastUpdateTime = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);

                    // Bildirimi periyodik olarak guncelliyoruz
                    long now = System.currentTimeMillis();
                    if (now - lastUpdateTime > 1000) {
                        int progress = (int) (total * 100 / fileLength);
                        updateNotification("Model indiriliyor: %" + progress);
                        lastUpdateTime = now;
                    }
                }

                output.flush();
                output.close();
                input.close();

                updateNotification("Model dosyasi zipten cikariliyor...");
                unzip(tempZip, targetDir.getParentFile());

                File extractedDir = new File(targetDir.getParentFile(), "vosk-model-small-tr-0.3");
                if (extractedDir.exists()) {
                    extractedDir.renameTo(targetDir);
                }

                if (tempZip.exists()) {
                    tempZip.delete();
                }

                handler.post(() -> {
                    Toast.makeText(VoiceCommandService.this, "Ses modeli kuruldu!", Toast.LENGTH_SHORT).show();
                    loadModel(targetDir.getAbsolutePath());
                });

            } catch (Exception e) {
                android.util.Log.e("VoiceCommandService", "Model indirme/kurulum hatasi", e);
                if (tempZip.exists()) {
                    tempZip.delete();
                }
                handler.post(() -> {
                    Toast.makeText(VoiceCommandService.this, "Model indirilemedi, internet baglantisini kontrol edin", Toast.LENGTH_LONG).show();
                    stopSelf();
                });
            }
        });
    }

    private void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("Klasor olusturulamadi: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    // --- Vosk RecognitionListener Metotları ---

    @Override
    public void onResult(String hypothesis) {
        parseAndProcessCommand(hypothesis);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        parseAndProcessCommand(hypothesis);
    }

    @Override
    public void onPartialResult(String hypothesis) {
    }

    @Override
    public void onError(Exception exception) {
        android.util.Log.e("VoiceCommandService", "Vosk tanima hatasi", exception);
    }

    @Override
    public void onTimeout() {
        if (isListeningForCommand) {
            switchToWakeWordMode();
        }
    }

    private void parseAndProcessCommand(String hypothesis) {
        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.optString("text", "").toLowerCase(Locale.getDefault()).trim();
            if (text.isEmpty()) return;

            android.util.Log.d("VoiceCommandService", "Algilanan Metin: " + text);

            if (!isListeningForCommand) {
                if (text.contains("hey car") || text.contains("hey kar") || text.contains("hey kart") || text.contains("hey")) {
                    triggerWakeWordReaction();
                }
            } else {
                executeVoiceCommand(text);
                switchToWakeWordMode();
            }
        } catch (Exception e) {
            android.util.Log.e("VoiceCommandService", "JSON parse hatasi", e);
        }
    }

    private void triggerWakeWordReaction() {
        isListeningForCommand = true;
        updateNotification("Dinliyorum...");

        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {
            // ignore
        }

        startSpeechService(commandRecognizer);

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::switchToWakeWordMode, 5000);
    }

    private void switchToWakeWordMode() {
        if (!isListeningForCommand) return;
        isListeningForCommand = false;
        updateNotification("\"Hey Car\" tetikleme kelimesi bekleniyor...");
        startSpeechService(wakeWordRecognizer);
    }

    private void executeVoiceCommand(String text) {
        handler.post(() -> {
            if (text.contains("muzik") && (text.contains("cal") || text.contains("oynat") || text.contains("baslat"))) {
                Toast.makeText(VoiceCommandService.this, "Muzik oynatiliyor", Toast.LENGTH_SHORT).show();
                if (musicManager != null) {
                    musicManager.togglePlayPause();
                }
            } else if (text.contains("muzik") && (text.contains("durdur") || text.contains("duraklat") || text.contains("kes"))) {
                Toast.makeText(VoiceCommandService.this, "Muzik durduruldu", Toast.LENGTH_SHORT).show();
                if (musicManager != null) {
                    musicManager.togglePlayPause();
                }
            } else if (text.contains("sonraki") || text.contains("atla")) {
                Toast.makeText(VoiceCommandService.this, "Sonraki sarki", Toast.LENGTH_SHORT).show();
                if (musicManager != null) {
                    musicManager.skipToNext();
                }
            } else if (text.contains("onceki") || text.contains("geri")) {
                Toast.makeText(VoiceCommandService.this, "Onceki sarki", Toast.LENGTH_SHORT).show();
                if (musicManager != null) {
                    musicManager.skipToPrevious();
                }
            } else if (text.contains("sesi") && (text.contains("ac") || text.contains("yukselt") || text.contains("artir"))) {
                adjustVolume(true);
            } else if (text.contains("sesi") && (text.contains("kis") || text.contains("azalt") || text.contains("dusur"))) {
                adjustVolume(false);
            } else if (text.contains("harita") || text.contains("navigasyon") || text.contains("yol")) {
                Toast.makeText(VoiceCommandService.this, "Harita aciliyor", Toast.LENGTH_SHORT).show();
                try {
                    Intent mapIntent = new Intent(VoiceCommandService.this, net.osmand.plus.activities.MapActivity.class);
                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mapIntent);
                } catch (Exception e) {
                    // ignore
                }
            } else {
                Toast.makeText(VoiceCommandService.this, "Anlasilamayan komut: " + text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void adjustVolume(boolean increase) {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int direction = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
                Toast.makeText(this, increase ? "Ses yukseltildi" : "Ses azaltildi", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
