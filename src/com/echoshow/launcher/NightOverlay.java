package com.echoshow.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/** Fades a pure-black red clock over any screen when the room is completely dark. */
public class NightOverlay implements NightMode.Listener {
    private final View mHost;
    private final Handler mHandler = new Handler();
    private final Paint mBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTime = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDate = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mAlpha;
    private boolean mTarget;
    private final Runnable mStep = new Runnable() {
        @Override public void run() {
            float delta = (mTarget ? 1f : 0f) - mAlpha;
            if (Math.abs(delta) < 0.01f) {
                mAlpha = mTarget ? 1f : 0f;
            } else {
                mAlpha += delta * 0.18f;
            }
            mHost.invalidate();
            if (mAlpha != (mTarget ? 1f : 0f)) mHandler.postDelayed(this, 16L);
        }
    };

    public NightOverlay(View host) {
        mHost = host;
        mBlack.setColor(0xFF000000);
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        mTime.setTypeface(light);
        mTime.setTextAlign(Paint.Align.LEFT);
        mDate.setTypeface(light);
        mDate.setTextAlign(Paint.Align.CENTER);
        mTarget = NightMode.isNight();
        mAlpha = mTarget ? 1f : 0f;
        NightMode.addListener(this);
    }

    public void destroy() {
        NightMode.removeListener(this);
        mHandler.removeCallbacks(mStep);
    }

    public boolean isActive() { return mTarget && mAlpha > 0.01f; }

    @Override public void onNightChanged(boolean night) {
        mTarget = night;
        mHandler.removeCallbacks(mStep);
        mHandler.post(mStep);
    }

    public void draw(Canvas c, int w, int h, float s) {
        if (mAlpha <= 0.001f) return;
        int a = (int) (255 * mAlpha);
        mBlack.setAlpha(a);
        c.drawRect(0, 0, w, h, mBlack);

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        String time = String.format(Locale.US, "%d:%02d", hour, now.get(Calendar.MINUTE));
        String ampm = now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

        // Match the home clock exactly so the crossfade has no jump in font or position.
        mTime.setTextSize(196f * s);
        float tw = mTime.measureText(time);
        mTime.setTextSize(54f * s);
        float aw = mTime.measureText(ampm);
        float total = tw + 12f * s + aw;
        float startX = (w - total) / 2f;

        mTime.setTextSize(196f * s);
        mTime.setColor(0xFFFF2E2E);
        mTime.setAlpha(a);
        float centerY = h * 0.42f;
        float baseline = centerY - (mTime.getFontMetrics().ascent + mTime.getFontMetrics().descent) / 2f;
        c.drawText(time, startX, baseline, mTime);
        mTime.setTextSize(54f * s);
        c.drawText(ampm, startX + tw + 12f * s, baseline - 2f * s, mTime);

        mDate.setTextSize(30f * s);
        mDate.setColor(0xFFB02020);
        mDate.setAlpha(a);
        c.drawText(DateFormat.format("EEEE, MMMM d", now).toString(),
                w / 2f, baseline + 126f * s, mDate);
    }
}
