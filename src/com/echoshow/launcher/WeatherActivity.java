package com.echoshow.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class WeatherActivity extends Activity {
    private AutoBrightness mAutoBrightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAutoBrightness = new AutoBrightness(this);
        setContentView(new WeatherView(this));
    }

    @Override
    protected void onPause() {
        mAutoBrightness.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAutoBrightness.start();
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
