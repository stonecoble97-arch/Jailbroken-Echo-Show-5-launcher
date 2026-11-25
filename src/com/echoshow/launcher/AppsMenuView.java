package com.echoshow.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Applications screen: real app icons, gray names, free drag scrolling with momentum. */
public class AppsMenuView extends View {
    private static final int TITLE_GRAY = 0xFFB8C2CC;
    private static final int LABEL_GRAY = 0xFF8A98A6;

    private final Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<AppInfo> mApps = new ArrayList<AppInfo>();
    private final List<RectF> mIconHits = new ArrayList<RectF>();
    private final List<Integer> mIconIndices = new ArrayList<Integer>();
    private final RectF mBackHit = new RectF();

    private final Handler mHandler = new Handler();
    private final Runnable mFling = new Runnable() {
        @Override public void run() {
            mScrollPx -= mVelocity * 16f;
            float maxPx = maxFirst() * stepPx();
            if (mScrollPx < 0f) { mScrollPx = 0f; mVelocity = 0f; }
            if (mScrollPx > maxPx) { mScrollPx = maxPx; mVelocity = 0f; }
            mVelocity *= 0.94f;
            invalidate();
            if (Math.abs(mVelocity) > 0.02f) mHandler.postDelayed(this, 16L);
        }
    };

    private NightOverlay mNightOverlay;
    private float mScrollPx;
    private float mVelocity;
    private float mDownX;
    private float mLastX;
    private long mLastTime;
    private boolean mDragging;
    private float mScale = 1f;

    private static class AppInfo {
        String label;
        Drawable icon;
        Intent intent;
    }

    public AppsMenuView(Context c) { super(c); init(c); }
    public AppsMenuView(Context c, AttributeSet a) { super(c, a); init(c); }

    private void init(Context c) {
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);

        mTitlePaint.setColor(TITLE_GRAY);
        mTitlePaint.setTypeface(light);
        mLabelPaint.setTypeface(regular);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);

        mNightOverlay = new NightOverlay(this);
        loadApps(c);
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

    private int visibleCount() { return 7; }
    private float stepPx() { return 132f * mScale; }
    private int maxFirst() { return Math.max(0, mApps.size() - visibleCount()); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        long t = e.getEventTime();
        int action = e.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN && mNightOverlay.isActive()) {
            NightMode.suppress(30000L);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            mHandler.removeCallbacks(mFling);
            mDownX = mLastX = x;
            mLastTime = t;
            mVelocity = 0f;
            mDragging = false;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - mLastX;
            long dt = Math.max(1L, t - mLastTime);
            mVelocity = dx / (float) dt;
            if (Math.abs(x - mDownX) > 8f) mDragging = true;
            mScrollPx = clampScroll(mScrollPx - dx);
            mLastX = x;
            mLastTime = t;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!mDragging) {
                handleTap(x, e.getY());
            } else {
                mHandler.post(mFling);
            }
            return true;
        }
        return true;
    }

    private float clampScroll(float v) {
        return Math.max(0f, Math.min(maxFirst() * stepPx(), v));
    }

    private void handleTap(float x, float y) {
        if (mBackHit.contains(x, y)) {
            if (getContext() instanceof Activity) ((Activity) getContext()).finish();
            return;
        }
        for (int i = 0; i < mIconHits.size(); i++) {
            if (mIconHits.get(i).contains(x, y)) {
                int idx = mIconIndices.get(i);
                if (idx >= 0 && idx < mApps.size()) {
                    getContext().startActivity(mApps.get(idx).intent);
                    if (getContext() instanceof Activity) {
                        ((Activity) getContext()).overridePendingTransition(
                                android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
                return;
            }
        }
    }

    private static String clip(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max - 1) + "…" : s);
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

        Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
        back.setStyle(Paint.Style.STROKE);
        back.setStrokeWidth(4f * s);
        back.setColor(TITLE_GRAY);
        KftvIcons.chevron(c, 46f * s, 46f * s, 1.2f * s, back, true);
        mBackHit.set(16f * s, 16f * s, 82f * s, 78f * s);

        mTitlePaint.setTextSize(30f * s);
        c.drawText("Applications", 96f * s, 56f * s, mTitlePaint);

        float step = stepPx();
        float startX = 82f * s;
        float cy = h * 0.44f;
        float iconSize = 78f * s;

        mIconHits.clear();
        mIconIndices.clear();
        int first = Math.max(0, (int) (mScrollPx / step) - 1);
        for (int idx = first; idx < mApps.size(); idx++) {
            float cx = startX + idx * step - mScrollPx;
            if (cx < -iconSize || cx > w + iconSize) continue;

            AppInfo ai = mApps.get(idx);
            ai.icon.setBounds((int) (cx - iconSize / 2f), (int) (cy - iconSize / 2f),
                    (int) (cx + iconSize / 2f), (int) (cy + iconSize / 2f));
            ai.icon.draw(c);

            mLabelPaint.setTextSize(20f * s);
            mLabelPaint.setColor(LABEL_GRAY);
            c.drawText(clip(ai.label, 18), cx, cy + iconSize / 2f + 40f * s, mLabelPaint);

            mIconHits.add(new RectF(cx - step / 2f, cy - 70f * s,
                    cx + step / 2f, cy + iconSize / 2f + 52f * s));
            mIconIndices.add(idx);
        }
        mNightOverlay.draw(c, w, h, s);
    }
}
