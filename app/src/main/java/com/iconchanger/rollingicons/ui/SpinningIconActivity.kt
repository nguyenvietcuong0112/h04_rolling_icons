package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.mobi.libraryads.ads.native_ads.NativeManager
import kotlinx.coroutines.launch

class SpinningIconActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spinning_icon)

        preferenceRepository = PreferenceRepository(this)

        // Nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // TabLayout Setup
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val layoutTab3DShapes = findViewById<View>(R.id.layoutTab3DShapes)
        val layoutTabSpinning = findViewById<View>(R.id.layoutTabSpinning)

        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_3d_shapes))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_spinning))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTab3DShapes.visibility = View.VISIBLE
                        layoutTabSpinning.visibility = View.GONE
                    }
                    1 -> {
                        layoutTab3DShapes.visibility = View.GONE
                        layoutTabSpinning.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Check if intent specified a default tab
        val defaultTab = intent.getIntExtra("default_tab", 0)
        if (defaultTab in 0..1) {
            tabLayout.getTabAt(defaultTab)?.select()
        }

        // 1. Setup 3D Shape Option Listeners (8 shapes including Hearting)
        findViewById<View>(R.id.cardHeart).setOnClickListener { selectShapeAndNavigate("heart") }
        findViewById<View>(R.id.cardInfinity).setOnClickListener { selectShapeAndNavigate("infinity") }
        findViewById<View>(R.id.cardStar).setOnClickListener { selectShapeAndNavigate("star") }
        findViewById<View>(R.id.cardCrown).setOnClickListener { selectShapeAndNavigate("crown") }
        findViewById<View>(R.id.cardFlower).setOnClickListener { selectShapeAndNavigate("flower") }
        findViewById<View>(R.id.cardClover).setOnClickListener { selectShapeAndNavigate("clover") }
        findViewById<View>(R.id.cardButterfly).setOnClickListener { selectShapeAndNavigate("butterfly") }
        findViewById<View>(R.id.cardDiamond).setOnClickListener { selectShapeAndNavigate("diamond") }

        // 2. Setup Spinning Option Listeners (3 patterns)
        findViewById<View>(R.id.cardSingleCircle).setOnClickListener { openSpinningPicker("single_circle") }
        findViewById<View>(R.id.cardDualCircle).setOnClickListener { openSpinningPicker("dual_circle") }
        findViewById<View>(R.id.cardVortex).setOnClickListener { openSpinningPicker("vortex") }

        loadAdsNative()
    }

    private fun selectShapeAndNavigate(shape: String) {
        lifecycleScope.launch {
            preferenceRepository.setShapePathType(shape)
            val intent = Intent(this@SpinningIconActivity, ShapeSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openSpinningPicker(pattern: String) {
        val intent = Intent(this, SpinningAppPickerActivity::class.java).apply {
            putExtra("spinning_pattern", pattern)
        }
        startActivity(intent)
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
