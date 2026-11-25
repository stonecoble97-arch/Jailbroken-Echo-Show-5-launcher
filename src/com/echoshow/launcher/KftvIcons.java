package com.echoshow.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Thin outline icons in the style of the classic KFTV / Fire TV menus. */
public final class KftvIcons {
    private KftvIcons() { }

    public static void grid(Canvas c, float cx, float cy, float s, Paint p) {
        float d = 22f * s;
        float gap = 7f * s;
        c.drawRect(new RectF(cx - d - gap / 2, cy - d - gap / 2, cx - gap / 2, cy - gap / 2), p);
        c.drawRect(new RectF(cx + gap / 2, cy - d - gap / 2, cx + d + gap / 2, cy - gap / 2), p);
        c.drawRect(new RectF(cx - d - gap / 2, cy + gap / 2, cx - gap / 2, cy + d + gap / 2), p);
        c.drawRect(new RectF(cx + gap / 2, cy + gap / 2, cx + d + gap / 2, cy + d + gap / 2), p);
    }

    public static void gamepad(Canvas c, float cx, float cy, float s, Paint p) {
        RectF r = new RectF(cx - 32f * s, cy - 20f * s, cx + 32f * s, cy + 20f * s);
        c.drawRoundRect(r, 18f * s, 18f * s, p);
        c.drawLine(cx - 20f * s, cy - 6f * s, cx - 20f * s, cy + 6f * s, p);
        c.drawLine(cx - 26f * s, cy, cx - 14f * s, cy, p);
        c.drawCircle(cx + 12f * s, cy - 6f * s, 3.2f * s, p);
        c.drawCircle(cx + 21f * s, cy + 3f * s, 3.2f * s, p);
    }

    public static void play(Canvas c, float cx, float cy, float s, Paint p) {
        c.drawCircle(cx, cy, 26f * s, p);
        Path tri = new Path();
        tri.moveTo(cx - 7f * s, cy - 12f * s);
        tri.lineTo(cx + 13f * s, cy);
        tri.lineTo(cx - 7f * s, cy + 12f * s);
        tri.close();
        c.drawPath(tri, p);
    }

    public static void sliders(Canvas c, float cx, float cy, float s, Paint p) {
        float[] ys = {cy - 13f * s, cy, cy + 13f * s};
        float[] xs = {cx + 12f * s, cx - 10f * s, cx + 16f * s};
        for (int i = 0; i < 3; i++) {
            c.drawLine(cx - 30f * s, ys[i], cx + 30f * s, ys[i], p);
            c.drawCircle(xs[i], ys[i], 6f * s, p);
        }
    }

    public static void monitor(Canvas c, float cx, float cy, float s, Paint p) {
        c.drawRect(new RectF(cx - 30f * s, cy - 24f * s, cx + 30f * s, cy + 14f * s), p);
        c.drawLine(cx, cy + 14f * s, cx, cy + 24f * s, p);
        c.drawLine(cx - 16f * s, cy + 24f * s, cx + 16f * s, cy + 24f * s, p);
        c.drawCircle(cx - 20f * s, cy - 16f * s, 2f * s, p);
    }

    public static void alexa(Canvas c, float cx, float cy, float s, Paint p) {
        c.drawCircle(cx, cy, 26f * s, p);
        Path cut = new Path();
        cut.moveTo(cx - 9f * s, cy + 24f * s);
        cut.lineTo(cx + 4f * s, cy + 25f * s);
        cut.lineTo(cx - 1f * s, cy + 12f * s);
        cut.lineTo(cx - 12f * s, cy + 16f * s);
        cut.close();
        c.drawPath(cut, p);
    }

    public static void music(Canvas c, float cx, float cy, float s, Paint p) {
        c.drawCircle(cx - 10f * s, cy + 12f * s, 8f * s, p);
        c.drawLine(cx - 2f * s, cy + 12f * s, cx - 2f * s, cy - 18f * s, p);
        c.drawLine(cx - 2f * s, cy - 18f * s, cx + 14f * s, cy - 14f * s, p);
        c.drawLine(cx + 14f * s, cy - 14f * s, cx + 14f * s, cy - 6f * s, p);
    }

    public static void chevron(Canvas c, float cx, float cy, float s, Paint p, boolean left) {
        Path path = new Path();
        if (left) {
            path.moveTo(cx + 8f * s, cy - 14f * s);
            path.lineTo(cx - 6f * s, cy);
            path.lineTo(cx + 8f * s, cy + 14f * s);
        } else {
            path.moveTo(cx - 8f * s, cy - 14f * s);
            path.lineTo(cx + 6f * s, cy);
            path.lineTo(cx - 8f * s, cy + 14f * s);
        }
        c.drawPath(path, p);
    }
}
