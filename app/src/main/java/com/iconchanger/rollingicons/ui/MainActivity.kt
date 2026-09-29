package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.utils.AdsConfig
import com.iconchanger.rollingicons.utils.RemoteConfigs
import androidx.lifecycle.lifecycleScope
import com.mobi.libraryads.ads.banner_ads.Banner
import com.mobi.libraryads.ads.native_ads.NativeManager
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private var currentLangCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        currentLangCode = com.iconchanger.rollingicons.utils.SystemUtil.getPreLanguage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val layoutLoadingOverlay = findViewById<View>(R.id.layoutLoadingOverlay)
        layoutLoadingOverlay?.postDelayed({
            layoutLoadingOverlay.visibility = View.GONE
        }, 2000)

        // 1. Nút Settings góc trên bên phải trên header (Không hiện Inter)
        findViewById<ImageView>(R.id.btnHeaderSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. Click vào thẻ Rolling Icon hoặc nút Get Started của Rolling Icon
        val cardRollingIcon = findViewById<View>(R.id.cardRollingIcon)
        val btnGetStartedRolling = findViewById<TextView>(R.id.btnGetStartedRolling)
        cardRollingIcon.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java)
                startActivity(intent)
            }
        }
        btnGetStartedRolling.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java)
                startActivity(intent)
            }
        }

        // 3. Click vào thẻ Spinning Icon hoặc nút Get Started của Spinning Icon
        val cardSpinningIcon = findViewById<View>(R.id.cardSpinningIcon)
        val btnGetStartedSpinning = findViewById<TextView>(R.id.btnGetStartedSpinning)
        cardSpinningIcon.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, SpinningIconActivity::class.java)
                startActivity(intent)
            }
        }
        btnGetStartedSpinning.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, SpinningIconActivity::class.java)
                startActivity(intent)
            }
        }

        // 4. Click vào thẻ Hearting Icon (chạy trực tiếp Heart Path)
        findViewById<View>(R.id.cardShapePathIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                lifecycleScope.launch {
                    com.iconchanger.rollingicons.data.PreferenceRepository(this@MainActivity).setShapePathType("heart")
                    val intent = Intent(this@MainActivity, ShapeSelectionActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        // 5. Click vào thẻ Emoji Icon
        findViewById<View>(R.id.cardEmojiIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                    putExtra("default_tab", 1)
                    putExtra("single_mode", true)
                }
                startActivity(intent)
            }
        }

        // 6. Click vào thẻ Photo Icon
        findViewById<View>(R.id.cardPhotoIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                    putExtra("default_tab", 2)
                    putExtra("single_mode", true)
                }
                startActivity(intent)
            }
        }

        // 7. Click vào thẻ Wallpaper -> Mở ApiWallpaperActivity (Online API Mode)
        findViewById<View>(R.id.cardWallpaper).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, ApiWallpaperActivity::class.java)
                startActivity(intent)
            }
        }

        // Xử lý nút Back hiển thị Popup xác nhận thoát ứng dụng
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })

        loadAdsNative()
        loadBanner()
    }

    override fun onResume() {
        super.onResume()
        val newLang = com.iconchanger.rollingicons.utils.SystemUtil.getPreLanguage(this)
        if (newLang.isNotEmpty() && newLang != currentLangCode) {
            currentLangCode = newLang
            recreate()
        }
    }

    private fun loadBanner() {
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutBanner) ?: return
        val isEnabled = RemoteConfigs.banner_collap_home

        Banner.requestBanner(
            activity = this,
            id = getString(R.string.banner_collap_home),
            typeAds = Banner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM,
            adFrame = frAds,
            canShowAd = isEnabled
        )
    }

    private fun loadAdsNative() {
        val isEnabled = RemoteConfigs.native_home
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutAds) ?: return

        NativeManager.showNative(
            adFrame = frAds,
            adName = "native_home",
            adId = getString(R.string.native_home),
            adLayout = R.layout.layout_native_media_medium,
            canShowAd = isEnabled
        )
    }

    private var activeExitDialog: android.app.Dialog? = null

    private fun showExitDialog() {
        if (activeExitDialog?.isShowing == true) return

        val dialog = android.app.Dialog(this)
        activeExitDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_exit, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnDialogCancel)
        val btnExit = view.findViewById<android.widget.Button>(R.id.btnDialogExit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        dialog.setOnDismissListener {
            activeExitDialog = null
        }
        dialog.show()
    }
}

