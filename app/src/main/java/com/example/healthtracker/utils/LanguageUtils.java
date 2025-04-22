package com.example.healthtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

public class LanguageUtils {
    private static final String TAG = "LanguageUtils";
    private static final String PREF_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "language";

    /**
     * Set application language
     * @param context The application context
     * @param languageCode Language code (en, vi, id)
     */
    public static void setLocale(Context context, String languageCode) {
        Log.d(TAG, "Setting locale to: " + languageCode);
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config);
        }
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        
        // Save selected language
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();
        
        Log.d(TAG, "Locale set successfully. Current locale: " + Locale.getDefault().getLanguage());
    }

    /**
     * Get saved language code
     * @param context The application context
     * @return Language code (en, vi, id) or default "en"
     */
    public static String getSavedLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String language = preferences.getString(LANGUAGE_KEY, "en");
        Log.d(TAG, "Retrieved saved language: " + language);
        return language;
    }

    /**
     * Apply saved language to context
     * @param context The context to apply language to
     * @return Context with applied language
     */
    public static Context applyLanguage(Context context) {
        String languageCode = getSavedLanguage(context);
        setLocale(context, languageCode);
        return context;
    }
} 