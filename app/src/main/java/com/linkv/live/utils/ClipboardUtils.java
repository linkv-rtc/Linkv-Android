package com.linkv.live.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

public class ClipboardUtils {

    public static boolean copy(Context context, String text) {
        if (context == null || TextUtils.isEmpty(text)) {
            return false;
        }
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", text);
            cm.setPrimaryClip(mClipData);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String pase(Context context) {
        if (context == null) {
            return "";
        }
        StringBuilder resultString = new StringBuilder();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip()) {
            ClipData clipData = cm.getPrimaryClip();
            if (clipData != null) {
                int count = clipData.getItemCount();
                for (int i = 0; i < count; ++i) {
                    ClipData.Item item = clipData.getItemAt(i);
                    CharSequence str = item.coerceToText(context);
                    resultString.append(str);
                }
            }
        }
        return resultString.toString();
    }
}
