package com.echoshow.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

/** Original Echo Show home: clock, live weather, date, music strip, night clock. */
public class HomeView extends View {
    private static final int WHITE = 0xFFF2F3F5;
    private static final int GRAY = 0xFF969BA3;
    private static final int AMBER = 0xFFFFB340;
    private static final int RED = 0xFFFF2E2E;

    private final Paint mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAmPmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mWeatherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mWeatherHit = new RectF();
    private final RectF mAppsHit = new RectF();
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
            float intro = clamp((System.currentTimeMillis() - mAnimStart) / 700f);
            invalidate();
            if (intro < 1f) mHandler.postDelayed(this, 16L);
        }
    };

    private GestureDetector mGesture;
    private AutoBrightness mAutoBrightness;
    private NightOverlay mNightOverlay;
    private WeatherData mWeather = new WeatherData();
    private float mScale = 1f;
    private long mAnimStart;
    private long mTimeAnimStart;
    private int mLastMinute = -1;

    public HomeView(Context c) { super(c); init(c); }
    public HomeView(Context c, AttributeSet a) { super(c, a); init(c); }

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
        mNightPaint.setTypeface(light);
        mNightPaint.setTextAlign(Paint.Align.CENTER);

        mGesture = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                handleTap(e.getX(), e.getY());
                return true;
            }
            @Override public void onLongPress(MotionEvent e) {
                if (mClockHit.contains(e.getX(), e.getY()) && mAutoBrightness != null) {
                    mAutoBrightness.setEnabled(!mAutoBrightness.isEnabled());
                    Toast.makeText(getContext(), mAutoBrightness.isEnabled()
                                    ? "Auto brightness on" : "Auto brightness off",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mNightOverlay = new NightOverlay(this);
        mAnimStart = System.currentTimeMillis();
        mHandler.post(mClockTick);
        mHandler.post(mAnimFrame);
    }

    public void setAutoBrightness(AutoBrightness autoBrightness) {
        mAutoBrightness = autoBrightness;
        mAutoBrightness.setManualListener(new AutoBrightness.ManualListener() {
            @Override public void onManualOverride() {
                Toast.makeText(getContext(),
                        "Brightness set manually — long-press the clock to re-enable auto",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float easeOut(float t) { return 1f - (1f - t) * (1f - t) * (1f - t); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && mNightOverlay.isActive()) {
            NightMode.suppress(30000L);
        }
        mGesture.onTouchEvent(e);
        return true;
    }

    private void handleTap(float x, float y) {
        if (mMusicToggleHit.width() > 0 && mMusicToggleHit.contains(x, y)) {
            MusicPlayer.toggle();
            return;
        }
        if (mMusicHit.contains(x, y)) {
            open(MusicActivity.class);
            return;
        }
        if (mAppsHit.contains(x, y)) {
            open(AppsMenuActivity.class);
            return;
        }
        if (mWeatherHit.contains(x, y)) open(WeatherActivity.class);
    }

    private void open(Class<?> cls) {
        getContext().startActivity(new Intent(getContext(), cls));
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        mScale = w / 960f;
        float s = mScale;

        WeatherData fresh = WeatherData.load(getContext());
        if (fresh.fetchedAt != mWeather.fetchedAt) mWeather = fresh;

        Wallpaper.draw(c, w, h);
        c.drawColor(0x33000000);

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
        drawAppsButton(c, w, s);
        drawClock(c, w, h, s);
        c.restoreToCount(sc);

        drawMusicBar(c, w, h, s);
        mNightOverlay.draw(c, w, h, s);
    }

    private void drawWeather(Canvas c, float s) {
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

    private void drawAppsButton(Canvas c, int w, float s) {
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
        mAppsHit.set(w - 74f * s, 12f * s, w - 10f * s, 72f * s);
    }

    private void drawClock(Canvas c, int w, int h, float s) {
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

    private void drawMusicBar(Canvas c, int w, int h, float s) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFF1C1F24);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextSize(19f * s);

        float y = h - 62f * s;
        float barH = 44f * s;
        MusicPlayer.Track t = MusicPlayer.current();
        if (t == null) {
            mMusicHit.set(-1f, -1f, -1f, -1f);
            mMusicToggleHit.set(-1f, -1f, -1f, -1f);
            return;
        }
        float x = 24f * s;
        float barW = 320f * s;
        RectF r = new RectF(x, y, x + barW, y + barH);
        c.drawRoundRect(r, barH / 2f, barH / 2f, bg);

        Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
        icon.setColor(AMBER);
        c.drawCircle(x + 20f * s, y + barH / 2f + 5f * s, 5f * s, icon);
        c.drawRect(x + 24f * s, y + barH / 2f - 12f * s, x + 27f * s, y + barH / 2f + 6f * s, icon);
        c.drawRect(x + 24f * s, y + barH / 2f - 12f * s, x + 34f * s, y + barH / 2f - 9f * s, icon);

        String title = t.title;
        if (title.length() > 26) title = title.substring(0, 25) + "…";
        text.setColor(MusicPlayer.isPlaying() ? WHITE : 0xFFB9BEC6);
        c.drawText(title, x + 44f * s, y + barH / 2f + 7f * s, text);

        float px = x + barW - 30f * s;
        float py = y + barH / 2f;
        Paint ctrl = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrl.setColor(WHITE);
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
    }
}
