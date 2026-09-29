package com.iconchanger.rollingicons.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.utils.AdsConfig
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.iconchanger.rollingicons.wallpaper.RollingWallpaperService
import com.mobi.libraryads.ads.native_ads.NativeManager
import kotlinx.coroutines.launch

class CustomizeActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var btnBack: ImageView
    private lateinit var cardLiveWallpaper: View
    private lateinit var cardHomeLauncher: View

    private lateinit var btnSizeSmall: TextView
    private lateinit var btnSizeMiddle: TextView
    private lateinit var btnSizeLarge: TextView
    private lateinit var switchWallpaperTouch: SwitchCompat
    private lateinit var switchFloatButton: SwitchCompat
    private lateinit var btnPreviewApply: View

    private var selectedMode = "wallpaper" // "wallpaper" or "launcher"
    private var selectedSize = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)

        preferenceRepository = PreferenceRepository(this)

        // Binding views
        btnBack = findViewById(R.id.btnBack)
        cardLiveWallpaper = findViewById(R.id.cardLiveWallpaper)
        cardHomeLauncher = findViewById(R.id.cardHomeLauncher)

        btnSizeSmall = findViewById(R.id.btnSizeSmall)
        btnSizeMiddle = findViewById(R.id.btnSizeMiddle)
        btnSizeLarge = findViewById(R.id.btnSizeLarge)
        switchWallpaperTouch = findViewById(R.id.switchWallpaperTouch)
        switchFloatButton = findViewById(R.id.switchFloatButton)
        btnPreviewApply = findViewById(R.id.btnPreviewApply)

        // Back button click listener
        btnBack.setOnClickListener { finish() }

        // Setup Mode Selector Cards Clicks
        cardLiveWallpaper.setOnClickListener {
            selectedMode = "wallpaper"
            updateModeSelectionUI()
        }
        cardHomeLauncher.setOnClickListener {
            selectedMode = "launcher"
            updateModeSelectionUI()
        }

        // Setup Icon Size buttons
        btnSizeSmall.setOnClickListener { updateIconSize(0.6f) }
        btnSizeMiddle.setOnClickListener { updateIconSize(1.0f) }
        btnSizeLarge.setOnClickListener { updateIconSize(1.4f) }

        // Load config from preferences
        loadConfigFromPrefs()

        // Preview & Apply Action Button
        btnPreviewApply.setOnClickListener {
            AdsConfig.showInterApplyAd(this, it) {
                saveConfigAndNavigate()
            }
        }
        loadAdsNative()

    }

    private fun loadAdsNative() {
        val isEnabled = RemoteConfigs.native_all
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutAds) ?: return

        NativeManager.showNative(
            adFrame = frAds,
            adName = "native_all",
            adId = getString(R.string.native_all),
            adLayout = R.layout.layout_native_media_medium,
            canShowAd = isEnabled
        )
    }


    private fun loadConfigFromPrefs() {
        lifecycleScope.launch {
            selectedSize = preferenceRepository.getIconSize()
            updateSizeButtonsUI(selectedSize)

            switchWallpaperTouch.isChecked = preferenceRepository.isWallpaperTouchEnabled()
            switchFloatButton.isChecked = preferenceRepository.isFloatButtonEnabled()

            // 3. Default to wallpaper mode selection UI
            selectedMode = "wallpaper"
            updateModeSelectionUI()
        }
    }

    private fun updateModeSelectionUI() {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_card_selected)
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.bg_card_unselected)

        cardLiveWallpaper.background = if (selectedMode == "wallpaper") selectedBg else unselectedBg
        cardHomeLauncher.background = if (selectedMode != "wallpaper") selectedBg else unselectedBg
    }

    private fun updateIconSize(size: Float) {
        selectedSize = size
        updateSizeButtonsUI(size)
    }

    private fun updateSizeButtonsUI(size: Float) {
        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_purple_gradient_btn)
        val inactiveBg = null

        val isSmall = size < 0.8f
        val isMiddle = size in 0.8f..1.2f
        val isLarge = size > 1.2f

        btnSizeSmall.background = if (isSmall) activeBg else inactiveBg
        btnSizeMiddle.background = if (isMiddle) activeBg else inactiveBg
        btnSizeLarge.background = if (isLarge) activeBg else inactiveBg

        val whiteColor = android.graphics.Color.WHITE
        val darkColor = android.graphics.Color.parseColor("#1A1A1A")

        btnSizeSmall.setTextColor(if (isSmall) whiteColor else darkColor)
        btnSizeMiddle.setTextColor(if (isMiddle) whiteColor else darkColor)
        btnSizeLarge.setTextColor(if (isLarge) whiteColor else darkColor)
    }

    private fun saveConfigAndNavigate() {
        lifecycleScope.launch {
            // Save settings parameters
            preferenceRepository.setIconSize(selectedSize)
            preferenceRepository.setWallpaperTouchEnabled(switchWallpaperTouch.isChecked)
            preferenceRepository.setFloatButtonEnabled(switchFloatButton.isChecked)

            if (selectedMode == "wallpaper") {
                // Navigate to system Live Wallpaper chooser preview
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@CustomizeActivity, RollingWallpaperService::class.java)
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(intent)
                    Toast.makeText(this@CustomizeActivity, getString(R.string.toast_apply_wallpaper_tip), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        startActivity(chooserIntent)
                    } catch (ex: Exception) {
                        Toast.makeText(this@CustomizeActivity, getString(R.string.toast_unsupported_wallpaper), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Navigate to interactive LauncherActivity preview screen
                val intent = Intent(this@CustomizeActivity, LauncherActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            finish()
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}


