package com.echoshow.launcher;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** A self-contained abstract wallpaper drawn on canvas (no external assets). */
public final class Wallpaper {
    private Wallpaper() { }

    public static void draw(Canvas c, int w, int h) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                0xFF0B0E16, 0xFF050609, Shader.TileMode.CLAMP);
        p.setShader(bg);
        c.drawRect(0, 0, w, h, p);

        blob(c, w, h, 0.16f, 0.26f, 0.58f, 0x4D1A8E88);
        blob(c, w, h, 0.78f, 0.18f, 0.52f, 0x4D3A2E7C);
        blob(c, w, h, 0.54f, 0.88f, 0.62f, 0x3D5A2E70);
        blob(c, w, h, 0.94f, 0.74f, 0.46f, 0x337A4A18);
        blob(c, w, h, 0.28f, 0.98f, 0.50f, 0x331A5A70);

        RadialGradient vignette = new RadialGradient(w / 2f, h / 2f,
                Math.max(w, h) * 0.64f, 0x00000000, 0x88000000, Shader.TileMode.CLAMP);
        p.setShader(vignette);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private static void blob(Canvas c, int w, int h, float x, float y, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient g = new RadialGradient(w * x, h * y, Math.min(w, h) * r,
                color, 0x00000000, Shader.TileMode.CLAMP);
        p.setShader(g);
        c.drawRect(0, 0, w, h, p);
    }
}
