package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.mobi.libraryads.ads.native_ads.NativeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShapeSelectorActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var cardHeart: View
    private lateinit var cardInfinity: View
    private lateinit var cardStar: View
    private lateinit var cardFlower: View
    private lateinit var cardClover: View
    private lateinit var cardButterfly: View
    private lateinit var cardCrown: View
    private lateinit var cardDiamond: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shape_selector)

        preferenceRepository = PreferenceRepository(this)

        cardHeart = findViewById(R.id.cardHeart)
        cardInfinity = findViewById(R.id.cardInfinity)
        cardStar = findViewById(R.id.cardStar)
        cardFlower = findViewById(R.id.cardFlower)
        cardClover = findViewById(R.id.cardClover)
        cardButterfly = findViewById(R.id.cardButterfly)
        cardCrown = findViewById(R.id.cardCrown)
        cardDiamond = findViewById(R.id.cardDiamond)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add selection click listeners - direct navigation on click
        cardHeart.setOnClickListener { selectShapeAndNavigate("heart") }
        cardInfinity.setOnClickListener { selectShapeAndNavigate("infinity") }
        cardStar.setOnClickListener { selectShapeAndNavigate("star") }
        cardFlower.setOnClickListener { selectShapeAndNavigate("flower") }
        cardClover.setOnClickListener { selectShapeAndNavigate("clover") }
        cardButterfly.setOnClickListener { selectShapeAndNavigate("butterfly") }
        cardCrown.setOnClickListener { selectShapeAndNavigate("crown") }
        cardDiamond.setOnClickListener { selectShapeAndNavigate("diamond") }

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

    private fun selectShapeAndNavigate(shape: String) {
        scope.launch {
            preferenceRepository.setShapePathType(shape)
            val intent = Intent(this@ShapeSelectorActivity, ShapeSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
