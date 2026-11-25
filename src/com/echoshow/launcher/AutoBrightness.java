package com.echoshow.launcher;

import android.app.Activity;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;

/** Ambient-light brightness with a near-darkness night mode and manual-override detection. */
public class AutoBrightness implements SensorEventListener {
    public interface NightListener { void onNightChanged(boolean night); }
    public interface ManualListener { void onManualOverride(); }

    private final Activity mActivity;
    private final SensorManager mSensorManager;
    private final Sensor mLight;
    private final Handler mHandler = new Handler();

    private boolean mEnabled = true;
    private boolean mRegistered = false;
    private boolean mNight = false;
    private float mTarget = -1f;
    private float mCurrent = -1f;

    private NightListener mNightListener;
    private ManualListener mManualListener;

    private final ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override public void onChange(boolean selfChange) {
            if (selfChange || !mEnabled) return;
            // The user changed brightness in system settings — stop overriding it.
            mEnabled = false;
            mHandler.removeCallbacks(mStep);
            if (mManualListener != null) mManualListener.onManualOverride();
        }
    };

    private final Runnable mStep = new Runnable() {
        @Override public void run() {
            if (!mEnabled || mTarget < 0f) return;
            if (mCurrent < 0f) mCurrent = mTarget;
            float delta = mTarget - mCurrent;
            if (Math.abs(delta) < 0.01f) {
                mCurrent = mTarget;
                apply(mCurrent);
                return;
            }
            mCurrent += delta * 0.25f;
            apply(mCurrent);
            mHandler.postDelayed(this, 40L);
        }
    };

    public AutoBrightness(Activity activity) {
        mActivity = activity;
        mSensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
        mLight = mSensorManager == null ? null : mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public boolean isAvailable() { return mLight != null; }
    public boolean isEnabled() { return mEnabled; }
    public boolean isNight() { return mNight; }

    public void setNightListener(NightListener l) { mNightListener = l; }
    public void setManualListener(ManualListener l) { mManualListener = l; }

    public void start() {
        if (mLight == null || mRegistered) return;
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mActivity.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false, mBrightnessObserver);
        mRegistered = true;
    }

    public void stop() {
        if (mRegistered) {
            mSensorManager.unregisterListener(this);
            mActivity.getContentResolver().unregisterContentObserver(mBrightnessObserver);
            mRegistered = false;
        }
        mHandler.removeCallbacks(mStep);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!enabled) {
            mHandler.removeCallbacks(mStep);
            mTarget = -1f;
            mCurrent = -1f;
            apply(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
        } else if (mTarget >= 0f) {
            mCurrent = -1f;
            mHandler.post(mStep);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        float lux = Math.max(0f, event.values[0]);

        boolean night = mNight;
        if (!night && lux < 0.8f) night = true;
        else if (night && lux > 1.5f) night = false;
        if (night != mNight) {
            mNight = night;
            NightMode.set(night);
            if (mNightListener != null) mNightListener.onNightChanged(night);
        }

        if (!mEnabled) return;

        float brightness;
        if (mNight) {
            brightness = 0.01f;
        } else {
            float l = Math.max(1f, lux);
            brightness = 0.10f + 0.75f * (float) (Math.log10(l) / 4.0);
            brightness = Math.max(0.08f, Math.min(1.0f, brightness));
        }
        if (mTarget >= 0f && Math.abs(brightness - mTarget) < 0.02f) return;
        mTarget = brightness;
        mHandler.removeCallbacks(mStep);
        mHandler.post(mStep);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void apply(float value) {
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
        lp.screenBrightness = value;
        mActivity.getWindow().setAttributes(lp);
    }
}
