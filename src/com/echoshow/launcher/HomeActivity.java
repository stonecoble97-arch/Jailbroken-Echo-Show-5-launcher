package com.echoshow.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

public class HomeActivity extends Activity {
    private HomeView mView;
    private AutoBrightness mAutoBrightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeatherClient.refresh(this);
        MusicPlayer.init(this);
        mAutoBrightness = new AutoBrightness(this);
        mView = new HomeView(this);
        mView.setAutoBrightness(mAutoBrightness);
        setContentView(mView);
        hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAutoBrightness.start();
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        mAutoBrightness.stop();
        super.onPause();
    }

    /** Opens the music screen, asking for library access on first use. */
    public void openMusic() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            return;
        }
        launchMusic();
    }

    private void launchMusic() {
        startActivity(new Intent(this, MusicActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            MusicPlayer.loadLocal();
            launchMusic();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onBackPressed() {
        if (mView != null && mView.closeDrawer()) return;
        // A launcher stays on the home screen when Back is pressed.
    }
}
