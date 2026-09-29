package com.iconchanger.rollingicons.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.wallpaper.RollingWallpaperService
import com.mobi.libraryads.ads.utils.StatusShowAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirstOpenWallpaperActivity : BaseActivity() {

    private var wasWallpaperAlreadyApplied = false
    private var hasLaunchedPreview = false

    private val liveWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        val isApplied = wallpaperInfo?.packageName == packageName

        val isSuccess = if (result.resultCode == RESULT_OK) {
            isApplied
        } else {
            !wasWallpaperAlreadyApplied && isApplied
        }

        if (isSuccess) {
            val intent = Intent(this, SuccessActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("use_inter_set_wallpaper", true)
            }
            startActivity(intent)
            finish()
        } else {
            com.iconchanger.rollingicons.utils.AdsConfig.showInterSetWallpaperAd(this) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_open_wallpaper)

        hasLaunchedPreview = savedInstanceState?.getBoolean("has_launched_preview", false) ?: false
        if (!hasLaunchedPreview) {
            hasLaunchedPreview = true
            prepareDefaultsAndOpenWallpaper()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("has_launched_preview", hasLaunchedPreview)
    }

    override fun onBackPressed() {
        com.iconchanger.rollingicons.utils.AdsConfig.showInterSetWallpaperAd(this) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun prepareDefaultsAndOpenWallpaper() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val appRepository = AppRepository(this@FirstOpenWallpaperActivity)
                val preferenceRepository = PreferenceRepository(this@FirstOpenWallpaperActivity)

                // 1. Mặc định chọn 20 icon đầu tiên
                val allApps = appRepository.getInstalledApps()
                val default20 = allApps.take(20).map { it.packageName }.toSet()
                appRepository.saveSelectedApps(default20)

                // 2. Mặc định wallpaper default đầu tiên
                val defaultWallpaperUri = "android.resource://$packageName/${R.drawable.bg_wallpaper_00}"
                preferenceRepository.setBgImagePath(defaultWallpaperUri)
                preferenceRepository.setBgType(2) // Image
                preferenceRepository.setWallpaperMode("rolling")
            }

            openLiveWallpaperPreview()
        }
    }

    private fun openLiveWallpaperPreview() {
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        wasWallpaperAlreadyApplied = (wallpaperInfo?.packageName == packageName)

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@FirstOpenWallpaperActivity, RollingWallpaperService::class.java)
            )
        }
        try {
            liveWallpaperLauncher.launch(intent)
            Toast.makeText(this, getString(R.string.toast_apply_wallpaper_tip), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            try {
                liveWallpaperLauncher.launch(chooserIntent)
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.toast_unsupported_wallpaper), Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }
}
