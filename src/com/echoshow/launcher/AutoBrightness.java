package com.echoshow.launcher;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.WindowManager;

/** Uses the ambient light sensor to smoothly drive this window's brightness. */
public class AutoBrightness implements SensorEventListener {
    private final Activity mActivity;
    private final SensorManager mSensorManager;
    private final Sensor mLight;
    private final Handler mHandler = new Handler();

    private boolean mEnabled = true;
    private boolean mRegistered = false;
    private float mTarget = -1f;
    private float mCurrent = -1f;

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

    public void start() {
        if (mLight == null || mRegistered) return;
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mRegistered = true;
    }

    public void stop() {
        if (mRegistered) {
            mSensorManager.unregisterListener(this);
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
        if (!mEnabled || event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        float lux = Math.max(1f, event.values[0]);
        float brightness = 0.10f + 0.75f * (float) (Math.log10(lux) / 4.0);
        brightness = Math.max(0.08f, Math.min(1.0f, brightness));
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
