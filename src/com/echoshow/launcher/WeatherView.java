package com.echoshow.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/** The Echo Show weather screen: current conditions plus a five-day outlook. */
public class WeatherView extends View {
    private final Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mConditionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mButtonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Handler mHandler = new Handler();
    private final Runnable mAnimFrame = new Runnable() {
        @Override public void run() {
            float t = Math.min(1f, (System.currentTimeMillis() - mAnimStart) / 600f);
            invalidate();
            if (t < 1f) mHandler.postDelayed(this, 16L);
        }
    };

    private final Runnable mTick = new Runnable() {
        @Override public void run() { invalidate(); mHandler.postDelayed(this, 1000L); }
    };

    private long mAnimStart;
    private WeatherData mWeather = new WeatherData();

    public WeatherView(Context c) { super(c); init(); }
    public WeatherView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);

        mTitlePaint.setColor(0xFF4FA8FF);
        mTitlePaint.setTypeface(light);
        mTitlePaint.setTextAlign(Paint.Align.LEFT);

        mTimePaint.setColor(0xFF9AA0A6);
        mTimePaint.setTypeface(regular);
        mTimePaint.setTextAlign(Paint.Align.RIGHT);

        mBigPaint.setColor(0xFFF2F3F5);
        mBigPaint.setTypeface(light);

        mConditionPaint.setColor(0xFFFFB340);
        mConditionPaint.setTypeface(regular);

        mGrayPaint.setColor(0xFF9AA0A6);
        mGrayPaint.setTypeface(regular);

        mBluePaint.setColor(0xFF4FA8FF);
        mBluePaint.setTypeface(regular);

        mBorderPaint.setColor(0xFF3A414A);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(2f);

        mButtonPaint.setColor(0xFF232830);

        mButtonTextPaint.setColor(0xFFF2F3F5);
        mButtonTextPaint.setTypeface(regular);
        mButtonTextPaint.setTextAlign(Paint.Align.CENTER);

        mAnimStart = System.currentTimeMillis();
        mHandler.post(mTick);
        mHandler.post(mAnimFrame);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP
                && getContext() instanceof Activity) {
            Activity a = (Activity) getContext();
            a.finish();
            a.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float s = w / 960f;
        WeatherData fresh = WeatherData.load(getContext());
        if (fresh.fetchedAt != mWeather.fetchedAt) mWeather = fresh;
        WeatherData d = mWeather;

        Wallpaper.draw(c, w, h);
        c.drawColor(0xA0000000);

        mGlowPaint.setShader(new RadialGradient(340f * s, 220f * s, 250f * s,
                0x2A1C1208, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, mGlowPaint);
        mGlowPaint.setShader(null);

        float t = Math.min(1f, (System.currentTimeMillis() - mAnimStart) / 600f);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        Paint layer = new Paint();
        layer.setAlpha((int) (255 * eased));
        int sc = c.saveLayer(0, 0, w, h, layer);
        c.translate(0, (1f - eased) * 24f * s);

        mTitlePaint.setTextSize(34f * s);
        c.drawText("Weather", 36f * s, 60f * s, mTitlePaint);

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        String time = String.format(Locale.US, "%d:%02d %s", hour,
                now.get(Calendar.MINUTE),
                now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");
        mTimePaint.setTextSize(30f * s);
        c.drawText(time, w - 36f * s, 58f * s, mTimePaint);

        float rotation = (System.currentTimeMillis() / 90f) % 360f;
        Icons.drawWeather(c, 100f * s, 180f * s, 2.4f * s, d.code, d.isDay, rotation);

        mBigPaint.setTextSize(96f * s);
        c.drawText(d.temp + "°", 190f * s, 205f * s, mBigPaint);

        mConditionPaint.setTextSize(42f * s);
        c.drawText(d.condition, 44f * s, 286f * s, mConditionPaint);

        mGrayPaint.setTextSize(32f * s);
        c.drawText("High " + d.high + "°    Low " + d.low + "°", 44f * s, 334f * s, mGrayPaint);

        mBluePaint.setTextSize(32f * s);
        c.drawText("Rain " + d.rain + "%", 44f * s, 382f * s, mBluePaint);

        int n = Math.min(5, d.days.length);
        float colW = 88f * s;
        float fx = w - 36f * s - n * colW;
        mBorderPaint.setStrokeWidth(2f * s);
        for (int i = 0; i < n; i++) {
            RectF r = new RectF(fx + i * colW, 90f * s,
                    fx + i * colW + 78f * s, 356f * s);
            c.drawRoundRect(r, 6f * s, 6f * s, mBorderPaint);
            float cx = r.centerX();

            mBluePaint.setTextSize(30f * s);
            mBluePaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(d.days[i], cx, 126f * s, mBluePaint);

            Icons.drawWeather(c, cx, 168f * s, 0.9f * s,
                    d.dayCodes[i], true, rotation + i * 24f);

            Paint centerWhite = new Paint(mBigPaint);
            centerWhite.setTextSize(34f * s);
            centerWhite.setTextAlign(Paint.Align.CENTER);
            c.drawText(d.dayHighs[i] + "°", cx, 250f * s, centerWhite);

            Paint centerGray = new Paint(mGrayPaint);
            centerGray.setTextSize(30f * s);
            centerGray.setTextAlign(Paint.Align.CENTER);
            c.drawText(d.dayLows[i] + "°", cx, 292f * s, centerGray);

            mBluePaint.setTextSize(26f * s);
            c.drawText(d.dayRains[i] + "%", cx, 332f * s, mBluePaint);
            mBluePaint.setTextAlign(Paint.Align.LEFT);
        }

        RectF rb = new RectF(w - 178f * s, h - 84f * s, w - 40f * s, h - 36f * s);
        c.drawRoundRect(rb, 8f * s, 8f * s, mButtonPaint);
        mButtonTextPaint.setTextSize(28f * s);
        c.drawText("Radar", rb.centerX(), rb.centerY() + 10f * s, mButtonTextPaint);

        c.restoreToCount(sc);
    }
}
