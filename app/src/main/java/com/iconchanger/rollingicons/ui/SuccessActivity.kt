package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.utils.AdsConfig
import com.mobi.libraryads.ads.native_ads.NativeManager

class SuccessActivity : BaseActivity() {

    private var useInterSetWallpaper = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        useInterSetWallpaper = intent.getBooleanExtra("use_inter_set_wallpaper", false)

        findViewById<Button>(R.id.btnOk)?.setOnClickListener { view ->
            showInterAndNavigateHome(view)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showInterAndNavigateHome()
            }
        })

        loadAdsNative()
    }

    private fun loadAdsNative() {
        val isEnabled = com.iconchanger.rollingicons.utils.RemoteConfigs.native_all
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutAds) ?: return

        NativeManager.showNative(
            adFrame = frAds,
            adName = "native_all",
            adId = getString(R.string.native_all),
            adLayout = R.layout.layout_native_media,
            canShowAd = isEnabled
        )
    }

    private fun showInterAndNavigateHome(view: android.view.View? = null) {
        if (useInterSetWallpaper) {
            AdsConfig.showInterSetWallpaperAd(this, view) {
                navigateToHome()
            }
        } else {
            AdsConfig.showInterSuccessAd(this, view) {
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        showInterAndNavigateHome()
    }
}
