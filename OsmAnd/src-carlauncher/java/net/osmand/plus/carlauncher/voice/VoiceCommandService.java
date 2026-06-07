package net.osmand.plus.carlauncher.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
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
    private TextToSpeech tts;
    
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
        
        // TextToSpeech motorunu baslat (Turkce karakter yok)
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("tr", "TR"));
            }
        });
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
                speak("Muzik oynatiliyor");
                if (musicManager != null) {
                    musicManager.togglePlayPause();
                }
            } else if (text.contains("muzik") && (text.contains("durdur") || text.contains("duraklat") || text.contains("kes"))) {
                speak("Muzik durduruldu");
                if (musicManager != null) {
                    musicManager.togglePlayPause();
                }
            } else if (text.contains("sonraki") || text.contains("atla")) {
                speak("Sonraki sarki");
                if (musicManager != null) {
                    musicManager.skipToNext();
                }
            } else if (text.contains("onceki") || text.contains("geri")) {
                speak("Onceki sarki");
                if (musicManager != null) {
                    musicManager.skipToPrevious();
                }
            } else if (text.contains("sesi") && (text.contains("kapa") || text.contains("kapat") || text.contains("sustur") || text.contains("sessize"))) {
                try {
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
                        speak("Ses kapatildi");
                    }
                } catch (Exception e) {}
            } else if (text.contains("sesi") && (text.contains("yuzde") || text.contains("yüzde"))) {
                int pct = parsePercentage(text);
                if (pct >= 0 && pct <= 100) {
                    try {
                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (audioManager != null) {
                            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int targetVol = (pct * maxVol) / 100;
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI);
                            speak("Ses yuzde " + pct + " yapildi.");
                        }
                    } catch (Exception e) {
                        speak("Ses ayarlanamadi.");
                    }
                } else {
                    speak("Gecersiz ses yuzdesi.");
                }
            } else if (text.contains("sesi") && (text.contains("ac") || text.contains("yukselt") || text.contains("artir"))) {
                adjustVolume(true);
            } else if (text.contains("sesi") && (text.contains("kis") || text.contains("azalt") || text.contains("dusur"))) {
                adjustVolume(false);
            } else if (text.contains("ekran") && (text.contains("kapat") || text.contains("kapa"))) {
                speak("Ekran kapatiliyor.");
                try {
                    Intent screenIntent = new Intent("xy.android.setScreenState");
                    screenIntent.putExtra("screenstate", 2);
                    sendBroadcast(screenIntent);
                } catch (Exception e) {
                    android.util.Log.e("VoiceCommandService", "Ekran kapatma hatasi", e);
                }
            } else if (text.contains("saat") && (text.contains("kac") || text.contains("soyle"))) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", new java.util.Locale("tr", "TR"));
                    String timeStr = sdf.format(new java.util.Date());
                    speak("Saat su an " + timeStr);
                } catch (Exception e) {}
            } else if (text.contains("tarih") && (text.contains("nedir") || text.contains("soyle") || text.contains("gunlerden"))) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("tr", "TR"));
                    String dateStr = sdf.format(new java.util.Date());
                    speak("Bugun " + dateStr);
                } catch (Exception e) {}
            } else if (text.contains("eve") && (text.contains("gotur") || text.contains("git"))) {
                startNavigationTo("home");
            } else if (text.contains("ise") && (text.contains("gotur") || text.contains("git"))) {
                startNavigationTo("work");
            } else if (text.contains("harita") || text.contains("navigasyon") || text.contains("yol")) {
                openExternalMap();
            } else {
                speak("Anlasilamayan komut: " + text);
            }
        });
    }

    private void adjustVolume(boolean increase) {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int direction = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
                speak(increase ? "Ses yukseltildi" : "Ses azaltildi");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void speak(String text) {
        if (tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VoiceAssistant");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private int parsePercentage(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (Exception e) {}
        }
        if (text.contains("yuz") || text.contains("yüz")) return 100;
        if (text.contains("doksan")) return 90;
        if (text.contains("seksen")) return 80;
        if (text.contains("yetmis") || text.contains("yetmiş")) return 70;
        if (text.contains("altmis") || text.contains("altmış")) return 60;
        if (text.contains("elli")) return 50;
        if (text.contains("kirk") || text.contains("kırk")) return 40;
        if (text.contains("otuz")) return 30;
        if (text.contains("yirmi")) return 20;
        if (text.contains("on")) return 10;
        if (text.contains("sifir") || text.contains("sıfır")) return 0;
        return -1;
    }

    private void startNavigationTo(String type) {
        try {
            OsmandApplication app = (OsmandApplication) getApplication();
            FavouritesHelper favoritesHelper = app.getFavoritesHelper();
            boolean hasPoint = false;
            
            if ("home".equals(type)) {
                hasPoint = favoritesHelper.getSpecialPoint(net.osmand.data.SpecialPointType.HOME) != null;
            } else if ("work".equals(type)) {
                hasPoint = favoritesHelper.getSpecialPoint(net.osmand.data.SpecialPointType.WORK) != null;
            }

            if (!hasPoint) {
                speak(("home".equals(type) ? "Ev" : "Is") + " adresi OsmAnd icinde tanimli degil.");
                return;
            }

            String shortcutId = "home".equals(type) ? "navigate_to_home" : "navigate_to_work";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("osmand.shortcuts://shortcut?id=" + shortcutId));
            intent.setComponent(new ComponentName(this, net.osmand.plus.activities.MapActivity.class));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            speak(("home".equals(type) ? "Eve" : "Ise") + " navigasyon baslatiliyor.");
        } catch (Exception e) {
            android.util.Log.e("VoiceCommandService", "Navigasyon baslatilamadi", e);
            speak("Navigasyon baslatilamadi.");
        }
    }

    private void openExternalMap() {
        PackageManager pm = getPackageManager();
        Intent intent = null;
        
        try {
            intent = pm.getLaunchIntentForPackage("com.google.android.apps.maps");
        } catch (Exception e) {}
        
        if (intent == null) {
            try {
                intent = pm.getLaunchIntentForPackage("ru.yandex.yandexnavi");
            } catch (Exception e) {}
        }
        
        if (intent == null) {
            try {
                intent = new Intent(this, net.osmand.plus.activities.MapActivity.class);
            } catch (Exception e) {}
        }
        
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            speak("Harita aciliyor.");
        } else {
            speak("Harita uygulamasi bulunamadi.");
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
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
