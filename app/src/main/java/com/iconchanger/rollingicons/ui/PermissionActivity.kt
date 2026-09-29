package com.iconchanger.rollingicons.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.mobi.libraryads.ads.native_ads.NativeManager

class PermissionActivity : BaseActivity() {

    private lateinit var switchPhotoPermission: SwitchCompat
    private lateinit var btnContinue: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        switchPhotoPermission.isChecked = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isPhotoPermissionGranted()) {
            val intent = Intent(this@PermissionActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_permission)

        switchPhotoPermission = findViewById(R.id.switchPhotoPermission)
        btnContinue = findViewById(R.id.btnContinue)

        updatePermissionSwitchState()

        switchPhotoPermission.setOnClickListener {
            switchPhotoPermission.isChecked = true
        }

        btnContinue.setOnClickListener {
            val intent = Intent(this@PermissionActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }

        loadNativeAd()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionSwitchState()
    }

    private fun updatePermissionSwitchState() {
        switchPhotoPermission.isChecked = isPhotoPermissionGranted()
    }

    private fun isPhotoPermissionGranted(): Boolean {
        return true
    }

    private fun loadNativeAd() {
        val frAds = findViewById<FrameLayout>(R.id.layoutAds) ?: return
        val isEnabled = RemoteConfigs.native_permission

        NativeManager.showNative(
            adFrame = frAds,
            adName = "native_permission",
            adId = getString(R.string.native_all),
            adLayout = R.layout.layout_native_media,
            canShowAd = isEnabled
        )
    }
}
