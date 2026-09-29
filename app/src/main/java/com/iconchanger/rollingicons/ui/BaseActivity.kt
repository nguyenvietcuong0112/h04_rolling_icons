package com.iconchanger.rollingicons.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iconchanger.rollingicons.utils.SystemUtil
import com.mobi.libraryads.ads.utils.StatusShowAd
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SystemUtil.getPreLanguage(newBase)
        android.util.Log.d("LanguageDebug", "${this.javaClass.simpleName} attachBaseContext: resolved lang=$lang")
        val locale = Locale(if (lang.isNullOrEmpty()) "en" else lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("LanguageDebug", "${this.javaClass.simpleName} onCreate: locale=${resources.configuration.locales.get(0)?.language}")

        // Edge-to-edge Full Screen style
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val decorView = window.decorView
        val windowInsetsController = WindowCompat.getInsetsController(window, decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false

        // Ẩn thanh điều hướng bên dưới (Navigation Bar - Immersive Mode)
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        window.decorView.postDelayed({
            StatusShowAd.ignoreAOA = false
        }, 1000)
    }
}
