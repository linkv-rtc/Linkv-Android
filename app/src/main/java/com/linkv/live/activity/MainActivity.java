package com.linkv.live.activity;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.linkv.live.LivePresenter;
import com.linkv.live.R;
import com.linkv.live.utils.Constants;
import com.linkv.live.utils.SPUtils;

import java.util.Locale;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends BaseActivity implements View.OnClickListener, BaseActivity.ChangeLanguageListener {

    private View ll_switch_live;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLanguage();
        setContentView(R.layout.activity_main);
        initView();
        initEngine();
        addLanguageChangeListener(this);
    }

    private void initEngine() {
        LivePresenter.getInstance(this).initEngine(false);
    }

    private void initView() {
        View tv_setting = findViewById(R.id.tv_setting);
        ll_switch_live = findViewById(R.id.ll_switch_live);
        View btn_play_live = findViewById(R.id.btn_play_live);
        View btn_watch_live = findViewById(R.id.btn_watch_live);
        View view_switch_live = findViewById(R.id.view_switch_live);
        View tv_common_live = findViewById(R.id.tv_common_live);
        View tv_meeting_live = findViewById(R.id.tv_meeting_live);
        View tv_audio_live = findViewById(R.id.tv_audio_live);
        View tv_screen_record = findViewById(R.id.tv_screen_sharing);

        btn_play_live.setOnClickListener(this);
        btn_watch_live.setOnClickListener(this);
        tv_common_live.setOnClickListener(this);
        tv_meeting_live.setOnClickListener(this);
        tv_audio_live.setOnClickListener(this);
        tv_screen_record.setOnClickListener(this);
        tv_setting.setOnClickListener(this);

        view_switch_live.setOnTouchListener((v, event) -> {
            ll_switch_live.setVisibility(View.GONE);
            return true;
        });

    }

    private boolean checkMyPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
            return false;
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            checkMyPermission();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play_live:
                if (!checkMyPermission()) {
                    return;
                }
                ll_switch_live.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_watch_live:
                goToWatch();
                break;
            case R.id.tv_common_live:
                goToLive(LiveActivity.COMMON_LIVE);
                break;
            case R.id.tv_meeting_live:
                goToLive(LiveActivity.MEETING_LIVE);
                break;
            case R.id.tv_audio_live:
                goToLive(LiveActivity.AUDIO_LIVE);
                break;
            case R.id.tv_screen_sharing:
                goToLive(LiveActivity.SCREEN_SHARING_LIVE);
                break;
            case R.id.tv_setting:
                goToSetting();
                break;
        }
    }


    public void goToLive(int type) {
        Intent intent = new Intent(this, LiveActivity.class);
        intent.putExtra(LiveActivity.KEY_ENTER_FROM, LiveActivity.FROM_LIVE);
        intent.putExtra(LiveActivity.KEY_LIVE_TYPE, type);
        startActivity(intent);
        ll_switch_live.setVisibility(View.GONE);
    }

    public void goToWatch() {
        if (!checkMyPermission()) {
            return;
        }
        startActivity(new Intent(this, RoomListActivity.class));
        ll_switch_live.setVisibility(View.GONE);
    }

    private void goToSetting() {
        startActivity(new Intent(this, SettingActivity.class));
    }

    private void setLanguage() {
        int language = SPUtils.getAppLanguage(Constants.DEFAULT_INTERNATIONAL_ENV);
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        Locale locale = null;
        switch (language) {
            case ENV_CHINA:
                locale = Locale.CHINESE;
                break;
            case ENV_INTERNATIONAL:
                locale = Locale.US;
                break;
        }
        if (locale != null) {
            config.setLocale(locale);
            resources.updateConfiguration(config, dm);
        }
    }

    @Override
    public void onLanguageChanged(int language) {
        recreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeLanguageChangeListener(this);
    }
}
