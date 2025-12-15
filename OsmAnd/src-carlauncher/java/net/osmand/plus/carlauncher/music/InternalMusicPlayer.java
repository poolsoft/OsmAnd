package net.osmand.plus.carlauncher.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles playback of local audio files using MediaPlayer.
 * Manages queue and playback state.
 */
public class InternalMusicPlayer {

    private static final String TAG = "InternalMusicPlayer";

    public interface PlaybackListener {
        void onTrackChanged(MusicRepository.AudioTrack track);

        void onPlaybackStateChanged(boolean isPlaying);

        void onCompletion();
    }

    private final Context context;
    private MediaPlayer mediaPlayer;
    private List<MusicRepository.AudioTrack> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isPrepared = false;
    private PlaybackListener listener;

    public InternalMusicPlayer(Context context) {
        this.context = context;
        initMediaPlayer();
    }

    public void setListener(PlaybackListener listener) {
        this.listener = listener;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.start();
            if (listener != null) {
                listener.onPlaybackStateChanged(true);
            }
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
            return true; // handled
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
        }
    }

    public void play() {
        if (isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (listener != null)
                listener.onPlaybackStateChanged(true);
        }
    }

    public void pause() {
        if (isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null)
                listener.onPlaybackStateChanged(false);
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
            // Resume or retry
            playTrack(currentIndex);
        }
    }

    public void playNext() {
        if (playlist.isEmpty())
            return;
        int nextIndex = currentIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0; // Loop
        }
        playTrack(nextIndex);
    }

    public void playPrevious() {
        if (playlist.isEmpty())
            return;
        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1;
        }
        // If playing more than 3 seconds, restart song
        if (mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
        } else {
            playTrack(prevIndex);
        }
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
