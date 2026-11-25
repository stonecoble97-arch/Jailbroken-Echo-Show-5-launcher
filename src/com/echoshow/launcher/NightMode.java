package com.echoshow.launcher;

import java.util.ArrayList;
import java.util.List;

/** Shared near-darkness state so every screen can fade the same night overlay. */
public final class NightMode {
    private NightMode() { }

    public interface Listener { void onNightChanged(boolean night); }

    private static final List<Listener> LISTENERS = new ArrayList<Listener>();
    private static boolean sNight;
    private static long sSuppressUntil;

    public static boolean isNight() { return sNight; }

    /** Tap-to-wake: ignore darkness for a short while so night mode can't feel stuck. */
    public static synchronized void suppress(long millis) {
        sSuppressUntil = System.currentTimeMillis() + millis;
        if (sNight) set(false);
    }

    public static synchronized void set(boolean night) {
        if (night && System.currentTimeMillis() < sSuppressUntil) return;
        if (sNight == night) return;
        sNight = night;
        for (Listener l : new ArrayList<Listener>(LISTENERS)) {
            l.onNightChanged(night);
        }
    }

    public static synchronized void addListener(Listener l) {
        if (!LISTENERS.contains(l)) LISTENERS.add(l);
    }

    public static synchronized void removeListener(Listener l) {
        LISTENERS.remove(l);
    }
}
