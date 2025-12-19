package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.music.MusicManager;
import net.osmand.plus.carlauncher.music.MusicRepository;
import net.osmand.plus.carlauncher.music.PlaylistManager;
import net.osmand.plus.carlauncher.dock.AppPickerDialog;
import net.osmand.plus.activities.MapActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern Muzik Player Fragment.
 * Iki mod destekler: Harici uygulama kontrolu ve Dahili calma.
 */
public class MusicPlayerFragment extends Fragment implements MusicManager.MusicUIListener {

    private MusicManager musicManager;
    private PlaylistManager playlistManager;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;

    // UI Elements
    private LinearLayout trackListPanel;
    private View playerPanel;
    private ImageView appIcon;
    private View appSelector;
    private ImageButton btnPlaylist, btnClose;
    private ImageView nowPlayingArt;
    private TextView nowPlayingTitle, nowPlayingArtist;
    private SeekBar seekbar;
    private TextView timeCurrent, timeTotal;
    private ImageButton btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat;
    private TextView tabPlaylists, tabRecent;
    private EditText searchInput;

    // State
    private boolean isExternalMode = true; // Default: external app control
    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0=off, 1=one, 2=all

    // Seekbar Handler
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;

    // Data
    private List<MusicRepository.AudioTrack> allTracks = new ArrayList<>();
    private List<MusicRepository.AudioTrack> filteredTracks = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            musicManager = MusicManager.getInstance(getContext());
            playlistManager = new PlaylistManager(getContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(net.osmand.plus.R.layout.fragment_music_player, container, false);

        // Find Views
        trackListPanel = root.findViewById(net.osmand.plus.R.id.track_list_panel);
        playerPanel = root.findViewById(net.osmand.plus.R.id.player_panel);
        appIcon = root.findViewById(net.osmand.plus.R.id.app_icon);
        appSelector = root.findViewById(net.osmand.plus.R.id.app_selector);
        btnPlaylist = root.findViewById(net.osmand.plus.R.id.btn_playlist);
        btnClose = root.findViewById(net.osmand.plus.R.id.btn_close);
        nowPlayingArt = root.findViewById(net.osmand.plus.R.id.now_playing_art);
        nowPlayingTitle = root.findViewById(net.osmand.plus.R.id.now_playing_title);
        nowPlayingArtist = root.findViewById(net.osmand.plus.R.id.now_playing_artist);
        seekbar = root.findViewById(net.osmand.plus.R.id.seekbar);
        timeCurrent = root.findViewById(net.osmand.plus.R.id.time_current);
        timeTotal = root.findViewById(net.osmand.plus.R.id.time_total);
        btnShuffle = root.findViewById(net.osmand.plus.R.id.btn_shuffle);
        btnPrev = root.findViewById(net.osmand.plus.R.id.btn_prev);
        btnPlay = root.findViewById(net.osmand.plus.R.id.btn_play);
        btnNext = root.findViewById(net.osmand.plus.R.id.btn_next);
        btnRepeat = root.findViewById(net.osmand.plus.R.id.btn_repeat);
        tabPlaylists = root.findViewById(net.osmand.plus.R.id.tab_playlists);
        tabRecent = root.findViewById(net.osmand.plus.R.id.tab_recent);
        searchInput = root.findViewById(net.osmand.plus.R.id.search_input);
        recyclerView = root.findViewById(net.osmand.plus.R.id.music_recycler);

        // Marquee
        if (nowPlayingTitle != null)
            nowPlayingTitle.setSelected(true);

        setupListeners();
        setupRecyclerView();
        updateModeUI();

        return root;
    }

    private void setupListeners() {
        // Close
        btnClose.setOnClickListener(v -> closeFragment());

        // Playback Controls
        btnPlay.setOnClickListener(v -> musicManager.playPause());
        btnNext.setOnClickListener(v -> musicManager.next());
        btnPrev.setOnClickListener(v -> musicManager.prev());

        // Shuffle
        btnShuffle.setOnClickListener(v -> {
            isShuffleOn = !isShuffleOn;
            updateShuffleUI();
        });

        // Repeat
        btnRepeat.setOnClickListener(v -> {
            repeatMode = (repeatMode + 1) % 3;
            updateRepeatUI();
        });

        // App Selector
        appSelector.setOnClickListener(v -> showAppPicker());

        // Playlist Toggle
        btnPlaylist.setOnClickListener(v -> {
            isExternalMode = !isExternalMode;
            updateModeUI();
        });

        // Seekbar
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isExternalMode) {
                    int duration = musicManager.getInternalPlayer().getDuration();
                    int newPos = (int) ((progress / 100f) * duration);
                    musicManager.getInternalPlayer().seekTo(newPos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Search
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTracks(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        // Tabs
        if (tabPlaylists != null) {
            tabPlaylists.setOnClickListener(v -> loadPlaylists());
        }
        if (tabRecent != null) {
            tabRecent.setOnClickListener(v -> loadRecentlyPlayed());
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    private void updateModeUI() {
        if (trackListPanel != null) {
            trackListPanel.setVisibility(isExternalMode ? View.GONE : View.VISIBLE);
        }

        // Load tracks if internal mode
        if (!isExternalMode && allTracks.isEmpty()) {
            loadAllTracks();
        }

        // Update app icon
        updateAppIcon();
    }

    private void loadAllTracks() {
        if (musicManager != null && musicManager.getRepository() != null) {
            List<MusicRepository.AudioFolder> folders = musicManager.getRepository().getCachedFolders();
            if (folders == null || folders.isEmpty()) {
                musicManager.getRepository().scanMusic((tracks, f) -> {
                    allTracks.clear();
                    for (MusicRepository.AudioFolder folder : f) {
                        allTracks.addAll(folder.getTracks());
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> showTracks(allTracks));
                    }
                });
            } else {
                allTracks.clear();
                for (MusicRepository.AudioFolder folder : folders) {
                    allTracks.addAll(folder.getTracks());
                }
                showTracks(allTracks);
            }
        }
    }

    private void showTracks(List<MusicRepository.AudioTrack> tracks) {
        filteredTracks = new ArrayList<>(tracks);
        adapter = new MusicAdapter(filteredTracks, track -> {
            // Play track
            int index = allTracks.indexOf(track);
            List<MusicRepository.AudioTrack> queue = isShuffleOn ? shuffleWithFirst(allTracks, track) : allTracks;
            musicManager.getInternalPlayer().setPlaylist(queue, isShuffleOn ? 0 : index);

            // Add torecentlyyPlayed
            playlistManager.addToRecentlyPlayed(track.getPath());
        });
        if (recyclerView != null) {
            recyclerView.setAdapter(adapter);
        }
    }

    private List<MusicRepository.AudioTrack> shuffleWithFirst(List<MusicRepository.AudioTrack> tracks,
            MusicRepository.AudioTrack first) {
        List<MusicRepository.AudioTrack> result = new ArrayList<>(tracks);
        result.remove(first);
        java.util.Collections.shuffle(result);
        result.add(0, first);
        return result;
    }

    private void filterTracks(String query) {
        if (query == null || query.isEmpty()) {
            showTracks(allTracks);
        } else {
            String lower = query.toLowerCase(Locale.getDefault());
            List<MusicRepository.AudioTrack> filtered = new ArrayList<>();
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getTitle().toLowerCase(Locale.getDefault()).contains(lower) ||
                        t.getArtist().toLowerCase(Locale.getDefault()).contains(lower)) {
                    filtered.add(t);
                }
            }
            showTracks(filtered);
        }
    }

    private void loadPlaylists() {
        // Update tab colors
        if (tabPlaylists != null)
            tabPlaylists.setTextColor(0xFF00FFFF);
        if (tabRecent != null)
            tabRecent.setTextColor(0xFF888888);
        // TODO: Show playlists
    }

    private void loadRecentlyPlayed() {
        // Update tab colors
        if (tabPlaylists != null)
            tabPlaylists.setTextColor(0xFF888888);
        if (tabRecent != null)
            tabRecent.setTextColor(0xFF00FFFF);

        List<String> recentPaths = playlistManager.getRecentlyPlayed();
        List<MusicRepository.AudioTrack> recentTracks = new ArrayList<>();
        for (String path : recentPaths) {
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getPath().equals(path)) {
                    recentTracks.add(t);
                    break;
                }
            }
        }
        showTracks(recentTracks);
    }

    private void showAppPicker() {
        if (getContext() == null)
            return;
        new AppPickerDialog(getContext(), true, (packageName, appName, icon) -> {
            musicManager.setPreferredPackage(packageName);
            updateAppIcon();
        }).show();
    }

    private void updateAppIcon() {
        if (appIcon == null || getContext() == null)
            return;
        String pkg = musicManager.getPreferredPackage();
        if (pkg != null) {
            try {
                Drawable icon = getContext().getPackageManager().getApplicationIcon(pkg);
                appIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                appIcon.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            appIcon.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void updateShuffleUI() {
        if (btnShuffle != null) {
            btnShuffle.setColorFilter(isShuffleOn ? 0xFF00FFFF : 0xFF666666);
        }
    }

    private void updateRepeatUI() {
        if (btnRepeat == null)
            return;
        switch (repeatMode) {
            case 0: // Off
                btnRepeat.setColorFilter(0xFF666666);
                break;
            case 1: // Repeat One
                btnRepeat.setColorFilter(0xFF00FFFF);
                // Could change icon here
                break;
            case 2: // Repeat All
                btnRepeat.setColorFilter(0xFF00FF00);
                break;
        }
    }

    private void closeFragment() {
        if (getActivity() instanceof MapActivity) {
            ((MapActivity) getActivity()).closeAppDrawer();
        } else if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    // --- Lifecycle ---

    @Override
    public void onStart() {
        super.onStart();
        if (musicManager != null)
            musicManager.addListener(this);
        startSeekbarUpdater();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (musicManager != null)
            musicManager.removeListener(this);
        stopSeekbarUpdater();
    }

    private void startSeekbarUpdater() {
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                updateSeekbar();
                seekHandler.postDelayed(this, 1000);
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekbarUpdater() {
        if (seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
        }
    }

    private void updateSeekbar() {
        if (isExternalMode || musicManager == null)
            return;

        int current = musicManager.getInternalPlayer().getCurrentPosition();
        int duration = musicManager.getInternalPlayer().getDuration();

        if (duration > 0 && seekbar != null) {
            int progress = (int) ((current * 100f) / duration);
            seekbar.setProgress(progress);
        }

        if (timeCurrent != null)
            timeCurrent.setText(formatTime(current));
        if (timeTotal != null)
            timeTotal.setText(formatTime(duration));
    }

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / 1000) / 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    // --- MusicUIListener ---

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (nowPlayingTitle != null)
            nowPlayingTitle.setText(title != null ? title : "Muzik Secin");
        if (nowPlayingArtist != null)
            nowPlayingArtist.setText(artist != null ? artist : "---");
        if (nowPlayingArt != null) {
            if (albumArt != null) {
                nowPlayingArt.setImageBitmap(albumArt);
            } else {
                nowPlayingArt.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (btnPlay != null) {
            btnPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        isExternalMode = !isInternal;
        updateModeUI();
    }

    // --- Adapter ---

    private static class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.Holder> {

        private final List<MusicRepository.AudioTrack> tracks;
        private final OnTrackClickListener listener;

        public interface OnTrackClickListener {
            void onClick(MusicRepository.AudioTrack track);
        }

        public MusicAdapter(List<MusicRepository.AudioTrack> tracks, OnTrackClickListener listener) {
            this.tracks = tracks;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(net.osmand.plus.R.layout.item_music_track, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            MusicRepository.AudioTrack track = tracks.get(position);
            holder.title.setText(track.getTitle());
            holder.artist.setText(track.getArtist());
            holder.itemView.setOnClickListener(v -> listener.onClick(track));
        }

        @Override
        public int getItemCount() {
            return tracks.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView title, artist;
            ImageView icon;

            public Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(net.osmand.plus.R.id.music_title);
                artist = itemView.findViewById(net.osmand.plus.R.id.music_artist);
                icon = itemView.findViewById(net.osmand.plus.R.id.music_icon);
            }
        }
    }
}
