package com.echoshow.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Echo Show style home screen with a wallpaper, intro/ambient animations and an app drawer. */
public class HomeView extends View {
    private static final int WHITE = 0xFFF2F3F5;
    private static final int GRAY = 0xFF969BA3;
    private static final int AMBER = 0xFFFFB340;
    private static final int BLUE = 0xFF4FA8FF;

    private final Paint mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAmPmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mWeatherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDrawerTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDrawerLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mWeatherHit = new RectF();
    private final RectF mDrawerHit = new RectF();
    private final RectF mClockHit = new RectF();
    private final RectF mMusicHit = new RectF();
    private final RectF mMusicToggleHit = new RectF();

    private final Handler mHandler = new Handler();
    private final Runnable mClockTick = new Runnable() {
        @Override public void run() {
            Calendar now = Calendar.getInstance();
            int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            if (mLastMinute != -1 && minute != mLastMinute) {
                mTimeAnimStart = System.currentTimeMillis();
            }
            mLastMinute = minute;
            invalidate();
            mHandler.postDelayed(this, 250L);
        }
    };

    private final Runnable mAnimFrame = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            float intro = clamp((now - mAnimStart) / 700f);
            float target = mDrawerOpen ? 1f : 0f;
            float p = mDrawerProgress + (target - mDrawerProgress) * 0.22f;
            if (Math.abs(p - target) < 0.004f) p = target;
            mDrawerProgress = p;
            boolean more = intro < 1f || p != target;
            invalidate();
            if (more) mHandler.postDelayed(this, 16L);
        }
    };

    private GestureDetector mGesture;
    private final List<AppInfo> mApps = new ArrayList<AppInfo>();

    private WeatherData mWeather = new WeatherData();
    private AutoBrightness mAutoBrightness;

    private boolean mDrawerOpen = false;
    private float mDrawerProgress = 0f;
    private float mScale = 1f;
    private long mAnimStart;
    private long mTimeAnimStart;
    private int mLastMinute = -1;

    private static class AppInfo {
        String label;
        Drawable icon;
        Intent intent;
    }

    public HomeView(Context c) {
        super(c);
        init(c);
    }

    public HomeView(Context c, AttributeSet a) {
        super(c, a);
        init(c);
    }

    private void init(Context c) {
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);

        mTimePaint.setColor(WHITE);
        mTimePaint.setTypeface(light);
        mTimePaint.setTextAlign(Paint.Align.LEFT);

        mAmPmPaint.setColor(AMBER);
        mAmPmPaint.setTypeface(regular);
        mAmPmPaint.setTextAlign(Paint.Align.LEFT);

        mDatePaint.setColor(GRAY);
        mDatePaint.setTypeface(regular);
        mDatePaint.setTextAlign(Paint.Align.CENTER);

        mWeatherPaint.setColor(GRAY);
        mWeatherPaint.setTypeface(regular);
        mWeatherPaint.setTextAlign(Paint.Align.LEFT);

        mGridPaint.setColor(0xE6FFFFFF);
        mDrawerTitlePaint.setColor(WHITE);
        mDrawerTitlePaint.setTypeface(light);
        mDrawerTitlePaint.setTextAlign(Paint.Align.CENTER);
        mDrawerLabelPaint.setColor(0xFFE8EAED);
        mDrawerLabelPaint.setTypeface(regular);
        mDrawerLabelPaint.setTextAlign(Paint.Align.CENTER);

        mGesture = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onSingleTapUp(MotionEvent e) {
                handleTap(e.getX(), e.getY());
                return true;
            }

            @Override public void onLongPress(MotionEvent e) {
                if (mClockHit.contains(e.getX(), e.getY()) && mAutoBrightness != null) {
                    mAutoBrightness.setEnabled(!mAutoBrightness.isEnabled());
                    Toast.makeText(getContext(),
                            mAutoBrightness.isEnabled()
                                    ? "Auto brightness on" : "Auto brightness off",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (vy < -600f) { setDrawerOpen(true); return true; }
                if (vy > 600f) { setDrawerOpen(false); return true; }
                return false;
            }
        });

        loadApps(c);
        mAnimStart = System.currentTimeMillis();
        mHandler.post(mClockTick);
        startAnim();
    }

    public void setAutoBrightness(AutoBrightness autoBrightness) {
        mAutoBrightness = autoBrightness;
    }

    private void loadApps(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(pm));
        for (ResolveInfo ri : list) {
            AppInfo ai = new AppInfo();
            ai.label = ri.loadLabel(pm).toString();
            ai.icon = ri.loadIcon(pm);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ai.intent = i;
            mApps.add(ai);
        }
    }

    private void startAnim() {
        mHandler.removeCallbacks(mAnimFrame);
        mHandler.post(mAnimFrame);
    }

    private void setDrawerOpen(boolean open) {
        mDrawerOpen = open;
        startAnim();
    }

    public boolean closeDrawer() {
        if (!mDrawerOpen) return false;
        setDrawerOpen(false);
        return true;
    }

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float easeOut(float t) { return 1f - (1f - t) * (1f - t) * (1f - t); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mGesture.onTouchEvent(e);
        return true;
    }

    private void handleTap(float x, float y) {
        if (mDrawerOpen) {
            float top = getHeight() * 0.14f;
            float cellW = getWidth() / 5f;
            float cellH = 118f * mScale;
            int col = (int) (x / cellW);
            int row = (int) ((y - top) / cellH);
            if (col >= 0 && col < 5 && row >= 0) {
                int idx = row * 5 + col;
                if (idx >= 0 && idx < mApps.size()) {
                    getContext().startActivity(mApps.get(idx).intent);
                    if (getContext() instanceof Activity) {
                        ((Activity) getContext()).overridePendingTransition(
                                android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
            }
            setDrawerOpen(false);
            return;
        }
        if (mMusicToggleHit.width() > 0 && mMusicToggleHit.contains(x, y)) {
            MusicPlayer.toggle();
            return;
        }
        if (mMusicHit.contains(x, y)) {
            if (getContext() instanceof HomeActivity) {
                ((HomeActivity) getContext()).openMusic();
            }
            return;
        }
        if (mWeatherHit.contains(x, y)) {
            getContext().startActivity(new Intent(getContext(), WeatherActivity.class));
            if (getContext() instanceof Activity) {
                ((Activity) getContext()).overridePendingTransition(
                        android.R.anim.fade_in, android.R.anim.fade_out);
            }
            return;
        }
        if (mDrawerHit.contains(x, y)) setDrawerOpen(true);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        mScale = w / 960f;
        float s = mScale;

        Wallpaper.draw(c, w, h);
        c.drawColor(0x33000000);

        // Slow breathing glow behind the clock.
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 1400.0);
        int ga = (int) (14 + 18 * pulse);
        mGlowPaint.setShader(new RadialGradient(w * 0.5f, h * 0.44f, h * 0.72f,
                Color.argb(ga, 0x35, 0x5C, 0x78), 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, mGlowPaint);
        mGlowPaint.setShader(null);

        float intro = easeOut(clamp((System.currentTimeMillis() - mAnimStart) / 700f));
        Paint layer = new Paint();
        layer.setAlpha((int) (255 * intro));
        int sc = c.saveLayer(0, 0, w, h, layer);
        c.translate(0, (1f - intro) * 36f * s);
        drawWeather(c, s);
        drawClock(c, w, h, s);
        c.restoreToCount(sc);

        drawMusicBar(c, w, h, s);

        if (mDrawerProgress > 0f) drawDrawer(c, w, h, s, mDrawerProgress);
    }

    private void drawWeather(Canvas c, float s) {
        WeatherData fresh = WeatherData.load(getContext());
        if (fresh.fetchedAt != mWeather.fetchedAt) mWeather = fresh;
        WeatherData d = mWeather;

        float pad = 30f * s;
        float cy = 46f * s;
        float rotation = (System.currentTimeMillis() / 90f) % 360f;
        Icons.drawWeather(c, pad + 14f * s, cy, 1.05f * s, d.code, d.isDay, rotation);
        String text = d.temp + "°  ·  " + d.condition;
        mWeatherPaint.setTextSize(26f * s);
        float tx = pad + 42f * s;
        c.drawText(text, tx, cy + 9f * s, mWeatherPaint);
        mWeatherHit.set(pad - 10f * s, 8f * s,
                tx + mWeatherPaint.measureText(text) + 10f * s, 68f * s);
    }

    private void drawDrawerButton(Canvas c, int w, float s) {
        float gx = w - 58f * s;
        float gy = 24f * s;
        float cell = 12f * s;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float x = gx + col * cell;
                float y = gy + row * cell;
                c.drawRoundRect(new RectF(x, y, x + 7f * s, y + 7f * s),
                        1.5f * s, 1.5f * s, mGridPaint);
            }
        }
        mDrawerHit.set(w - 74f * s, 12f * s, w - 10f * s, 72f * s);
    }

    private void drawClock(Canvas c, int w, int h, float s) {
        drawDrawerButton(c, w, s);

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        int minute = now.get(Calendar.MINUTE);
        String time = String.format(Locale.US, "%d:%02d", hour, minute);
        String ampm = now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

        mTimePaint.setTextSize(196f * s);
        mAmPmPaint.setTextSize(54f * s);

        float timeW = mTimePaint.measureText(time);
        float amW = mAmPmPaint.measureText(ampm);
        float gap = 12f * s;
        float total = timeW + gap + amW;
        float startX = (w - total) / 2f;

        float centerY = h * 0.42f;
        FontMetrics fm = mTimePaint.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2f;
        mClockHit.set(startX - 10f * s, baseline - 160f * s,
                startX + total + 10f * s, baseline + 40f * s);

        // Subtle fade each time the minute rolls over.
        float tp = clamp((System.currentTimeMillis() - mTimeAnimStart) / 380f);
        int alpha = (int) (96 + 159 * tp);
        mTimePaint.setAlpha(alpha);
        mAmPmPaint.setAlpha(alpha);
        mTimePaint.setColor(WHITE);
        c.drawText(time, startX, baseline, mTimePaint);
        mAmPmPaint.setColor(AMBER);
        c.drawText(ampm, startX + timeW + gap, baseline - 2f * s, mAmPmPaint);
        mTimePaint.setAlpha(255);
        mAmPmPaint.setAlpha(255);

        String date = DateFormat.format("EEEE, MMMM d", now).toString();
        mDatePaint.setTextSize(30f * s);
        c.drawText(date, w / 2f, baseline + 126f * s, mDatePaint);
    }

    private void drawMusicNote(Canvas c, float cx, float cy, float s, Paint p) {
        c.drawCircle(cx - 4f * s, cy + 5f * s, 5f * s, p);
        c.drawRect(cx, cy - 12f * s, cx + 3f * s, cy + 6f * s, p);
        c.drawRect(cx, cy - 12f * s, cx + 10f * s, cy - 9f * s, p);
    }

    private void drawMusicBar(Canvas c, int w, int h, float s) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFF1C1F24);
        Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
        icon.setColor(0xFFFFB340);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(0xFFE8EAED);
        text.setTextSize(19f * s);

        float y = h - 62f * s;
        float barH = 44f * s;
        MusicPlayer.Track t = MusicPlayer.current();
        if (t != null) {
            float x = 24f * s;
            float barW = 320f * s;
            RectF r = new RectF(x, y, x + barW, y + barH);
            c.drawRoundRect(r, barH / 2f, barH / 2f, bg);
            drawMusicNote(c, x + 24f * s, y + barH / 2f, s, icon);
            String title = t.title;
            if (title.length() > 26) title = title.substring(0, 25) + "…";
            text.setColor(MusicPlayer.isPlaying() ? 0xFFF2F3F5 : 0xFFB9BEC6);
            c.drawText(title, x + 44f * s, y + barH / 2f + 7f * s, text);

            float px = x + barW - 30f * s;
            float py = y + barH / 2f;
            Paint ctrl = new Paint(Paint.ANTI_ALIAS_FLAG);
            ctrl.setColor(0xFFFFFFFF);
            if (MusicPlayer.isPlaying()) {
                ctrl.setStrokeWidth(4f * s);
                c.drawLine(px - 5f * s, py - 9f * s, px - 5f * s, py + 9f * s, ctrl);
                c.drawLine(px + 5f * s, py - 9f * s, px + 5f * s, py + 9f * s, ctrl);
            } else {
                android.graphics.Path tri = new android.graphics.Path();
                tri.moveTo(px - 6f * s, py - 10f * s);
                tri.lineTo(px + 9f * s, py);
                tri.lineTo(px - 6f * s, py + 10f * s);
                tri.close();
                c.drawPath(tri, ctrl);
            }
            mMusicHit.set(r);
            mMusicToggleHit.set(x + barW - 54f * s, y, x + barW, y + barH);
        } else {
            float x = 46f * s;
            float cy = y + barH / 2f;
            c.drawCircle(x, cy, 22f * s, bg);
            drawMusicNote(c, x, cy, 1.2f * s, icon);
            mMusicHit.set(x - 22f * s, cy - 22f * s, x + 22f * s, cy + 22f * s);
            mMusicToggleHit.set(-1f, -1f, -1f, -1f);
        }
    }

    private void drawDrawer(Canvas c, int w, int h, float s, float p) {
        Paint layer = new Paint();
        layer.setAlpha((int) (255 * p));
        int sc = c.saveLayer(0, 0, w, h, layer);
        c.translate(0, (1f - p) * 28f * s);
        c.drawColor(0xFF08090C);

        mDrawerTitlePaint.setTextSize(30f * s);
        c.drawText("Apps", w / 2f, 58f * s, mDrawerTitlePaint);

        float top = h * 0.14f;
        float cellW = w / 5f;
        float cellH = 118f * s;
        mDrawerLabelPaint.setTextSize(17f * s);
        int count = Math.min(mApps.size(), 10);
        for (int i = 0; i < count; i++) {
            AppInfo ai = mApps.get(i);
            int col = i % 5;
            int row = i / 5;
            float cx = col * cellW + cellW / 2f;
            float cy = top + row * cellH + 42f * s;
            Drawable d = ai.icon;
            d.setBounds((int) (cx - 26f * s), (int) (cy - 26f * s),
                    (int) (cx + 26f * s), (int) (cy + 26f * s));
            d.draw(c);
            String label = ai.label;
            if (label.length() > 12) label = label.substring(0, 11) + "…";
            c.drawText(label, cx, top + row * cellH + 90f * s, mDrawerLabelPaint);
        }
        c.restoreToCount(sc);
    }
}
