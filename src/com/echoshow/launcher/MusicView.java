package com.echoshow.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Full music screen: artwork placeholder, transport controls, seek bar and track list. */
public class MusicView extends View implements MusicPlayer.Listener {
    private final Paint mHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNotePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mControlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSeekBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSeekFg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRowActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mBackHit = new RectF();
    private final RectF mPrevHit = new RectF();
    private final RectF mPlayHit = new RectF();
    private final RectF mNextHit = new RectF();
    private final RectF mSeekHit = new RectF();
    private final List<RectF> mRowHits = new ArrayList<RectF>();
    private final List<Integer> mRowIndices = new ArrayList<Integer>();

    private NightOverlay mNightOverlay;
    private final Handler mHandler = new Handler();
    private final Runnable mTick = new Runnable() {
        @Override public void run() { invalidate(); mHandler.postDelayed(this, 500L); }
    };

    private float mScale = 1f;
    private float mSeekX1;
    private float mSeekX2;
    private int mScroll = 0;
    private float mDownY;
    private float mLastY;
    private boolean mDragged;

    public MusicView(Context c) { super(c); init(); }
    public MusicView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        mNightOverlay = new NightOverlay(this);
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);

        mHeaderPaint.setColor(0xFFF2F3F5);
        mHeaderPaint.setTypeface(light);
        mHeaderPaint.setTextAlign(Paint.Align.CENTER);
        mTimePaint.setColor(0xFF9AA0A6);
        mTimePaint.setTypeface(regular);
        mTimePaint.setTextAlign(Paint.Align.RIGHT);
        mTitlePaint.setColor(0xFFFFFFFF);
        mTitlePaint.setTypeface(light);
        mSubPaint.setColor(0xFF9AA0A6);
        mSubPaint.setTypeface(regular);
        mArtPaint.setColor(0xFF232A33);
        mNotePaint.setColor(0xFFFFB340);
        mControlPaint.setColor(0xFFF2F3F5);
        mControlPaint.setStyle(Paint.Style.FILL);
        mSeekBg.setColor(0xFF3A4048);
        mSeekBg.setStrokeWidth(4f);
        mSeekFg.setColor(0xFFFFB340);
        mSeekFg.setStrokeWidth(4f);
        mRowPaint.setColor(0x00000000);
        mRowActivePaint.setColor(0xFF243040);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MusicPlayer.addListener(this);
        mHandler.post(mTick);
    }

    @Override protected void onDetachedFromWindow() {
        MusicPlayer.removeListener(this);
        mHandler.removeCallbacks(mTick);
        super.onDetachedFromWindow();
    }

    @Override public void onChanged() { invalidate(); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && mNightOverlay.isActive()) {
            NightMode.suppress(30000L);
        }
        float x = e.getX();
        float y = e.getY();
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownY = mLastY = y;
            mDragged = false;
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dy = y - mLastY;
            mLastY = y;
            if (Math.abs(y - mDownY) > 12f) mDragged = true;
            if (mDragged) {
                mScroll += (int) (-dy / (38f * mScale));
                int max = Math.max(0, MusicPlayer.tracks().size() - 4);
                mScroll = Math.max(0, Math.min(max, mScroll));
                invalidate();
            }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP && !mDragged) handleTap(x, y);
        return true;
    }

    private void handleTap(float x, float y) {
        if (mBackHit.contains(x, y)) {
            if (getContext() instanceof Activity) ((Activity) getContext()).finish();
            return;
        }
        if (mPrevHit.contains(x, y)) { MusicPlayer.prev(); return; }
        if (mPlayHit.contains(x, y)) { MusicPlayer.toggle(); return; }
        if (mNextHit.contains(x, y)) { MusicPlayer.next(); return; }
        if (mSeekHit.contains(x, y) && MusicPlayer.duration() > 0) {
            float frac = (x - mSeekX1) / (mSeekX2 - mSeekX1);
            MusicPlayer.seek((int) (Math.max(0f, Math.min(1f, frac)) * MusicPlayer.duration()));
            return;
        }
        for (int i = 0; i < mRowHits.size(); i++) {
            if (mRowHits.get(i).contains(x, y)) {
                MusicPlayer.play(mRowIndices.get(i));
                return;
            }
        }
    }

    private static String clip(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max - 1) + "…" : s);
    }

    private static String time(int ms) {
        int sec = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
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
        c.drawColor(0x99000000);

        // Header
        mHeaderPaint.setTextSize(30f * s);
        c.drawText("Music", w / 2f, 52f * s, mHeaderPaint);
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        mTimePaint.setTextSize(26f * s);
        c.drawText(String.format(Locale.US, "%d:%02d %s", hour, now.get(Calendar.MINUTE),
                now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM"),
                w - 36f * s, 50f * s, mTimePaint);

        Path back = new Path();
        back.moveTo(52f * s, 40f * s);
        back.lineTo(37f * s, 52f * s);
        back.lineTo(52f * s, 64f * s);
        mNotePaint.setColor(0xFFF2F3F5);
        mNotePaint.setStrokeWidth(4f * s);
        mNotePaint.setStyle(Paint.Style.STROKE);
        c.drawPath(back, mNotePaint);
        mNotePaint.setStyle(Paint.Style.FILL);
        mBackHit.set(20f * s, 24f * s, 72f * s, 76f * s);

        // Artwork placeholder
        RectF art = new RectF(36f * s, 82f * s, 166f * s, 212f * s);
        c.drawRoundRect(art, 12f * s, 12f * s, mArtPaint);
        mNotePaint.setColor(0xFFFFB340);
        c.drawCircle(art.centerX() - 6f * s, art.centerY() - 16f * s, 16f * s, mNotePaint);
        c.drawRect(art.centerX() + 6f * s, art.centerY() - 46f * s,
                art.centerX() + 13f * s, art.centerY() - 2f * s, mNotePaint);
        c.drawCircle(art.centerX() - 10f * s, art.centerY() + 22f * s, 12f * s, mNotePaint);

        MusicPlayer.Track t = MusicPlayer.current();
        String title = t == null ? "Nothing playing" : t.title;
        String artist = t == null ? "Choose a station below"
                : (t.radio ? "Radio · " + t.artist : t.artist);
        mTitlePaint.setTextSize(34f * s);
        mSubPaint.setTextSize(24f * s);
        c.drawText(clip(title, 22), 186f * s, 132f * s, mTitlePaint);
        c.drawText(clip(artist, 30), 186f * s, 170f * s, mSubPaint);

        // Seek bar
        mSeekX1 = 186f * s;
        mSeekX2 = w - 36f * s;
        float py = 205f * s;
        mSeekBg.setStrokeWidth(4f * s);
        mSeekFg.setStrokeWidth(5f * s);
        c.drawLine(mSeekX1, py, mSeekX2, py, mSeekBg);
        int dur = MusicPlayer.duration();
        if (dur > 0) {
            float frac = (float) MusicPlayer.position() / dur;
            c.drawLine(mSeekX1, py, mSeekX1 + (mSeekX2 - mSeekX1) * frac, py, mSeekFg);
        }
        mSubPaint.setTextSize(18f * s);
        c.drawText(time(MusicPlayer.position()), mSeekX1, py + 26f * s, mSubPaint);
        mTimePaint.setTextSize(18f * s);
        c.drawText(dur > 0 ? time(dur) : "--:--", mSeekX2, py + 26f * s, mTimePaint);
        mSeekHit.set(mSeekX1 - 12f * s, py - 18f * s, mSeekX2 + 12f * s, py + 18f * s);

        // Transport controls
        float cy = 278f * s;
        float r = 38f * s;
        float prevX = 400f * s;
        float playX = 480f * s;
        float nextX = 560f * s;
        mControlPaint.setColor(0xFFF2F3F5);
        // previous
        c.drawRect(prevX - 18f * s, cy - 16f * s, prevX - 13f * s, cy + 16f * s, mControlPaint);
        Path left = new Path();
        left.moveTo(prevX + 14f * s, cy - 16f * s);
        left.lineTo(prevX - 8f * s, cy);
        left.lineTo(prevX + 14f * s, cy + 16f * s);
        left.close();
        c.drawPath(left, mControlPaint);
        // play / pause
        if (MusicPlayer.isPlaying()) {
            c.drawRect(playX - 14f * s, cy - 18f * s, playX - 5f * s, cy + 18f * s, mControlPaint);
            c.drawRect(playX + 5f * s, cy - 18f * s, playX + 14f * s, cy + 18f * s, mControlPaint);
        } else {
            Path play = new Path();
            play.moveTo(playX - 12f * s, cy - 18f * s);
            play.lineTo(playX + 16f * s, cy);
            play.lineTo(playX - 12f * s, cy + 18f * s);
            play.close();
            c.drawPath(play, mControlPaint);
        }
        // next
        Path right = new Path();
        right.moveTo(nextX - 14f * s, cy - 16f * s);
        right.lineTo(nextX + 8f * s, cy);
        right.lineTo(nextX - 14f * s, cy + 16f * s);
        right.close();
        c.drawPath(right, mControlPaint);
        c.drawRect(nextX + 13f * s, cy - 16f * s, nextX + 18f * s, cy + 16f * s, mControlPaint);

        mPrevHit.set(prevX - r, cy - r, prevX + r, cy + r);
        mPlayHit.set(playX - r, cy - r, playX + r, cy + r);
        mNextHit.set(nextX - r, cy - r, nextX + r, cy + r);

        // Track list
        List<MusicPlayer.Track> tracks = MusicPlayer.tracks();
        float listTop = 328f * s;
        float rowH = 38f * s;
        int maxRows = Math.max(1, (int) ((476f * s - listTop) / rowH));
        mRowHits.clear();
        mRowIndices.clear();
        for (int i = 0; i < maxRows && mScroll + i < tracks.size(); i++) {
            int idx = mScroll + i;
            RectF row = new RectF(24f * s, listTop + i * rowH,
                    w - 24f * s, listTop + (i + 1) * rowH - 4f * s);
            c.drawRoundRect(row, 8f * s, 8f * s,
                    idx == MusicPlayer.index() ? mRowActivePaint : mRowPaint);
            mTitlePaint.setTextSize(22f * s);
            mTitlePaint.setTextAlign(Paint.Align.LEFT);
            mTitlePaint.setColor(idx == MusicPlayer.index() ? 0xFFFFB340 : 0xFFE8EAED);
            c.drawText(clip(tracks.get(idx).title, 34), 40f * s,
                    row.top + 26f * s, mTitlePaint);
            mSubPaint.setTextSize(18f * s);
            mSubPaint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(clip(tracks.get(idx).artist, 22), w - 40f * s,
                    row.top + 26f * s, mSubPaint);
            mSubPaint.setTextAlign(Paint.Align.LEFT);
            mRowHits.add(row);
            mRowIndices.add(idx);
        }
        mNightOverlay.draw(c, w, h, s);
    }
}
