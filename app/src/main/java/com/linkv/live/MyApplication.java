package com.linkv.live;

import android.app.Application;

import com.linkv.live.utils.Constants;
import com.linkv.live.utils.SPUtils;
import com.linkv.rtc.BuildConfig;
import com.tencent.bugly.crashreport.CrashReport;

//import com.tencent.bugly.crashreport.CrashReport;

public class MyApplication extends Application {

    public static MyApplication instance;

    public MyApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            CrashReport.initCrashReport(getApplicationContext(), "8bf3b7425b", false);
        }
        SPUtils.init(getApplicationContext());
        setEnv();
    }

    private void setEnv() {
        Constants.setEnv(SPUtils.isTestEnv());
    }
}
