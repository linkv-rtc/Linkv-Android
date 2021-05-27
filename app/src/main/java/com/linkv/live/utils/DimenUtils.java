package com.linkv.live.utils;

import android.content.Context;
import android.graphics.Point;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.linkv.live.MyApplication;

import java.lang.reflect.Field;

public class DimenUtils {

    public final static float BASE_SCREEN_WIDH = 720f;
    public final static float BASE_SCREEN_HEIGHT = 1280f;
    public final static float BASE_SCREEN_DENSITY = 2f;
    private static final int DP_TO_PX = TypedValue.COMPLEX_UNIT_DIP;
    private static final int SP_TO_PX = TypedValue.COMPLEX_UNIT_SP;
    private static final int PX_TO_DP = TypedValue.COMPLEX_UNIT_MM + 1;
    private static final int PX_TO_SP = TypedValue.COMPLEX_UNIT_MM + 2;

    // -- dimens convert
    private static final int DP_TO_PX_SCALE_H = TypedValue.COMPLEX_UNIT_MM + 3;
    private static final int DP_SCALE_H = TypedValue.COMPLEX_UNIT_MM + 4;
    private static final int DP_TO_PX_SCALE_W = TypedValue.COMPLEX_UNIT_MM + 5;
    public static Float sScaleW, sScaleH;
    private static DisplayMetrics mMetrics = MyApplication.instance.getResources()
            .getDisplayMetrics();
    private static WindowManager wm = (WindowManager) MyApplication.instance.
            getSystemService(Context.WINDOW_SERVICE);
    private static int windowWidth = -1;
    private static int windowHeight = -1;

    private static float applyDimension(Context context, int unit, float value, DisplayMetrics metrics) {
        switch (unit) {
            case DP_TO_PX:
            case SP_TO_PX:
                return TypedValue.applyDimension(unit, value, metrics);
            case PX_TO_DP:
                return value / metrics.density;
            case PX_TO_SP:
                return value / metrics.scaledDensity;
            case DP_TO_PX_SCALE_H:
                return TypedValue.applyDimension(DP_TO_PX, value * getScaleFactorH(context), metrics);
            case DP_SCALE_H:
                return value * getScaleFactorH(context);
            case DP_TO_PX_SCALE_W:
                return TypedValue.applyDimension(DP_TO_PX, value * getScaleFactorW(context), metrics);
        }
        return 0;
    }

    public static int dp2px(Context context, float value) {
        return (int) applyDimension(context, DP_TO_PX, value, context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float value) {
        return (int) applyDimension(context, SP_TO_PX, value, context.getResources().getDisplayMetrics());
    }

    public static int px2dp(Context context, float value) {
        return (int) applyDimension(context, PX_TO_DP, value, context.getResources().getDisplayMetrics());
    }

    public static int px2sp(Context context, float value) {
        return (int) applyDimension(context, PX_TO_SP, value, context.getResources().getDisplayMetrics());
    }

    public static int dp2pxScaleW(Context context, float value) {
        return (int) applyDimension(context, DP_TO_PX_SCALE_W, value, context.getResources().getDisplayMetrics());
    }

    public static int dp2pxScaleH(Context context, float value) {
        return (int) applyDimension(context, DP_TO_PX_SCALE_H, value, context.getResources().getDisplayMetrics());
    }

    public static int dpScaleH(Context context, float value) {
        return (int) applyDimension(context, DP_SCALE_H, value, context.getResources().getDisplayMetrics());
    }

    public static float getScaleFactorW(Context context) {
        if (sScaleW == null) {
            sScaleW = (getScreenWidth(context) * BASE_SCREEN_DENSITY) / (getDensity(context) * BASE_SCREEN_WIDH);
        }
        return sScaleW;
    }

    public static float getScaleFactorH(Context context) {
        if (sScaleH == null) {
            sScaleH = (getScreenHeight(context) * BASE_SCREEN_DENSITY)
                    / (getDensity(context) * BASE_SCREEN_HEIGHT);
        }
        return sScaleH;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static int getWindowWidthForSMG_9500(Context activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getContentHeightForSMG_9500(Context activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    public static int dp2px(float value) {
        return (int) (0.5f + applyDimension(MyApplication.instance, DP_TO_PX, value, mMetrics));
    }

    public static int getWindowWidth() {
        try {
            if (windowWidth < 0) {
                Display display = wm.getDefaultDisplay();
                Point pp = new Point();
                display.getSize(pp);
                windowWidth = pp.x;
            }
            return windowWidth;
        } catch (Exception e) {

        }
        return mMetrics.widthPixels;
    }

    public static int getWindowHeight() {
        try {
            if (windowHeight < 0) {
                Display display = wm.getDefaultDisplay();
                Point pp = new Point();
                display.getSize(pp);
                windowHeight = pp.y;
            }
            return windowHeight;
        } catch (Exception e) {

        }
        return mMetrics.heightPixels;
    }

    public static int getContentHeight2() {
        int height = mMetrics.heightPixels - getStatusBarHeight2();
        return height;
    }

    public static int getStatusBarHeight2() {

        if (isXiaoMiNavigationGestureEnabled(MyApplication.instance)) {
            return 0;
        }
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = MyApplication.instance.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }

    //判断小米手机有没有开启全面屏手势
    public static boolean isXiaoMiNavigationGestureEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0;
    }

}
