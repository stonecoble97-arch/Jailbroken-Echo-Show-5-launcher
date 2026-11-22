package com.echoshow.launcher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

/** Simple in-app player: local tracks plus preset internet radio stations. */
public final class MusicPlayer {
    private MusicPlayer() { }

    public static class Track {
        public final String title;
        public final String artist;
        public final String source;
        public final boolean radio;

        public Track(String title, String artist, String source, boolean radio) {
            this.title = title;
            this.artist = artist;
            this.source = source;
            this.radio = radio;
        }
    }

    public interface Listener { void onChanged(); }

    private static final List<Track> TRACKS = new ArrayList<Track>();
    private static final List<Listener> LISTENERS = new ArrayList<Listener>();
    private static final Object LOCK = new Object();

    private static Context sCtx;
    private static MediaPlayer sPlayer;
    private static int sIndex = -1;
    private static boolean sPrepared;

    static {
        TRACKS.add(new Track("NCS Radio", "NoCopyrightSounds",
                "https://hyperadio-nocopyright.stream.laut.fm/hyperadio-nocopyright", true));
        TRACKS.add(new Track("Groove Salad", "SomaFM",
                "https://ice1.somafm.com/groovesalad-128-mp3", true));
        TRACKS.add(new Track("Indie Pop Rocks", "SomaFM",
                "https://ice1.somafm.com/indiepop-128-mp3", true));
        TRACKS.add(new Track("Drone Zone", "SomaFM",
                "https://ice1.somafm.com/dronezone-128-mp3", true));
    }

    public static void init(Context c) {
        sCtx = c.getApplicationContext();
        loadLocal();
    }

    public static void loadLocal() {
        if (sCtx == null) return;
        synchronized (LOCK) {
            for (int i = TRACKS.size() - 1; i >= 0; i--) {
                if (!TRACKS.get(i).radio) TRACKS.remove(i);
            }
        }
        if (sCtx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            notifyListeners();
            return;
        }
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };
        try {
            Cursor cur = sCtx.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0",
                    null,
                    MediaStore.Audio.Media.TITLE);
            if (cur != null) {
                while (cur.moveToNext()) {
                    long id = cur.getLong(0);
                    String title = cur.getString(1);
                    String artist = cur.getString(2);
                    Uri uri = Uri.parse("content://media/external/audio/media/" + id);
                    synchronized (LOCK) {
                        TRACKS.add(new Track(
                                title == null ? "Unknown" : title,
                                artist == null || artist.isEmpty() ? "Unknown artist" : artist,
                                uri.toString(), false));
                    }
                }
                cur.close();
            }
        } catch (Exception ignored) { }
        notifyListeners();
    }

    public static List<Track> tracks() {
        synchronized (LOCK) {
            return new ArrayList<Track>(TRACKS);
        }
    }

    public static Track current() {
        synchronized (LOCK) {
            return sIndex >= 0 && sIndex < TRACKS.size() ? TRACKS.get(sIndex) : null;
        }
    }

    public static int index() { return sIndex; }
    public static boolean isPlaying() { return sPlayer != null && sPlayer.isPlaying(); }
    public static boolean isPrepared() { return sPrepared; }

    public static int position() {
        try { return sPlayer == null ? 0 : sPlayer.getCurrentPosition(); }
        catch (Exception e) { return 0; }
    }

    public static int duration() {
        Track t = current();
        if (t == null || t.radio || sPlayer == null || !sPrepared) return 0;
        try { return sPlayer.getDuration(); }
        catch (Exception e) { return 0; }
    }

    public static void toggle() {
        if (sPlayer == null) {
            play(0);
            return;
        }
        if (sPlayer.isPlaying()) sPlayer.pause();
        else sPlayer.start();
        notifyListeners();
    }

    public static void next() {
        synchronized (LOCK) {
            if (TRACKS.isEmpty()) return;
        }
        play((sIndex + 1) % trackCount());
    }

    public static void prev() {
        int count = trackCount();
        if (count == 0) return;
        play((sIndex <= 0 ? count - 1 : sIndex - 1));
    }

    public static void play(int index) {
        synchronized (LOCK) {
            if (index < 0 || index >= TRACKS.size()) return;
        }
        releasePlayer();
        sIndex = index;
        sPrepared = false;
        Track t = current();
        try {
            sPlayer = new MediaPlayer();
            sPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (t.radio) {
                sPlayer.setDataSource(t.source);
            } else {
                sPlayer.setDataSource(sCtx, Uri.parse(t.source));
            }
            sPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    sPrepared = true;
                    try { mp.start(); } catch (Exception ignored) { }
                    notifyListeners();
                }
            });
            sPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) { next(); }
            });
            sPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    notifyListeners();
                    return true;
                }
            });
            sPlayer.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            sIndex = -1;
        }
        notifyListeners();
    }

    public static void seek(int ms) {
        try { if (sPlayer != null && sPrepared) sPlayer.seekTo(ms); }
        catch (Exception ignored) { }
    }

    private static int trackCount() {
        synchronized (LOCK) { return TRACKS.size(); }
    }

    private static void releasePlayer() {
        if (sPlayer != null) {
            try { sPlayer.reset(); } catch (Exception ignored) { }
            try { sPlayer.release(); } catch (Exception ignored) { }
            sPlayer = null;
        }
        sPrepared = false;
    }

    public static void addListener(Listener l) {
        synchronized (LISTENERS) { if (!LISTENERS.contains(l)) LISTENERS.add(l); }
    }

    public static void removeListener(Listener l) {
        synchronized (LISTENERS) { LISTENERS.remove(l); }
    }

    private static void notifyListeners() {
        List<Listener> copy;
        synchronized (LISTENERS) { copy = new ArrayList<Listener>(LISTENERS); }
        for (Listener l : copy) {
            try { l.onChanged(); } catch (Exception ignored) { }
        }
    }
}
