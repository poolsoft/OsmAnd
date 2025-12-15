package net.osmand.plus.carlauncher.music;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for scanning and managing local music files.
 * Handles checking external apps vs local files.
 */
public class MusicRepository {

    private static final String TAG = "MusicRepository";
    private final Context context;
    private List<AudioTrack> cachedTracks = new ArrayList<>();
    private List<AudioFolder> cachedFolders = new ArrayList<>();

    // Singleton support if needed, or instantiated by MusicManager
    public MusicRepository(Context context) {
        this.context = context;
    }

    public interface OnScanCompletedListener {
        void onScanCompleted(List<AudioTrack> tracks, List<AudioFolder> folders);
    }

    /**
     * Scan device for music files asynchronously.
     */
    public void scanMusic(final OnScanCompletedListener listener) {
        new Thread(() -> {
            List<AudioTrack> tracks = scanDeviceForAudio();
            List<AudioFolder> folders = organizeIntoFolders(tracks);

            synchronized (this) {
                cachedTracks = tracks;
                cachedFolders = folders;
            }

            if (listener != null) {
                // Return on main thread if possible, but caller usually handles threading
                listener.onScanCompleted(tracks, folders);
            }
        }).start();
    }

    public List<AudioTrack> getCachedTracks() {
        return cachedTracks;
    }

    public List<AudioFolder> getCachedFolders() {
        return cachedFolders;
    }

    private List<AudioTrack> scanDeviceForAudio() {
        List<AudioTrack> tracks = new ArrayList<>();

        Uri collection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA, // Path
                MediaStore.Audio.Media.ALBUM_ID
        };

        // Filter for music only
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    long duration = cursor.getLong(durationColumn);
                    String path = cursor.getString(dataColumn);
                    long albumId = cursor.getLong(albumIdColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // Album art uri
                    Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),
                            albumId);

                    AudioTrack track = new AudioTrack(id, title, artist, album, duration, path, contentUri,
                            albumArtUri);
                    tracks.add(track);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning audio", e);
        }

        return tracks;
    }

    private List<AudioFolder> organizeIntoFolders(List<AudioTrack> tracks) {
        Map<String, List<AudioTrack>> folderMap = new HashMap<>();

        for (AudioTrack track : tracks) {
            if (track.getPath() == null)
                continue;

            File file = new File(track.getPath());
            File parent = file.getParentFile();
            if (parent != null) {
                String folderName = parent.getName();
                String folderPath = parent.getAbsolutePath();

                // Use path as key to handle duplicate names in different locations
                if (!folderMap.containsKey(folderPath)) {
                    folderMap.put(folderPath, new ArrayList<>());
                }
                folderMap.get(folderPath).add(track);
            }
        }

        List<AudioFolder> folders = new ArrayList<>();
        for (Map.Entry<String, List<AudioTrack>> entry : folderMap.entrySet()) {
            File f = new File(entry.getKey());
            folders.add(new AudioFolder(f.getName(), entry.getKey(), entry.getValue()));
        }

        // Sort folders by name
        Collections.sort(folders, new Comparator<AudioFolder>() {
            @Override
            public int compare(AudioFolder o1, AudioFolder o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return folders;
    }

    // --- Data Models ---

    public static class AudioTrack {
        private final long id;
        private final String title;
        private final String artist;
        private final String album;
        private final long duration;
        private final String path;
        private final Uri contentUri;
        private final Uri albumArtUri;

        public AudioTrack(long id, String title, String artist, String album, long duration, String path,
                Uri contentUri, Uri albumArtUri) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
            this.path = path;
            this.contentUri = contentUri;
            this.albumArtUri = albumArtUri;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public Uri getContentUri() {
            return contentUri;
        }

        public String getPath() {
            return path;
        }

        public Uri getAlbumArtUri() {
            return albumArtUri;
        }
    }

    public static class AudioFolder {
        private final String name;
        private final String path;
        private final List<AudioTrack> tracks;

        public AudioFolder(String name, String path, List<AudioTrack> tracks) {
            this.name = name;
            this.path = path;
            this.tracks = tracks;
        }

        public String getName() {
            return name;
        }

        public List<AudioTrack> getTracks() {
            return tracks;
        }
    }
}
