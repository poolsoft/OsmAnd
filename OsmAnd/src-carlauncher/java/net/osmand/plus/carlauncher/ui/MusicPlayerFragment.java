package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Modern Muzik Player Fragment.
 * Iki mod destekler: Harici uygulama kontrolu ve Dahili calma.
 */
public class MusicPlayerFragment extends Fragment implements MusicManager.MusicUIListener, MusicManager.MusicVisualizerListener {

    // ...

    @Override
    public void onResume() {
        super.onResume();
        if (musicManager != null) {
            musicManager.addListener(this);
            musicManager.addVisualizerListener(this); // Centralized Visualizer
            updateModeUI(); 
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (musicManager != null) {
            musicManager.removeListener(this);
            musicManager.removeVisualizerListener(this);
        }
    }

    private MusicManager musicManager;
    private PlaylistManager playlistManager;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;

    // UI Elements
    private LinearLayout trackListPanel;
    private View playerPanel;
    private ImageView appIcon;
    private View appSelectorLaunch;
    private View appSelectorArrow;
    private ImageButton btnPlaylist, btnClose, btnEqualizer;
    private ImageView nowPlayingArt;
    private TextView nowPlayingTitle, nowPlayingArtist;
    private SeekBar seekbar;
    private TextView timeCurrent, timeTotal;
    private ImageButton btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat;
    private Spinner playlistSpinner;
    private EditText searchInput;

    // New Tab Views
    private TextView tabAllTracks, tabRecent, tabPlaylistLabel, appName;
    private View tabPlaylistsContainer;

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
        appSelectorLaunch = root.findViewById(net.osmand.plus.R.id.app_selector_launch);
        appSelectorArrow = root.findViewById(net.osmand.plus.R.id.app_selector_arrow);
        // btnPlaylist = root.findViewById(net.osmand.plus.R.id.btn_playlist);
        // btnEqualizer = root.findViewById(net.osmand.plus.R.id.btn_equalizer);
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
        playlistSpinner = root.findViewById(net.osmand.plus.R.id.playlist_spinner);
        searchInput = root.findViewById(net.osmand.plus.R.id.search_input);
        recyclerView = root.findViewById(net.osmand.plus.R.id.music_recycler);

        tabAllTracks = root.findViewById(net.osmand.plus.R.id.tab_all_tracks);
        tabRecent = root.findViewById(net.osmand.plus.R.id.tab_recent);
        tabPlaylistsContainer = root.findViewById(net.osmand.plus.R.id.tab_playlists_container);
        tabPlaylistLabel = root.findViewById(net.osmand.plus.R.id.tab_playlist_label);
        appName = root.findViewById(net.osmand.plus.R.id.app_name);

        // Marquee
        if (nowPlayingTitle != null)
            nowPlayingTitle.setSelected(true);

        setupListeners();
        setupRecyclerView();
        updateModeUI();

        // Handle Orientation
        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (root instanceof LinearLayout) {
            ((LinearLayout) root).setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

            if (playerPanel != null && trackListPanel != null) {
                LinearLayout.LayoutParams p1 = (LinearLayout.LayoutParams) playerPanel.getLayoutParams();
                LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) trackListPanel.getLayoutParams();

                if (isPortrait) {
                    p1.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    p1.height = 0;
                    p2.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    p2.height = 0;
                } else {
                    p1.width = 0;
                    p1.height = LinearLayout.LayoutParams.MATCH_PARENT;
                    p2.width = 0;
                    p2.height = LinearLayout.LayoutParams.MATCH_PARENT;
                }
                playerPanel.setLayoutParams(p1);
                trackListPanel.setLayoutParams(p2);
            }
        }

        // Set Icons
        if (btnPlay != null) btnPlay.setImageResource(net.osmand.plus.R.drawable.ic_music_play);
        if (btnNext != null) btnNext.setImageResource(net.osmand.plus.R.drawable.ic_music_next);
        if (btnPrev != null) btnPrev.setImageResource(net.osmand.plus.R.drawable.ic_music_prev);
        if (btnShuffle != null) btnShuffle.setImageResource(net.osmand.plus.R.drawable.ic_music_shuffle);
        if (btnRepeat != null) btnRepeat.setImageResource(net.osmand.plus.R.drawable.ic_music_repeat);
        if (btnClose != null) btnClose.setImageResource(net.osmand.plus.R.drawable.ic_music_close);
        
        // Find Visualizer
        visualizerView = root.findViewById(net.osmand.plus.R.id.player_visualizer);

        return root;
    }

    // --- Visualizer (Centralized) ---
    private net.osmand.plus.carlauncher.widgets.MusicVisualizerView visualizerView;

    @Override
    public void onFftDataCapture(byte[] fft) {
        if (visualizerView != null) {
            visualizerView.updateVisualizer(fft);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (musicManager != null) {
            musicManager.addListener(this);
            // Update UI state
            updateModeUI(); 
            // Check play state for visualizer
            if (musicManager.getInternalPlayer().isPlaying()) {
                // Check perms again
                if (getContext() != null && androidx.core.content.ContextCompat.checkSelfPermission(getContext(), 
                     android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                     startVisualizer();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (musicManager != null) {
            musicManager.removeListener(this);
        }
        stopVisualizer();
    }

    private void setupListeners() {
        // Close
        if (btnClose != null)
            btnClose.setOnClickListener(v -> closeFragment());

        // Playback Controls (MusicManager metod isimleri düzeltildi)
        // Playback Controls
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (isExternalMode) {
                    musicManager.togglePlayPause();
                } else {
                    if (musicManager.getInternalPlayer() != null) {
                        musicManager.getInternalPlayer().playPause();
                    }
                }
            });
        }
        if (btnNext != null)
            btnNext.setOnClickListener(v -> musicManager.skipToNext());
        if (btnPrev != null)
            btnPrev.setOnClickListener(v -> musicManager.skipToPrevious());

        // Shuffle
        if (btnShuffle != null)
            btnShuffle.setOnClickListener(v -> {
                isShuffleOn = !isShuffleOn;
                updateShuffleUI();
            });

        // Repeat
        if (btnRepeat != null)
            btnRepeat.setOnClickListener(v -> {
                repeatMode = (repeatMode + 1) % 3;
                updateRepeatUI();
            });

        // App Selector Launch (Icon + Name)
        if (appSelectorLaunch != null) {
            appSelectorLaunch.setOnClickListener(v -> {
                if (isExternalMode) {
                    // Launch External App
                    String pkg = musicManager.getPreferredPackage();
                    if (pkg != null && !pkg.isEmpty()) {
                         try {
                            Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(pkg);
                            if (launchIntent != null) {
                                // Launch in Overlay/Split based on logic, or just standard launch
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(launchIntent);
                            }
                        } catch (Exception e) {
                            android.widget.Toast.makeText(getContext(), "Uygulama açılamadı", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    android.widget.Toast.makeText(getContext(), "Dahili Oynatıcı", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        // App Selector Picker (Arrow)
        if (appSelectorArrow != null) {
            appSelectorArrow.setOnClickListener(v -> showAppPicker());
        }

        // Playlist Toggle (Switch Modes)
        if (btnPlaylist != null)
            btnPlaylist.setOnClickListener(v -> {
                isExternalMode = !isExternalMode;
                updateModeUI();
            });

        // Equalizer
        if (btnEqualizer != null) {
            btnEqualizer.setOnClickListener(v -> openEqualizer());
        }

        // Seekbar
        if (seekbar != null) {
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !isExternalMode && musicManager.getInternalPlayer() != null) {
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
        }

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

        // Playlist Spinner
        setupPlaylistSpinner();

        // Tab Listeners
        if (tabAllTracks != null)
            tabAllTracks.setOnClickListener(v -> {
                selectTab(0);
                checkPermissionsAndLoadTracks();
            });

        if (tabRecent != null)
            tabRecent.setOnClickListener(v -> {
                selectTab(1);
                loadRecentlyPlayed();
            });

        if (tabPlaylistsContainer != null)
            tabPlaylistsContainer.setOnClickListener(v -> {
                selectTab(2);
                if (playlistSpinner != null)
                    playlistSpinner.performClick();
            });
    }

    private void selectTab(int index) {
        // 0: All, 1: Recent, 2: Playlist
        int selectedColor = 0xFFFFFFFF;
        int unselectedColor = 0xFF888888;

        if (tabAllTracks != null)
            tabAllTracks.setTextColor(index == 0 ? selectedColor : unselectedColor);
        if (tabRecent != null)
            tabRecent.setTextColor(index == 1 ? selectedColor : unselectedColor);
        if (tabPlaylistLabel != null)
            tabPlaylistLabel.setTextColor(index == 2 ? selectedColor : unselectedColor);

        // Ensure Spinner is visible/hidden if needed, but it is inside Tab 3 container
        // so it's always there.
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    private void setupPlaylistSpinner() {
        if (playlistSpinner == null || getContext() == null)
            return;

        List<String> options = new ArrayList<>();
        options.add("Seciniz..."); // 0
        options.add("Favoriler"); // 1

        List<PlaylistManager.Playlist> playlists = playlistManager.getAllPlaylists();
        for (PlaylistManager.Playlist p : playlists) {
            options.add(p.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playlistSpinner.setAdapter(spinnerAdapter);

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;

                selectTab(2);

                if (position == 1) {
                    // Favorites
                    List<String> favPaths = playlistManager.getFavorites();
                    List<MusicRepository.AudioTrack> favTracks = new ArrayList<>();
                    for (String path : favPaths) {
                        for (MusicRepository.AudioTrack t : allTracks) {
                            if (t.getPath().equals(path)) {
                                favTracks.add(t);
                                break;
                            }
                        }
                    }
                    if (favTracks.isEmpty()) {
                        Toast.makeText(getContext(), "Favori listeniz bos", Toast.LENGTH_SHORT).show();
                    }
                    showTracks(favTracks);
                } else {
                    // Playlists
                    int playlistIndex = position - 2;
                    if (playlistIndex >= 0 && playlistIndex < playlists.size()) {
                        List<MusicRepository.AudioTrack> playlistTracks = getPlaylistTracks(
                                playlists.get(playlistIndex));
                        showTracks(playlistTracks);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateModeUI() {
        // Track list visible in both modes (handled by layout)

        // Dahili moddaysa veya liste bossa (izin varsa yükle, yoksa iste)
        if (allTracks.isEmpty()) {
            checkPermissionsAndLoadTracks();
        }

        // Uygulama ikonunu güncelle
        updateAppIcon();

        // Seekbar görünürlüğü (Opsiyonel: External modda seekbar çalışmayabilir)
        if (seekbar != null) {
            seekbar.setEnabled(!isExternalMode);
            if (isExternalMode)
                seekbar.setProgress(0);
        }
    }

    private void checkPermissionsAndLoadTracks() {
        List<String> perms = new ArrayList<>();
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (getContext().checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                 perms.add(android.Manifest.permission.READ_MEDIA_AUDIO);
             }
        } else {
             if (getContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                 perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
             }
        }
        
        if (getContext().checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            perms.add(android.Manifest.permission.RECORD_AUDIO);
        }

        if (!perms.isEmpty()) {
            requestPermissions(perms.toArray(new String[0]), 100);
        } else {
            loadAllTracks();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAllTracks();
            } else {
                Toast.makeText(getContext(), "Muzik taramak icin izin gerekli!", Toast.LENGTH_SHORT).show();
            }
        }
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
                        getActivity().runOnUiThread(() -> {
                            if (allTracks.isEmpty()) {
                                Toast.makeText(getContext(), "Cihazda muzik bulunamadi", Toast.LENGTH_SHORT).show();
                            }
                            showTracks(allTracks);
                        });
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
        adapter = new MusicAdapter(filteredTracks, new MusicAdapter.OnTrackClickListener() {
            @Override
            public void onClick(MusicRepository.AudioTrack track) {
                // Switch to internal mode
                if (isExternalMode) {
                    isExternalMode = false;
                    // Harici oynatıcıyı durdur
                    if (musicManager.getActiveExternalController() != null) {
                        try {
                            musicManager.getActiveExternalController().getTransportControls().pause();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    // Force MusicManager to drop external preference so it picks up internal player
                    musicManager.setPreferredPackage(null);
                    
                    updateModeUI();
                }

                // Internal Player Logic
                if (musicManager.getInternalPlayer() == null) {
                    return;
                }

                MusicRepository.AudioTrack current = musicManager.getInternalPlayer().getCurrentTrack();
                if (current != null && current.getPath().equals(track.getPath())) {
                    // Toggle Logic
                    if (musicManager.getInternalPlayer().isPlaying()) {
                        musicManager.getInternalPlayer().pause();
                    } else {
                        musicManager.getInternalPlayer().play();
                    }
                    return;
                }

                // Use current list (filteredTracks) as queue!
                int index = filteredTracks.indexOf(track);
                List<MusicRepository.AudioTrack> queue = isShuffleOn ? shuffleWithFirst(filteredTracks, track)
                        : filteredTracks;
                musicManager.getInternalPlayer().setPlaylist(queue, isShuffleOn ? 0 : index);

                // Add to recently played
                playlistManager.addToRecentlyPlayed(track.getPath());
            }

            @Override
            public void onAddClick(MusicRepository.AudioTrack track) {
                showAddTrackToPlaylistDialog(track);
            }
        }, playlistManager);
        
        // Sync Adapter State immediately
        if (!isExternalMode && musicManager != null && musicManager.getInternalPlayer() != null) {
            MusicRepository.AudioTrack current = musicManager.getInternalPlayer().getCurrentTrack();
            String path = current != null ? current.getPath() : null;
            boolean isPlaying = musicManager.getInternalPlayer().isPlaying();
            adapter.updateCurrentTrack(path, isPlaying);
        }

        if (recyclerView != null) {
            recyclerView.setAdapter(adapter);
        }
    }

    private List<MusicRepository.AudioTrack> shuffleWithFirst(List<MusicRepository.AudioTrack> tracks,
            MusicRepository.AudioTrack first) {
        List<MusicRepository.AudioTrack> result = new ArrayList<>(tracks);
        result.remove(first);
        Collections.shuffle(result);
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

    // --- Playlist & Dialog Logic (Kısaltıldı, orijinal mantık korundu) ---

    private void showAddTrackToPlaylistDialog(MusicRepository.AudioTrack track) {
        if (getContext() == null)
            return;
        List<PlaylistManager.Playlist> playlists = playlistManager.getAllPlaylists();
        String[] options = new String[playlists.size() + 1];
        options[0] = "+ Yeni Playlist Olustur";
        for (int i = 0; i < playlists.size(); i++) {
            options[i + 1] = playlists.get(i).name;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Playliste Ekle: " + track.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreatePlaylistAndAddTrackDialog(track);
                    } else {
                        PlaylistManager.Playlist p = playlists.get(which - 1);
                        p.tracks.add(track.getPath());
                        playlistManager.savePlaylist(p);
                        Toast.makeText(getContext(), "Eklendi: " + p.name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void showCreatePlaylistAndAddTrackDialog(MusicRepository.AudioTrack track) {
        if (getContext() == null)
            return;
        EditText input = new EditText(getContext());
        input.setHint("Playlist adi");
        input.setTextColor(0xFFFFFFFF);
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Yeni Playlist ve Ekle")
                .setView(input)
                .setPositiveButton("Olustur", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        PlaylistManager.Playlist p = new PlaylistManager.Playlist(name);
                        p.tracks.add(track.getPath());
                        playlistManager.savePlaylist(p);
                        Toast.makeText(getContext(), "Playlist olusturuldu ve sarki eklendi", Toast.LENGTH_SHORT)
                                .show();
                        setupPlaylistSpinner();
                    }
                })
                .setNegativeButton("Iptal", null).show();
    }

    private void showPlaylistOptionsDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;
        String[] options = { "Cal", "Parcalari Gor/Duzenle", "Sil" };
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(playlist.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playPlaylist(playlist);
                            break;
                        case 1:
                            showPlaylistTracksDialog(playlist);
                            break;
                        case 2:
                            confirmDeletePlaylist(playlist);
                            break;
                    }
                })
                .setNegativeButton("Kapat", null).show();
    }

    private void playPlaylist(PlaylistManager.Playlist playlist) {
        if (playlist.tracks.isEmpty()) {
            Toast.makeText(getContext(), "Playlist bos!", Toast.LENGTH_SHORT).show();
            return;
        }
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
            if (isShuffleOn)
                Collections.shuffle(queue);
            musicManager.getInternalPlayer().setPlaylist(queue, 0);
        }
    }

    private void showPlaylistTracksDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;
        String[] options = new String[playlist.tracks.size() + 1];
        options[0] = "+ Parcha Ekle";
        for (int i = 0; i < playlist.tracks.size(); i++) {
            String path = playlist.tracks.get(i);
            String name = path;
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getPath().equals(path)) {
                    name = t.getTitle();
                    break;
                }
            }
            options[i + 1] = name;
        }
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(playlist.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0)
                        showAddTrackToPlaylistDialog(playlist);
                    else
                        showRemoveTrackDialog(playlist, which - 1);
                })
                .setNegativeButton("Kapat", null).show();
    }

    private void showAddTrackToPlaylistDialog(PlaylistManager.Playlist playlist) {
        if (getContext() == null || allTracks.isEmpty())
            return;
        String[] trackNames = new String[allTracks.size()];
        for (int i = 0; i < allTracks.size(); i++)
            trackNames[i] = allTracks.get(i).getTitle();

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Parcha Ekle")
                .setItems(trackNames, (dialog, which) -> {
                    String path = allTracks.get(which).getPath();
                    if (!playlist.tracks.contains(path)) {
                        playlist.tracks.add(path);
                        playlistManager.savePlaylist(playlist);
                        Toast.makeText(getContext(), "Eklendi!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Iptal", null).show();
    }

    private void showRemoveTrackDialog(PlaylistManager.Playlist playlist, int index) {
        if (getContext() == null)
            return;
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Kaldir?")
                .setPositiveButton("Kaldir", (dialog, which) -> {
                    playlist.tracks.remove(index);
                    playlistManager.savePlaylist(playlist);
                    showPlaylistTracksDialog(playlist);
                })
                .setNegativeButton("Iptal", null).show();
    }

    private void confirmDeletePlaylist(PlaylistManager.Playlist playlist) {
        if (getContext() == null)
            return;
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Sil?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    playlistManager.deletePlaylist(playlist.id);
                    Toast.makeText(getContext(), "Silindi!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Iptal", null).show();
    }

    private void openEqualizer() {
        if (getContext() == null)
            return;
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE,
                    android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, 0);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Equalizer bulunamadi", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecentlyPlayed() {

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
            if ("usage.internal.player".equals(packageName)) {
                isExternalMode = false;
                musicManager.setPreferredPackage(null);
                updateModeUI();
                if (musicManager.getInternalPlayer() != null) {
                    musicManager.getInternalPlayer().resumeLastSession();
                }
            } else {
                musicManager.setPreferredPackage(packageName);
                updateAppIcon();
                // Launch App
                try {
                    android.content.Intent launchIntent = getContext().getPackageManager()
                            .getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        startActivity(launchIntent);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }).show();
    }

    private List<MusicRepository.AudioTrack> getPlaylistTracks(PlaylistManager.Playlist playlist) {
        List<MusicRepository.AudioTrack> result = new ArrayList<>();
        for (String path : playlist.tracks) {
            for (MusicRepository.AudioTrack t : allTracks) {
                if (t.getPath().equals(path)) {
                    result.add(t);
                    break;
                }
            }
        }
        return result;
    }

    private void updateAppIcon() {
        if (appIcon == null || getContext() == null)
            return;
        String pkg = musicManager.getPreferredPackage();
        try {
            PackageManager pm = getContext().getPackageManager();
            Drawable icon = (pkg != null) ? pm.getApplicationIcon(pkg) : null;
            CharSequence label = (pkg != null) ? pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)) : "Dahili Muzik";

            if (icon != null)
                appIcon.setImageDrawable(icon);
            else
                appIcon.setImageResource(net.osmand.plus.R.drawable.ic_music_play);

            if (appName != null) {
                appName.setText(label);
            }
        } catch (Exception e) {
            appIcon.setImageResource(net.osmand.plus.R.drawable.ic_music_play);
            if (appName != null)
                appName.setText("Muzik");
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
            case 0:
                btnRepeat.setColorFilter(0xFF666666);
                btnRepeat.setImageResource(net.osmand.plus.R.drawable.ic_music_repeat);
                break;
            case 1:
                btnRepeat.setColorFilter(0xFF00FFFF);
                btnRepeat.setImageResource(net.osmand.plus.R.drawable.ic_music_repeat_one);
                break; // Repeat One
            case 2:
                btnRepeat.setColorFilter(0xFF00FF00);
                btnRepeat.setImageResource(net.osmand.plus.R.drawable.ic_music_repeat);
                break; // Repeat All
        }
    }

    private void closeFragment() {
        if (getActivity() instanceof MapActivity) {
            ((MapActivity) getActivity()).closeAppDrawer();
        } else if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    // --- Lifecycle & Seek Updater (BİRLEŞTİRİLMİŞ) ---

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
        stopSeekbarUpdater(); // Varsa eskisini durdur
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                updateSeekbar();
                seekHandler.postDelayed(this, 1000); // 1 saniyede bir güncelle
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekbarUpdater() {
        if (seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
        }
    }

    private void updateSeekbar() {
        // Eğer External moddaysak veya player yoksa güncelleme yapma
        if (isExternalMode || musicManager == null || musicManager.getInternalPlayer() == null)
            return;

        // Internal Player'dan süreyi al
        long current = musicManager.getInternalPlayer().getCurrentPosition();
        long duration = musicManager.getInternalPlayer().getDuration();

        if (duration > 0 && seekbar != null && !seekbar.isPressed()) {
            int progress = (int) ((current * 100) / duration);
            seekbar.setProgress(progress);
        }

        if (timeCurrent != null)
            timeCurrent.setText(formatTime(current));
        if (timeTotal != null)
            timeTotal.setText(formatTime(duration));
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds);
    }

    // --- MusicUIListener ---

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(() -> {
            if (nowPlayingTitle != null)
                nowPlayingTitle.setText(title != null ? title : "Muzik Secin");
            if (nowPlayingArtist != null)
                nowPlayingArtist.setText(artist != null ? artist : "");

            if (nowPlayingArt != null) {
                if (albumArt != null)
                    nowPlayingArt.setImageBitmap(albumArt);
                else
                    nowPlayingArt.setImageResource(net.osmand.plus.R.drawable.ic_music_play);
            }

            // Update Adapter Highlight
            if (!isExternalMode && musicManager.getInternalPlayer() != null) {
                MusicRepository.AudioTrack current = musicManager.getInternalPlayer().getCurrentTrack();
                String path = current != null ? current.getPath() : null;
                boolean isPlaying = musicManager.getInternalPlayer().isPlaying();
                if (adapter != null) {
                    adapter.updateCurrentTrack(path, isPlaying);
                }
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(() -> {
            if (btnPlay != null) {
                btnPlay.setImageResource(
                        isPlaying ? net.osmand.plus.R.drawable.ic_music_pause : net.osmand.plus.R.drawable.ic_music_play);
            }

            // Update Adapter Icon
            if (!isExternalMode && musicManager.getInternalPlayer() != null) {
                MusicRepository.AudioTrack current = musicManager.getInternalPlayer().getCurrentTrack();
                String path = current != null ? current.getPath() : null;
                if (adapter != null) {
                    adapter.updateCurrentTrack(path, isPlaying);
                }
            }
            
            // Visualizer Control
            if (isPlaying) startVisualizer();
            else stopVisualizer();
        });
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        if (getActivity() == null)
            return;
        boolean newMode = !isInternal;
        if (isExternalMode != newMode) {
            isExternalMode = newMode;
            getActivity().runOnUiThread(this::updateModeUI);
        }
    }

    // --- Adapter ---

    private static class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.Holder> {
        private final List<MusicRepository.AudioTrack> tracks;
        private final OnTrackClickListener listener;
        private final net.osmand.plus.carlauncher.music.PlaylistManager playlistManager;
        private String currentTrackPath;
        private boolean isPlaying;

        public interface OnTrackClickListener {
            void onClick(MusicRepository.AudioTrack track);

            void onAddClick(MusicRepository.AudioTrack track);
        }

        public MusicAdapter(List<MusicRepository.AudioTrack> tracks, OnTrackClickListener listener,
                net.osmand.plus.carlauncher.music.PlaylistManager playlistManager) {
            this.tracks = tracks;
            this.listener = listener;
            this.playlistManager = playlistManager;
        }

        public void updateCurrentTrack(String path, boolean playing) {
            this.currentTrackPath = path;
            this.isPlaying = playing;
            notifyDataSetChanged();
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

            // Icon Logic
            boolean isCurrent = track.getPath().equals(currentTrackPath);
            if (isCurrent) {
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageResource(
                        isPlaying ? net.osmand.plus.R.drawable.ic_music_pause : net.osmand.plus.R.drawable.ic_music_play);
                holder.itemView.setBackgroundColor(0x3300FFFF); // Highlight
            } else {
                holder.icon.setVisibility(View.INVISIBLE);
                holder.icon.setImageResource(0);
                holder.itemView.setBackgroundResource(android.R.drawable.list_selector_background);
            }

            // Favorites Logic
            if (holder.btnFavorite != null && playlistManager != null) {
                boolean isFav = playlistManager.isFavorite(track.getPath());
                holder.btnFavorite.setImageResource(isFav ? android.R.drawable.star_on : android.R.drawable.star_off);

                // Tint: Gold if Fav, Gray/White if not
                if (isFav) {
                    holder.btnFavorite.setColorFilter(0xFFFFD700); // Gold
                } else {
                    holder.btnFavorite.setColorFilter(0xFF888888); // Gray
                }

                holder.btnFavorite.setOnClickListener(v -> {
                    if (isFav) {
                        playlistManager.removeFromFavorites(track.getPath());
                    } else {
                        playlistManager.addToFavorites(track.getPath());
                    }
                    notifyItemChanged(position);
                });
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(track));
            if (holder.btnAdd != null) {
                holder.btnAdd.setOnClickListener(v -> listener.onAddClick(track));
            }
        }

        @Override
        public int getItemCount() {
            return tracks != null ? tracks.size() : 0;
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView title, artist;
            ImageButton btnAdd;
            ImageButton btnFavorite;
            ImageView icon;

            public Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(net.osmand.plus.R.id.music_title);
                artist = itemView.findViewById(net.osmand.plus.R.id.music_artist);
                btnAdd = itemView.findViewById(net.osmand.plus.R.id.btn_add_to_playlist);
                btnFavorite = itemView.findViewById(net.osmand.plus.R.id.btn_favorite);
                icon = itemView.findViewById(net.osmand.plus.R.id.music_icon);
            }
        }
    }
}