package com.linkv.live.utils;


import com.linkv.live.activity.BaseActivity;
import com.linkv.live.network.NetManager;
import com.linkv.rtc.LVRTCEngine;

public class Constants {
    public static final String APP_VERSION = "1.0.0";
    // 控制国际和国内语言以及网络环境
    public static final int DEFAULT_INTERNATIONAL_ENV = BaseActivity.ENV_CHINA;
    public static String AppId;
    public static String AppSign;
    public static String AppIdSec;
    public static String AppSignSec;
    public static boolean IsTestEnv;

    public static void setEnv(boolean isTestEnv) {
        IsTestEnv = isTestEnv;
        SPUtils.setTestEnv(isTestEnv);
        NetManager.setUseTestEnv(Constants.IsTestEnv);
        LVRTCEngine.setUseTestEnv(Constants.IsTestEnv);
    }
}
