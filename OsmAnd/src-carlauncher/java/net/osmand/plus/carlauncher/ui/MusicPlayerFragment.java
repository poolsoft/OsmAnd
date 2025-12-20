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
    private ImageButton btnPlaylist, btnClose, btnEqualizer;
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
        btnEqualizer = root.findViewById(net.osmand.plus.R.id.btn_equalizer);
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

        // Equalizer
        if (btnEqualizer != null) {
            btnEqualizer.setOnClickListener(v -> openEqualizer());
        }

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

        // Show playlists dialog
        showPlaylistDialog();
    }

    private void showPlaylistDialog() {
        if (getContext() == null)
            return;

        List<PlaylistManager.Playlist> playlists = playlistManager.getAllPlaylists();

        String[] options;
        if (playlists.isEmpty()) {
            options = new String[] { "+ Yeni Playlist Olustur" };
        } else {
            options = new String[playlists.size() + 1];
            options[0] = "+ Yeni Playlist Olustur";
            for (int i = 0; i < playlists.size(); i++) {
                PlaylistManager.Playlist p = playlists.get(i);
                options[i + 1] = p.name + " (" + p.tracks.size() + " sarki)";
            }
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Playlistler")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreatePlaylistDialog();
                    } else {
                        PlaylistManager.Playlist selected = playlists.get(which - 1);
                        showPlaylistOptionsDialog(selected);
                    }
                })
                .setNegativeButton("Kapat", null)
                .show();
    }

    private void showCreatePlaylistDialog() {
        if (getContext() == null)
            return;

        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Playlist adi");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF888888);
        input.setPadding(48, 32, 48, 32);

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Yeni Playlist")
                .setView(input)
                .setPositiveButton("Olustur", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        PlaylistManager.Playlist newPlaylist = new PlaylistManager.Playlist(name);
                        playlistManager.savePlaylist(newPlaylist);
                        showPlaylistDialog(); // Refresh
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void showPlaylistOptionsDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;

        String[] options = { "Cal", "Parcalari Gor/Duzenle", "Sil" };

        new android.app.AlertDialog.Builder(getContext())
                .setTitle(playlist.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Play
                            playPlaylist(playlist);
                            break;
                        case 1: // Edit
                            showPlaylistTracksDialog(playlist);
                            break;
                        case 2: // Delete
                            confirmDeletePlaylist(playlist);
                            break;
                    }
                })
                .setNegativeButton("Kapat", null)
                .show();
    }

    private void playPlaylist(PlaylistManager.Playlist playlist) {
        if (playlist.tracks.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "Playlist bos!", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Find tracks by path
        List<MusicRepository.AudioTrack> queue = new ArrayList<>();
        for (String path : playlist.tracks) {
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getPath().equals(path)) {
                    queue.add(t);
                    break;
                }
            }
        }

        if (!queue.isEmpty()) {
            if (isShuffleOn) {
                java.util.Collections.shuffle(queue);
            }
            musicManager.getInternalPlayer().setPlaylist(queue, 0);
        }
    }

    private void showPlaylistTracksDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;

        // Show current tracks and option to add more
        String[] options = new String[playlist.tracks.size() + 1];
        options[0] = "+ Parcha Ekle";
        for (int i = 0; i < playlist.tracks.size(); i++) {
            // Find track name
            String path = playlist.tracks.get(i);
            String name = path; // Default to path
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getPath().equals(path)) {
                    name = t.getTitle();
                    break;
                }
            }
            options[i + 1] = name;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle(playlist.name + " - Parcalar")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddTrackToPlaylistDialog(playlist);
                    } else {
                        // Long press to remove? For now, show option
                        int trackIndex = which - 1;
                        showRemoveTrackDialog(playlist, trackIndex);
                    }
                })
                .setNegativeButton("Kapat", null)
                .show();
    }

    private void showAddTrackToPlaylistDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null || allTracks.isEmpty())
            return;

        String[] trackNames = new String[allTracks.size()];
        for (int i = 0; i < allTracks.size(); i++) {
            trackNames[i] = allTracks.get(i).getTitle();
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Parcha Ekle")
                .setItems(trackNames, (dialog, which) -> {
                    String path = allTracks.get(which).getPath();
                    if (!playlist.tracks.contains(path)) {
                        playlist.tracks.add(path);
                        playlistManager.savePlaylist(playlist);
                        android.widget.Toast.makeText(getContext(), "Eklendi!", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        android.widget.Toast.makeText(getContext(), "Zaten eklenmis", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void showRemoveTrackDialog(PlaylistManager.Playlist playlist, int trackIndex) {
        if (getContext() == null)
            return;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Parcayi Kaldir?")
                .setMessage("Bu parcayi playlistten kaldirmak istiyor musunuz?")
                .setPositiveButton("Kaldir", (dialog, which) -> {
                    playlist.tracks.remove(trackIndex);
                    playlistManager.savePlaylist(playlist);
                    showPlaylistTracksDialog(playlist); // Refresh
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void confirmDeletePlaylist(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Playlisti Sil?")
                .setMessage(playlist.name + " playlistini silmek istiyor musunuz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    playlistManager.deletePlaylist(playlist.id);
                    android.widget.Toast.makeText(getContext(), "Silindi!", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void openEqualizer() {
        if (getContext() == null)
            return;

        try {
            android.content.Intent intent = new android.content.Intent(
                    android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            int sessionId = 0;
            if (musicManager != null && musicManager.getInternalPlayer() != null) {
                // Cannot get session ID from InternalMusicPlayer currently
                // Using 0 opens system default
            }
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE,
                    android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC);
            startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), "Equalizer bulunamadi", android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
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

    @Override
    public void onResume() {
        super.onResume();
        if (musicManager != null) {
            musicManager.addListener(this);
            updateModeUI();
            updatePlayPauseUI(); // Initial state
        }
        startSeekUpdater();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (musicManager != null) {
            musicManager.removeListener(this);
        }
        stopSeekUpdater();
    }

    // --- Seek Updater ---

    private void startSeekUpdater() {
        stopSeekUpdater(); // Stop existing if any
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null || musicManager == null)
                    return;

                // Only update if playing (or force update once?)
                // Update for Internal Player
                if (!isExternalMode && musicManager.getInternalPlayer() != null) {
                    long current = musicManager.getInternalPlayer().getCurrentPosition();
                    long total = musicManager.getInternalPlayer().getDuration();

                    if (total > 0) {
                        int progress = (int) (current * 100 / total);
                        if (seekbar != null && !seekbar.isPressed()) {
                            seekbar.setProgress(progress);
                        }
                        if (timeCurrent != null)
                            timeCurrent.setText(formatTime(current));
                        if (timeTotal != null)
                            timeTotal.setText(formatTime(total));
                    }
                }

                // Re-post
                seekHandler.postDelayed(this, 1000);
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekUpdater() {
        if (seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds);
    }

    private void closeFragment() {
        // ... (existing implementation)
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
