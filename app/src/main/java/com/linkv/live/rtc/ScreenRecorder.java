package com.linkv.live.rtc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;

import com.linkv.live.utils.DimenUtils;
import com.linkv.rtc.internal.base.Frame;
import com.linkv.rtc.internal.base.I420Reader;

public class ScreenRecorder implements Frame.Listener {

    private static final String TAG = "ScreenRecorder";
    private int mWidth;
    private int mHeight;

    private I420Reader mReader;
    private ScreenRecorderCallback mCallback;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;

    public ScreenRecorder(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public static boolean isSupportRecord() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean checkPermission(Activity activity, int requestCode) {
        if (!isSupportRecord() || activity == null) {
            return false;
        }
        return checkProjectionPermission(activity, requestCode);
    }

    private static boolean checkProjectionPermission(Activity activity, int requestCode) {
        if (isSupportRecord()) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager != null) {
                Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    activity.startActivityForResult(intent, requestCode);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private void initReader() {
        configureVideoSize();
        releaseReader();
        mReader = new I420Reader(mWidth, mHeight);
        mReader.addFrameListener(this);
    }

    private void releaseReader() {
        if (mReader != null) {
            mReader.stop();
        }
    }

    public void startRecord(Activity activity, int resultCode, Intent resultData) {
        if (!isSupportRecord()) {
            return;
        }
        initReader();
        mReader.start();
        setupProjection(activity, resultCode, resultData);
    }

    public void stopRecord() {
        releaseReader();
        if (isSupportRecord()) {
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        }
    }

    private void setupProjection(Activity activity, int resultCode, Intent resultData) {
        if (isSupportRecord()) {
            MediaProjectionManager manager = (MediaProjectionManager) activity.
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            DisplayManager dm = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
            Display defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (defaultDisplay == null) {
                Log.e(TAG, "Record Error: Display is null!");
                releaseReader();
                mCallback.onRecordFailed();
                return;
            }

            mMediaProjection = manager.getMediaProjection(resultCode, resultData);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror", mWidth, mHeight, (int) DimenUtils.getDensity(activity),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mReader.getSurface(), new VirtualDisplay.Callback() {
                        @Override
                        public void onPaused() {
                            Log.i(TAG, "onPaused: ");
                            super.onPaused();
                        }

                        @Override
                        public void onResumed() {
                            Log.i(TAG, "onResumed: ");
                            super.onResumed();
                        }

                        @Override
                        public void onStopped() {
                            Log.i(TAG, "onStopped: ");
                            super.onStopped();
                            if (mCallback != null) {
                                mCallback.onStopped();
                            }
                        }
                    }, null);
        }
    }

    private void configureVideoSize() {
        Log.d(TAG, "Before mWidth:" + mWidth + "   mHeight:" + mHeight);

        //保证配置的尺寸是16倍数
        if (mWidth % 16 != 0) {
            mWidth = 16 * (mWidth / 16);
        }
        if (mHeight % 16 != 0) {
            mHeight = 16 * (mHeight / 16);
        }

        if (mCallback != null) {
            mCallback.updateVideoSize(mWidth, mHeight);
        }

        Log.d(TAG, "After mWidth:" + mWidth + "   mHeight:" + mHeight);
    }


    @Override
    public void onNewFrame(Frame frame) {
        if (mCallback != null) {
            mCallback.onDataUpdate(frame);
        }
    }

    public void setScreenRecorderCallback(ScreenRecorderCallback callback) {
        mCallback = callback;
    }

    public interface ScreenRecorderCallback {
        void onDataUpdate(Frame frame);

        void onStopped();

        void onRecordFailed();

        void updateVideoSize(int width, int height);
    }
}
