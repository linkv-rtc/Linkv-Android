package com.linkv.live.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BaseActivity extends Activity {

    public static final int ENV_CHINA = 0;
    public static final int ENV_INTERNATIONAL = 1;

    public static final Object mLanguageLock = new Object();
    public static final List<ChangeLanguageListener> mLanguageChangeListeners = new ArrayList<>();

    public void changeAppLanguage(int language) {
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

    public void addLanguageChangeListener(ChangeLanguageListener languageListener) {
        synchronized (mLanguageLock) {
            mLanguageChangeListeners.add(languageListener);
        }
    }

    public void removeLanguageChangeListener(ChangeLanguageListener languageListener) {
        synchronized (mLanguageLock) {
            mLanguageChangeListeners.remove(languageListener);
        }
    }

    public void dispatchLanguageChange(int language) {
        synchronized (mLanguageLock) {
            for (ChangeLanguageListener listener : mLanguageChangeListeners) {
                changeAppLanguage(language);
                listener.onLanguageChanged(language);
            }
        }
    }

    public interface ChangeLanguageListener {
        void onLanguageChanged(int language);
    }

}
