package net.osmand.plus.carlauncher.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.music.MusicManager;
import net.osmand.plus.carlauncher.music.MusicRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Muzik Secici (Drawer) Ekrani.
 * Klasorleri ve dosyallari listeler.
 */
public class MusicPlayerFragment extends Fragment implements MusicManager.MusicUIListener {

    private MusicManager musicManager;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;

    // Header UI
    private TextView nowPlayingTitle;
    private TextView nowPlayingArtist;
    private ImageView nowPlayingArt;
    private ImageButton btnPlay, btnNext, btnPrev, btnClose;
    private TextView tabFolders, tabFavorites;

    // Data
    private List<MusicRepository.AudioFolder> folders;
    private List<MusicRepository.AudioTrack> currentTracks;
    private boolean isShowingFolders = true; // true: Folders, false: Tracks in Folder
    private MusicRepository.AudioFolder selectedFolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            musicManager = MusicManager.getInstance(getContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(net.osmand.plus.R.layout.fragment_music_player, container, false);

        nowPlayingTitle = root.findViewById(net.osmand.plus.R.id.now_playing_title);
        if (nowPlayingTitle != null)
            nowPlayingTitle.setSelected(true); // Marquee effect
        nowPlayingArtist = root.findViewById(net.osmand.plus.R.id.now_playing_artist);
        nowPlayingArt = root.findViewById(net.osmand.plus.R.id.now_playing_art);

        btnPlay = root.findViewById(net.osmand.plus.R.id.btn_play);
        btnNext = root.findViewById(net.osmand.plus.R.id.btn_next);
        btnPrev = root.findViewById(net.osmand.plus.R.id.btn_prev);
        btnClose = root.findViewById(net.osmand.plus.R.id.btn_close);

        tabFolders = root.findViewById(net.osmand.plus.R.id.tab_folders);
        tabFavorites = root.findViewById(net.osmand.plus.R.id.tab_favorites);

        recyclerView = root.findViewById(net.osmand.plus.R.id.music_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupListeners();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load Data
        folders = musicManager.getRepository().getCachedFolders();
        if (folders == null || folders.isEmpty()) {
            // Re-scan if empty
            musicManager.getRepository().scanMusic((tracks, f) -> {
                folders = f;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::showFolders);
                }
            });
        } else {
            showFolders();
        }
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> musicManager.playPause());
        btnNext.setOnClickListener(v -> musicManager.next());
        btnPrev.setOnClickListener(v -> musicManager.prev());

        btnClose.setOnClickListener(v -> {
            if (getActivity() instanceof MapActivity) {
                ((MapActivity) getActivity()).closeAppDrawer();
            } else if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction().remove(this).commit();
            }
        });

        tabFolders.setOnClickListener(v -> {
            isShowingFolders = true;
            selectedFolder = null;
            showFolders();
            updateTabs();
        });

        tabFavorites.setOnClickListener(v -> {
            // TODO: Favorites logic
            updateTabs();
        });
    }

    private void updateTabs() {
        tabFolders.setTextColor(isShowingFolders ? 0xFFFFFFFF : 0xFFAAAAAA);
        // tabFavorites....
    }

    private void showFolders() {
        isShowingFolders = true;
        adapter = new MusicAdapter(folders, null, item -> {
            // Folder Click
            if (item instanceof MusicRepository.AudioFolder) {
                selectedFolder = (MusicRepository.AudioFolder) item;
                showTracks(selectedFolder);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void showTracks(MusicRepository.AudioFolder folder) {
        if (folder == null)
            return;
        isShowingFolders = false;
        adapter = new MusicAdapter(null, folder.getTracks(), item -> {
            // Track Click
            if (item instanceof MusicRepository.AudioTrack) {
                int index = folder.getTracks().indexOf(item);
                musicManager.getInternalPlayer().setPlaylist(folder.getTracks(), index);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (musicManager != null)
            musicManager.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (musicManager != null)
            musicManager.removeListener(this);
    }

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (nowPlayingTitle != null)
            nowPlayingTitle.setText(title);
        if (nowPlayingArtist != null)
            nowPlayingArtist.setText(artist);
        if (nowPlayingArt != null) {
            if (albumArt != null)
                nowPlayingArt.setImageBitmap(albumArt);
            else
                nowPlayingArt.setImageResource(android.R.drawable.ic_media_play);
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
    }

    // --- Adapter ---

    private static class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.Holder> {

        private final List<?> items;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Object item);
        }

        public MusicAdapter(List<MusicRepository.AudioFolder> folders, List<MusicRepository.AudioTrack> tracks,
                OnItemClickListener listener) {
            if (folders != null)
                this.items = folders;
            else
                this.items = tracks != null ? tracks : new ArrayList<>();
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(net.osmand.plus.R.layout.item_music_track, parent,
                    false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Object item = items.get(position);

            if (item instanceof MusicRepository.AudioFolder) {
                MusicRepository.AudioFolder f = (MusicRepository.AudioFolder) item;
                holder.title.setText(f.getName());
                holder.artist.setText(f.getTracks().size() + " songs");
                holder.icon.setImageResource(android.R.drawable.ic_menu_agenda); // Folder Icon
                holder.duration.setVisibility(View.GONE);
            } else if (item instanceof MusicRepository.AudioTrack) {
                MusicRepository.AudioTrack t = (MusicRepository.AudioTrack) item;
                holder.title.setText(t.getTitle());
                holder.artist.setText(t.getArtist());
                holder.icon.setImageResource(android.R.drawable.ic_media_play); // Track Icon
                holder.duration.setVisibility(View.VISIBLE);
                holder.duration.setText(formatDuration(0)); // Duration TODO
            }

            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        private String formatDuration(long millis) {
            // Simple formatter
            return "";
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView title, artist, duration;
            ImageView icon;

            public Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(net.osmand.plus.R.id.music_title);
                artist = itemView.findViewById(net.osmand.plus.R.id.music_artist);
                duration = itemView.findViewById(net.osmand.plus.R.id.music_duration);
                icon = itemView.findViewById(net.osmand.plus.R.id.music_icon);
            }
        }
    }
}
