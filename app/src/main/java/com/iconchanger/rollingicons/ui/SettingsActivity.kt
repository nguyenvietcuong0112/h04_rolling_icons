package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.iconchanger.rollingicons.BuildConfig
import androidx.lifecycle.lifecycleScope

import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.utils.SystemUtil
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.utils.StatusShowAd
import com.mobi.libraryads.commons.utils.Constants
import com.mobi.libraryads.views.language.LanguageActivity
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var btnBack: ImageView
    private lateinit var btnSetDefaultLauncher: View
    private lateinit var switchMakeLauncher: SwitchCompat
    private lateinit var btnSizeSmall: TextView
    private lateinit var btnSizeMiddle: TextView
    private lateinit var btnSizeLarge: TextView
    private lateinit var switchFloatButton: SwitchCompat
    private lateinit var switchWallpaperTouch: SwitchCompat
    private lateinit var tvSettingsVersionValue: TextView
    private var currentLangCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        currentLangCode = SystemUtil.getPreLanguage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferenceRepository = PreferenceRepository(this)

        btnBack = findViewById(R.id.btnBack)
        btnSetDefaultLauncher = findViewById(R.id.btnSetDefaultLauncher)
        switchMakeLauncher = findViewById(R.id.switchMakeLauncher)
        btnSizeSmall = findViewById(R.id.btnSizeSmall)
        btnSizeMiddle = findViewById(R.id.btnSizeMiddle)
        btnSizeLarge = findViewById(R.id.btnSizeLarge)
        switchFloatButton = findViewById(R.id.switchFloatButton)
        switchWallpaperTouch = findViewById(R.id.switchWallpaperTouch)
        tvSettingsVersionValue = findViewById(R.id.tv_settings_version_value)

        btnBack.setOnClickListener {
            com.iconchanger.rollingicons.utils.AdsConfig.showInterClickAd(this, it) {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                com.iconchanger.rollingicons.utils.AdsConfig.showInterClickAd(this@SettingsActivity) {
                    finish()
                }
            }
        })

        // 1. Make it as launcher action
        val openLauncherSettingsAction = {
            Toast.makeText(this, getString(R.string.toast_select_launcher), Toast.LENGTH_LONG).show()

           StatusShowAd.ignoreAOA = true
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }

        tvSettingsVersionValue.text = "v${BuildConfig.VERSION_NAME}"

        btnSetDefaultLauncher.setOnClickListener { openLauncherSettingsAction() }
        switchMakeLauncher.setOnClickListener { openLauncherSettingsAction() }

        // 2. Icon size Segmented control
        btnSizeSmall.setOnClickListener { updateIconSize(0.6f) }
        btnSizeMiddle.setOnClickListener { updateIconSize(1.0f) }
        btnSizeLarge.setOnClickListener { updateIconSize(1.4f) }

        // 3. Enable float button
        switchFloatButton.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setFloatButtonEnabled(isChecked)
            }
        }

        // 4. Enable wallpaper touch
        switchWallpaperTouch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setWallpaperTouchEnabled(isChecked)
            }
        }

        // Support Information Actions
        findViewById<View>(R.id.btnLanguage)?.setOnClickListener {
            android.util.Log.d("LanguageDebug", "SettingsActivity: btnLanguage clicked, currentLangCode=$currentLangCode")
            val intent = Intent(this, LanguageActivity::class.java).apply {
                putExtra(Constants.FROM_SETTING, true)
                putExtra(Constants.NAME_AD_NATIVE_LANGUAGE, "native_all")
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnPrivacyPolicy)?.setOnClickListener {
            openWebPage("")
        }

        findViewById<View>(R.id.btnAboutUs)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_about_app), Toast.LENGTH_LONG).show()
        }

        loadSettingsFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        val newLang = SystemUtil.getPreLanguage(this)
        android.util.Log.d("LanguageDebug", "SettingsActivity onResume: currentLangCode=$currentLangCode, newLang=$newLang")
        if (newLang.isNotEmpty() && newLang != currentLangCode) {
            android.util.Log.d("LanguageDebug", "SettingsActivity onResume: Language changed detected from $currentLangCode to $newLang. Applying and navigating to MainActivity...")
            currentLangCode = newLang
            SystemUtil.changeLang(newLang, this)
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("disable_animation", true)
            }
            startActivity(intent)
            finish()
            return
        }
        switchMakeLauncher.isChecked = isDefaultLauncher()
    }

    private fun loadSettingsFromPrefs() {
        lifecycleScope.launch {
            // Load Icon size state
            val size = preferenceRepository.getIconSize()
            updateSizeButtonsUI(size)

            // Load Launcher state
            switchMakeLauncher.isChecked = isDefaultLauncher()

            // Load Float Button
            switchFloatButton.isChecked = preferenceRepository.isFloatButtonEnabled()

            // Load Wallpaper Touch
            switchWallpaperTouch.isChecked = preferenceRepository.isWallpaperTouchEnabled()
        }
    }

    private fun updateIconSize(size: Float) {
        updateSizeButtonsUI(size)
        lifecycleScope.launch {
            preferenceRepository.setIconSize(size)
        }
    }

    private fun updateSizeButtonsUI(size: Float) {
        val isSmall = size < 0.8f
        val isMiddle = size in 0.8f..1.2f

        if (isSmall) {
            setSelectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        } else if (isMiddle) {
            setUnselectedSegmentButton(btnSizeSmall)
            setSelectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        } else {
            setUnselectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setSelectedSegmentButton(btnSizeLarge)
        }
        // Load Native Ad
        loadAdsNative()
    }

    private fun loadAdsNative() {
        val isEnabled = com.iconchanger.rollingicons.utils.RemoteConfigs.native_all
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutAds) ?: return

        NativeManager.showNative(
            adFrame = frAds,
            adName = "native_all",
            adId = getString(R.string.native_all),
            adLayout = R.layout.layout_native_media_medium,
            canShowAd = isEnabled
        )
    }

    private fun setSelectedSegmentButton(textView: TextView) {
        textView.background = ContextCompat.getDrawable(this, R.drawable.bg_icons_size_true)
        textView.setTextColor(Color.WHITE)
    }

    private fun setUnselectedSegmentButton(textView: TextView) {
        textView.background = null
        textView.setTextColor(Color.parseColor("#1A1A1A"))
    }

    private fun isDefaultLauncher(): Boolean {
        val localIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(localIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_no_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
