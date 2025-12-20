package net.osmand.plus.carlauncher.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles playback of local audio files using MediaPlayer.
 * Manages queue and playback state.
 * Includes Audio Focus management for Car interaction (Nav, Calls).
 */
public class InternalMusicPlayer {

    private static final String TAG = "InternalMusicPlayer";

    public interface PlaybackListener {
        void onTrackChanged(MusicRepository.AudioTrack track);

        void onPlaybackStateChanged(boolean isPlaying);

        void onCompletion();
    }

    private final Context context;
    private final AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private List<MusicRepository.AudioTrack> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isPrepared = false;
    private boolean playOnFocusGain = false; // Focus geri geldiğinde çalmaya devam etsin mi?
    private PlaybackListener listener;

    public InternalMusicPlayer(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        initMediaPlayer();
    }

    public void setListener(PlaybackListener listener) {
        this.listener = listener;
    }

    // --- Audio Focus Listener (Navigasyon ve Aramalar için) ---
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Kalıcı kayıp (Başka müzik uygulaması açıldı veya arama var)
                playOnFocusGain = false;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Geçici kayıp (Kısa konuşma vs.)
                if (isPlaying()) {
                    playOnFocusGain = true;
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Navigasyon konuşuyor -> Sesi kıs
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // Odak geri geldi
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f); // Sesi normale döndür
                }
                if (playOnFocusGain) {
                    play();
                }
                playOnFocusGain = false;
                break;
        }
    };

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        // Araç kullanımı için Attributes
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());

        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            // Hazır olunca çal (ancak önce Focus iste)
            play();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            if (listener != null) {
                listener.onCompletion();
            }
            playNext();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            isPrepared = false;
            // Hata olursa bir sonraki şarkıya geçmeyi dene, yoksa durur.
            playNext();
            return true;
        });
    }

    public void setPlaylist(List<MusicRepository.AudioTrack> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty())
            return;
        this.playlist = new ArrayList<>(tracks);
        if (startIndex >= 0 && startIndex < playlist.size()) {
            playTrack(startIndex);
        }
    }

    private void playTrack(int index) {
        if (index < 0 || index >= playlist.size())
            return;

        // Önceki durdur
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        currentIndex = index;
        MusicRepository.AudioTrack track = playlist.get(index);

        try {
            mediaPlayer.reset();
            isPrepared = false;
            mediaPlayer.setDataSource(context, track.getContentUri());
            mediaPlayer.prepareAsync();

            if (listener != null) {
                listener.onTrackChanged(track);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            // Dosya bozuksa bir sonrakine geç
            playNext();
        }
    }

    public void play() {
        if (!isPrepared)
            return;

        // Çalmadan önce Audio Focus iste
        int result = audioManager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                if (listener != null)
                    listener.onPlaybackStateChanged(true);
            }
        }
    }

    public void pause() {
        if (isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null)
                listener.onPlaybackStateChanged(false);

            // Focus'u bırakmaya gerek yok (Abandon focus), belki kullanıcı hemen devam
            // ettirir.
            // Ancak kalıcı durdurma durumunda abandonAudioFocus yapılabilir.
        }
    }

    public void playPause() {
        if (isPrepared) {
            if (mediaPlayer.isPlaying()) {
                pause();
            } else {
                play();
            }
        } else if (currentIndex != -1) {
            playTrack(currentIndex);
        }
    }

    public void playNext() {
        if (playlist.isEmpty())
            return;
        int nextIndex = currentIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0; // Loop list
        }
        playTrack(nextIndex);
    }

    public void playPrevious() {
        if (playlist.isEmpty())
            return;

        // Eğer şarkı 3 saniyeden fazla çaldıysa başa sar
        if (isPrepared && mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
            return;
        }

        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1;
        }
        playTrack(prevIndex);
    }

    public boolean isPlaying() {
        return isPrepared && mediaPlayer.isPlaying();
    }

    public MusicRepository.AudioTrack getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public void release() {
        if (audioManager != null) {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // --- Seekbar Support ---

    public int getCurrentPosition() {
        if (isPrepared && mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (isPrepared && mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (isPrepared && mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public boolean isPrepared() {
        return isPrepared;
    }
}