package com.linkv.live.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {

    private static SharedPreferences mSPInstance;

    public static void init(Context context) {
        mSPInstance = context.getSharedPreferences("LiveMeSDK", Context.MODE_PRIVATE);
    }

    public static int getInt(String key, int defaultValue) {
        return mSPInstance.getInt(key, defaultValue);
    }

    public static void setInt(String key, int value) {
        mSPInstance.edit().putInt(key, value).apply();
    }

    public static String getString(String key) {
        return mSPInstance.getString(key, null);
    }

    public static void setString(String key, String value) {
        mSPInstance.edit().putString(key, value).apply();
    }

    public static boolean getBoolean(String key) {
        return mSPInstance.getBoolean(key, false);
    }

    public static void setBoolean(String key, boolean value) {
        mSPInstance.edit().putBoolean(key, value).apply();
    }

    public static boolean isTestEnv() {
        return getBoolean("isTestEnv");
    }

    public static void setTestEnv(boolean value) {
        setBoolean("isTestEnv", value);
    }

    public static void setAppLanguage(int value) {
        setInt("appLanguage", value);
    }

    public static int getAppLanguage(int defaultValue) {
        return getInt("appLanguage", defaultValue);
    }

    public static void setTextureInput(int value) {
        setInt("textureInput", value);
    }

    public static int getTextureInput(int defaultValue) {
        return getInt("textureInput", defaultValue);
    }

    public static String getDebugServerIp() {
        return getString("debug_server_url");
    }

    public static void setDebugServerIp(String url) {
        setString("debug_server_url", url);
    }

    public static String getRoomId() {
        return getString("room_id");
    }

    public static void setRoomId(String roomId) {
        setString("room_id", roomId);
    }

    public static String getUid() {
        return getString("uid");
    }

    public static void setUid(String uid) {
        setString("uid", uid);
    }
}
