package com.linkv.live.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.linkv.live.GlobalConfig;
import com.linkv.live.LivePresenter;
import com.linkv.live.R;
import com.linkv.live.utils.ClipboardUtils;
import com.linkv.live.utils.Constants;
import com.linkv.live.utils.SPUtils;
import com.linkv.rtc.LVRTCEngine;


public class SettingActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener,
        BaseActivity.ChangeLanguageListener {

    final static int COUNTS = 5;
    final static long DURATION = 3000;
    long[] mHits = new long[COUNTS];
    private TextView tv_app_env;
    private TextView tv_language;
    private TextView tv_sdk_version;
    private View ll_switch_type;
    private View ll_switch_env;
    private View ll_switch_texture;
    private EditText et_debug_server_url;
    private EditText et_room_id;
    private EditText et_uid;
    private Switch sw_debug_server_url;
    private View fl_debug_info;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
        addLanguageChangeListener(this);
    }

    private void initView() {
        View iv_back = findViewById(R.id.iv_back);
        View fl_app_env = findViewById(R.id.fl_app_env);
        View fl_app_language = findViewById(R.id.fl_app_language);
        View fl_app_about = findViewById(R.id.fl_app_about);
        View fl_texture = findViewById(R.id.fl_texture);
        fl_debug_info = findViewById(R.id.fl_debug_info);
        View fl_setting_head = findViewById(R.id.fl_setting_head);
        View view_env = findViewById(R.id.view_env);
        View view_language = findViewById(R.id.view_language);
        View view_texture = findViewById(R.id.view_texture);
        ll_switch_type = findViewById(R.id.ll_switch_language);
        ll_switch_env = findViewById(R.id.ll_switch_env);
        ll_switch_texture = findViewById(R.id.ll_switch_texture);
        TextView tv_app_version = findViewById(R.id.tv_app_version);
        TextView tv_chinese = findViewById(R.id.tv_chinese);
        TextView tv_english = findViewById(R.id.tv_english);
        TextView tv_online_env = findViewById(R.id.tv_online_env);
        TextView tv_test_env = findViewById(R.id.tv_test_env);
        TextView tv_texture_adjust = findViewById(R.id.tv_texture_adjust);
        TextView tv_texture_on = findViewById(R.id.tv_texture_on);
        TextView tv_texture_off = findViewById(R.id.tv_texture_off);
        et_uid = findViewById(R.id.et_uid);
        et_room_id = findViewById(R.id.et_room_id);
        et_debug_server_url = findViewById(R.id.et_debug_server_url);

        tv_sdk_version = findViewById(R.id.tv_sdk_version);
        tv_app_env = findViewById(R.id.tv_app_env);
        tv_language = findViewById(R.id.tv_language);
        tv_language = findViewById(R.id.tv_language);
        tv_language = findViewById(R.id.tv_language);
        tv_language = findViewById(R.id.tv_language);

        iv_back.setOnClickListener(this);
        fl_app_about.setOnClickListener(this);
        fl_app_env.setOnClickListener(this);
        fl_app_language.setOnClickListener(this);
        fl_texture.setOnClickListener(this);
        tv_english.setOnClickListener(this);
        tv_chinese.setOnClickListener(this);
        tv_online_env.setOnClickListener(this);
        tv_test_env.setOnClickListener(this);
        tv_texture_adjust.setOnClickListener(this);
        tv_texture_on.setOnClickListener(this);
        tv_texture_off.setOnClickListener(this);
        fl_setting_head.setOnClickListener(this);

        tv_sdk_version.setOnLongClickListener(this);

        view_env.setOnTouchListener((v, event) -> {
            ll_switch_env.setVisibility(View.GONE);
            return true;
        });
        view_language.setOnTouchListener((v, event) -> {
            ll_switch_type.setVisibility(View.GONE);
            return true;
        });
        view_texture.setOnTouchListener((v, event) -> {
            ll_switch_texture.setVisibility(View.GONE);
            return true;
        });
        et_debug_server_url.setText(SPUtils.getDebugServerIp());

        tv_app_version.setText(Constants.APP_VERSION);
        tv_sdk_version.setText(LVRTCEngine.buildVersion());
        tv_app_env.setText(SPUtils.isTestEnv() ? R.string.setting_test : R.string.setting_prudtion);
        int language = SPUtils.getAppLanguage(Constants.DEFAULT_INTERNATIONAL_ENV);
        tv_language.setText(language == ENV_INTERNATIONAL ? getString(R.string.app_type_international)
                : getString(R.string.app_type_china));

        et_uid.setText(SPUtils.getUid());
        et_room_id.setText(SPUtils.getRoomId());

        Switch v = findViewById(R.id.switch_3a_sw);
        v.setChecked(GlobalConfig.sw3AMode);
        v.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GlobalConfig.sw3AMode = isChecked;
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                updateDebugInfo();
                finish();
                break;
            case R.id.fl_app_env:
                ll_switch_env.setVisibility(View.VISIBLE);
                break;
            case R.id.fl_app_language:
                ll_switch_type.setVisibility(View.VISIBLE);
                break;
            case R.id.fl_app_about:
                goToWeb();
                break;
            case R.id.tv_chinese:
                changeAppType(ENV_CHINA);
                break;
            case R.id.tv_english:
                changeAppType(ENV_INTERNATIONAL);
                break;
            case R.id.tv_online_env:
                changeEnv(false);
                break;
            case R.id.tv_test_env:
                changeEnv(true);
                break;
            case R.id.fl_texture:
                changeTexture();
                break;

            case R.id.tv_texture_adjust:

                break;
            case R.id.tv_texture_on:

                break;
            case R.id.tv_texture_off:

                break;

            case R.id.fl_setting_head:
                checkHeadFastClick();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        updateDebugInfo();
    }

    private void updateDebugInfo() {
        String ip = et_debug_server_url.getText().toString().trim();
        SPUtils.setRoomId(et_room_id.getText().toString().trim());
        SPUtils.setUid(et_uid.getText().toString().trim());
        SPUtils.setDebugServerIp(ip);
        LivePresenter.getInstance(this).resetEngine();
    }

    private void checkHeadFastClick() {
        if (fl_debug_info.getVisibility() != View.VISIBLE) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
                mHits = new long[COUNTS];//重新初始化数组
                fl_debug_info.setVisibility(View.VISIBLE);
            }
        }
    }

    private void changeTexture() {
//        SPUtils.setTextureInput();
    }

    private void changeAppType(int appType) {
        changeLanguage(appType);
    }

    private void changeLanguage(int language) {
        switch (language) {
            case ENV_CHINA:
                tv_language.setText(getString(R.string.app_type_china));
                break;
            case ENV_INTERNATIONAL:
                tv_language.setText(getString(R.string.app_type_international));
                break;
        }
        SPUtils.setAppLanguage(language);
        ll_switch_type.setVisibility(View.GONE);

        dispatchLanguageChange(language);
    }

    private void changeEnv(boolean isTestEnv) {
        ll_switch_env.setVisibility(View.GONE);
        SPUtils.setTestEnv(false);
        tv_app_env.setText(isTestEnv ? R.string.setting_test : R.string.setting_prudtion);
        Constants.setEnv(isTestEnv);
        LivePresenter.getInstance(this).resetEngine();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.tv_sdk_version:
                if (!TextUtils.isEmpty(tv_sdk_version.getText())
                        && ClipboardUtils.copy(this, tv_sdk_version.getText().toString().trim())) {
                    Toast.makeText(this, R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return false;
    }

    private void goToWeb() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse("http://www.linkv.io");
        intent.setData(content_url);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (ll_switch_type.getVisibility() == View.VISIBLE) {
                ll_switch_type.setVisibility(View.GONE);
            } else if (ll_switch_env.getVisibility() == View.VISIBLE) {
                ll_switch_env.setVisibility(View.GONE);
            } else {
                return super.onKeyDown(keyCode, event);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
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
