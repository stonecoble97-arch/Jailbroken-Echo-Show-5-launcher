package com.echoshow.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Canvas icons shared by the home and weather screens. */
public final class Icons {
    private Icons() { }

    private static final Paint SUN = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint CLOUD = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint RAIN = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint SNOW = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint BOLT = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        SUN.setColor(0xFFFFC63D);
        SUN.setStrokeWidth(2f);
        CLOUD.setColor(0xFFBFC7D1);
        RAIN.setColor(0xFF6FC3FF);
        RAIN.setStrokeWidth(2.5f);
        SNOW.setColor(0xFFE8F2FF);
        BOLT.setColor(0xFFFFC63D);
    }

    /** Sun-behind-cloud, centred on (cx, cy) and scaled by s. */
    public static void drawSunCloud(Canvas c, float cx, float cy, float s) {
        drawSunCloud(c, cx, cy, s, 0f);
    }

    /** Same icon, with the sun rays slowly rotating. */
    public static void drawSunCloud(Canvas c, float cx, float cy, float s, float rotation) {
        drawSun(c, cx - 7f * s, cy - 8f * s, s, rotation);
        drawCloudAt(c, cx, cy, s);
    }

    /** Full weather glyph for a WMO code. */
    public static void drawWeather(Canvas c, float cx, float cy, float s,
                                   int code, boolean isDay, float rotation) {
        if (code <= 1) {
            if (isDay) drawSun(c, cx, cy - 2f * s, 1.15f * s, rotation);
            else drawMoon(c, cx, cy - 2f * s, 1.15f * s);
            return;
        }
        if (code == 2) {
            if (isDay) drawSun(c, cx - 9f * s, cy - 11f * s, 0.75f * s, rotation);
            else drawMoon(c, cx - 9f * s, cy - 11f * s, 0.75f * s);
            drawCloudAt(c, cx + 3f * s, cy + 4f * s, 0.95f * s);
            return;
        }
        drawCloudAt(c, cx, cy, s);
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) {
            drawRain(c, cx, cy, s);
        } else if (code >= 71 && code <= 77 || code >= 85 && code <= 86) {
            drawSnow(c, cx, cy, s);
        } else if (code >= 95) {
            drawBolt(c, cx, cy, s);
        } else if (code == 45 || code == 48) {
            drawFog(c, cx, cy, s);
        }
    }

    private static void drawSun(Canvas c, float cx, float cy, float s, float rotation) {
        c.save();
        c.rotate(rotation, cx, cy);
        SUN.setStrokeWidth(2f * s);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            float c1 = (float) Math.cos(a);
            float s1 = (float) Math.sin(a);
            c.drawLine(cx + 13f * s * c1, cy + 13f * s * s1,
                    cx + 18f * s * c1, cy + 18f * s * s1, SUN);
        }
        c.restore();
        c.drawCircle(cx, cy, 9f * s, SUN);
    }

    private static void drawMoon(Canvas c, float cx, float cy, float s) {
        Path p = new Path();
        p.addCircle(cx, cy, 11f * s, Path.Direction.CW);
        p.addCircle(cx + 5f * s, cy - 4f * s, 10f * s, Path.Direction.CW);
        p.setFillType(Path.FillType.EVEN_ODD);
        c.drawPath(p, SUN);
    }

    private static void drawCloudAt(Canvas c, float cx, float cy, float s) {
        c.drawCircle(cx - 8f * s, cy + 8f * s, 11f * s, CLOUD);
        c.drawCircle(cx + 5f * s, cy + 4f * s, 14f * s, CLOUD);
        c.drawCircle(cx + 18f * s, cy + 10f * s, 10f * s, CLOUD);
        c.drawRect(cx - 8f * s, cy + 8f * s, cx + 18f * s, cy + 18f * s, CLOUD);
    }

    private static void drawRain(Canvas c, float cx, float cy, float s) {
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * 10f * s;
            c.drawLine(x - 2f * s, cy + 22f * s, x - 5f * s, cy + 30f * s, RAIN);
        }
    }

    private static void drawSnow(Canvas c, float cx, float cy, float s) {
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * 11f * s;
            float y = cy + 26f * s;
            c.drawLine(x - 3f * s, y, x + 3f * s, y, SNOW);
            c.drawLine(x, y - 3f * s, x, y + 3f * s, SNOW);
        }
    }

    private static void drawFog(Canvas c, float cx, float cy, float s) {
        for (int i = 0; i < 2; i++) {
            float y = cy + (24f + i * 7f) * s;
            c.drawLine(cx - 16f * s, y, cx + 18f * s, y, CLOUD);
        }
    }

    private static void drawBolt(Canvas c, float cx, float cy, float s) {
        Path p = new Path();
        p.moveTo(cx + 2f * s, cy + 18f * s);
        p.lineTo(cx - 6f * s, cy + 32f * s);
        p.lineTo(cx - 1f * s, cy + 32f * s);
        p.lineTo(cx - 4f * s, cy + 42f * s);
        p.lineTo(cx + 8f * s, cy + 26f * s);
        p.lineTo(cx + 2f * s, cy + 26f * s);
        p.close();
        c.drawPath(p, BOLT);
    }
}
