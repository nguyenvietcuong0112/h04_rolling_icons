package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.iconchanger.rollingicons.R
import com.mobi.libraryads.ads.native_ads.NativeManager

class SpinningIconActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spinning_icon)

        // Quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Action điều hướng sang chọn app
        val openAppPicker = { pattern: String ->
            val intent = Intent(this, SpinningAppPickerActivity::class.java).apply {
                putExtra("spinning_pattern", pattern)
            }
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.cardSingleCircle).setOnClickListener {
            openAppPicker("single_circle")
        }

        findViewById<android.view.View>(R.id.cardDualCircle).setOnClickListener {
            openAppPicker("dual_circle")
        }

        findViewById<android.view.View>(R.id.cardVortex).setOnClickListener {
            openAppPicker("vortex")
        }

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
}

