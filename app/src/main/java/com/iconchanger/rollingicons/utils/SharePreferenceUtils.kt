package com.iconchanger.rollingicons.utils

import android.content.Context
import com.mobi.libraryads.FOConfigs


object SharePreferenceUtils {
    private const val PREF_NAME = "rolling_icons_prefs"
    private const val KEY_LANGUAGE = "selected_language"


    private const val KEY_IS_ORGANIC = "is_organic"

    fun isOrganic(context: Context?): Boolean {
        if (FOConfigs.isOrganic) {
            setOrganic(context, true)
            return true
        }
        if (context == null) return false
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        return false;
        return prefs.getBoolean(KEY_IS_ORGANIC, false)
    }

    fun setOrganic(context: Context?, isOrganic: Boolean) {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_ORGANIC, isOrganic).apply()
    }


    fun getLanguage(context: Context?): String? {
        if (context == null) return null
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
    }

    fun setLanguage(context: Context?, lang: String) {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }
}
