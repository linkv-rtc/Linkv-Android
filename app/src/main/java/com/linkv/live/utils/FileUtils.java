package com.linkv.live.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    static final String TAG = "FileUtils";

    public static void saveBitmap(Bitmap bitmap, ILMSaveBitmapCallback callback) {
        if (callback == null) {
            return;
        }
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            savePath = "/sdcard";
        } else {
            Log.d(TAG, "saveBitmap failed.  no sdcard.");
            callback.onSaveResult(false);
            return;
        }
        try {
            filePic = new File(savePath, "SnapShot_" + System.currentTimeMillis() + ".png");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "saveBitmap failed. e: " + e.getMessage());
            callback.onSaveResult(false);
            return;
        }
        Log.d(TAG, "saveBitmap: " + filePic.getAbsolutePath());
        callback.onSaveResult(true);
    }

    public interface ILMSaveBitmapCallback {
        void onSaveResult(boolean success);
    }

}
