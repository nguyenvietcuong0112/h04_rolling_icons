package com.iconchanger.rollingicons.ui

import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.ApiWallpaperItem
import com.iconchanger.rollingicons.utils.AdsConfig
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.mobi.libraryads.ads.banner_ads.Banner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ApiWallpaperPreviewActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: ImageView
    private lateinit var btnSetWallpaper: AppCompatButton
    private lateinit var progressBar: LottieAnimationView

    private lateinit var layoutHeader: View
    private lateinit var cardPreviewContainer: FrameLayout
    private lateinit var cardPreview: MaterialCardView
    private lateinit var bottomShadow: View
    private lateinit var btnFullScreen: ImageView
    private lateinit var viewFullscreenOverlay: View
    private var isFullScreen = false

    private val wallpaperList = ArrayList<ApiWallpaperItem>()
    private var currentPosition = 0
    private var pagerAdapter: ApiWallpaperPreviewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_wallpaper_preview)

        preferenceRepository = PreferenceRepository(this)

        val holderList = com.iconchanger.rollingicons.data.WallpaperDataHolder.staticWallpapers
        @Suppress("DEPRECATION")
        val singleItem = intent.getSerializableExtra("api_wallpaper_item") as? ApiWallpaperItem
        val initialPosition = intent.getIntExtra("current_position", 0)

        if (holderList.isNotEmpty()) {
            wallpaperList.addAll(holderList)
            currentPosition = initialPosition.coerceIn(0, wallpaperList.size - 1)
        } else if (singleItem != null) {
            wallpaperList.add(singleItem)
            currentPosition = 0
        }

        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnBack)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        progressBar = findViewById(R.id.progressBar)

        layoutHeader = findViewById(R.id.layoutHeader)
        cardPreviewContainer = findViewById(R.id.cardPreviewContainer)
        cardPreview = findViewById(R.id.cardPreview)
        bottomShadow = findViewById(R.id.bottomShadow)
        btnFullScreen = findViewById(R.id.btnFullScreen)
        viewFullscreenOverlay = findViewById(R.id.viewFullscreenOverlay)

        btnBack.setOnClickListener {
            finish()
        }

        pagerAdapter = ApiWallpaperPreviewAdapter(wallpaperList)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2

        if (wallpaperList.isNotEmpty()) {
            viewPager.setCurrentItem(currentPosition, false)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
            }
        })

        btnSetWallpaper.setOnClickListener {
            val item = if (currentPosition in wallpaperList.indices) wallpaperList[currentPosition] else null
            if (item != null) {
                AdsConfig.showInterApplyAd(this, it) {
                    showSetWallpaperAsDialog(item)
                }
            }
        }

        setupFullScreenToggle()
        loadBanner()
    }

    private fun setupFullScreenToggle() {
        btnFullScreen.setOnClickListener {
            toggleFullScreen(true)
        }

        viewFullscreenOverlay.setOnClickListener {
            toggleFullScreen(false)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullScreen) {
                    toggleFullScreen(false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun toggleFullScreen(fullScreen: Boolean) {
        isFullScreen = fullScreen
        val layoutBanner = findViewById<FrameLayout>(R.id.layoutBanner)

        if (fullScreen) {
            layoutHeader.visibility = View.GONE
            layoutBanner?.visibility = View.GONE
            btnSetWallpaper.visibility = View.GONE
            bottomShadow.visibility = View.GONE
            btnFullScreen.visibility = View.GONE
            viewFullscreenOverlay.visibility = View.VISIBLE

            val params = cardPreviewContainer.layoutParams as LinearLayout.LayoutParams
            params.setMargins(0, 0, 0, 0)
            cardPreviewContainer.layoutParams = params
            cardPreviewContainer.setPadding(0, 0, 0, 0)
            cardPreview.radius = 0f
        } else {
            layoutHeader.visibility = View.VISIBLE
            layoutBanner?.visibility = View.VISIBLE
            btnSetWallpaper.visibility = View.VISIBLE
            bottomShadow.visibility = View.VISIBLE
            btnFullScreen.visibility = View.VISIBLE
            viewFullscreenOverlay.visibility = View.GONE

            val density = resources.displayMetrics.density
            val margin12 = (12 * density).toInt()
            val padding16 = (16 * density).toInt()

            val params = cardPreviewContainer.layoutParams as LinearLayout.LayoutParams
            params.setMargins(0, margin12, 0, 0)
            cardPreviewContainer.layoutParams = params
            cardPreviewContainer.setPadding(padding16, 0, padding16, padding16)
            cardPreview.radius = 24 * density
        }
    }

    private fun showSetWallpaperAsDialog(selectedItem: ApiWallpaperItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_set_wallpaper_as)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)
        val cardHome = dialog.findViewById<MaterialCardView>(R.id.cardOptionHome)
        val cardLock = dialog.findViewById<MaterialCardView>(R.id.cardOptionLock)
        val cardBoth = dialog.findViewById<MaterialCardView>(R.id.cardOptionBoth)

        val radioHome = dialog.findViewById<ImageView>(R.id.radioHome)
        val radioLock = dialog.findViewById<ImageView>(R.id.radioLock)
        val radioBoth = dialog.findViewById<ImageView>(R.id.radioBoth)

        val btnDialogSetWallpaper = dialog.findViewById<Button>(R.id.btnSetWallpaper)

        var selectedTargetOption = 0 // 0: Home, 1: Lock, 2: Both

        fun updateOptionsUI() {
            val selectedBg = Color.parseColor("#F7F4FF")
            val unselectedBg = Color.parseColor("#F9F9FC")
            val selectedStroke = Color.parseColor("#8A52FF")
            val unselectedStroke = Color.parseColor("#EAEAEA")

            cardHome.setCardBackgroundColor(if (selectedTargetOption == 0) selectedBg else unselectedBg)
            cardHome.strokeColor = if (selectedTargetOption == 0) selectedStroke else unselectedStroke
            cardHome.strokeWidth = if (selectedTargetOption == 0) (1.5f * resources.displayMetrics.density).toInt() else (1f * resources.displayMetrics.density).toInt()
            radioHome.setImageResource(if (selectedTargetOption == 0) R.drawable.ic_check_circle else R.drawable.ic_circle_unselected)

            cardLock.setCardBackgroundColor(if (selectedTargetOption == 1) selectedBg else unselectedBg)
            cardLock.strokeColor = if (selectedTargetOption == 1) selectedStroke else unselectedStroke
            cardLock.strokeWidth = if (selectedTargetOption == 1) (1.5f * resources.displayMetrics.density).toInt() else (1f * resources.displayMetrics.density).toInt()
            radioLock.setImageResource(if (selectedTargetOption == 1) R.drawable.ic_check_circle else R.drawable.ic_circle_unselected)

            cardBoth.setCardBackgroundColor(if (selectedTargetOption == 2) selectedBg else unselectedBg)
            cardBoth.strokeColor = if (selectedTargetOption == 2) selectedStroke else unselectedStroke
            cardBoth.strokeWidth = if (selectedTargetOption == 2) (1.5f * resources.displayMetrics.density).toInt() else (1f * resources.displayMetrics.density).toInt()
            radioBoth.setImageResource(if (selectedTargetOption == 2) R.drawable.ic_check_circle else R.drawable.ic_circle_unselected)
        }

        cardHome.setOnClickListener {
            selectedTargetOption = 0
            updateOptionsUI()
        }

        cardLock.setOnClickListener {
            selectedTargetOption = 1
            updateOptionsUI()
        }

        cardBoth.setOnClickListener {
            selectedTargetOption = 2
            updateOptionsUI()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnDialogSetWallpaper.setOnClickListener {
            dialog.dismiss()
            progressBar.visibility = View.VISIBLE
            progressBar.playAnimation()

            scope.launch(Dispatchers.IO) {
                // 1. Save preference for Live Wallpaper engine
                preferenceRepository.setBgImagePath(selectedItem.originalImageUrl)
                    preferenceRepository.setBgType(2)

                    val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                    val isLiveWallpaperActive = wallpaperManager.wallpaperInfo?.packageName == packageName

                    var setSuccess = false
                    if (!isLiveWallpaperActive) {
                        try {
                            val url = URL(selectedItem.originalImageUrl)
                            val inputStream = url.openStream()
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()

                            if (bitmap != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    val flag = when (selectedTargetOption) {
                                        1 -> WallpaperManager.FLAG_LOCK
                                        2 -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                        else -> WallpaperManager.FLAG_SYSTEM
                                    }
                                    wallpaperManager.setBitmap(bitmap, null, true, flag)
                                } else {
                                    wallpaperManager.setBitmap(bitmap)
                                }
                                bitmap.recycle()
                                setSuccess = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        setSuccess = true
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.cancelAnimation()
                        progressBar.visibility = View.GONE

                        if (isLiveWallpaperActive) {
                            Toast.makeText(this@ApiWallpaperPreviewActivity, getString(R.string.toast_wallpaper_updated_rolling), Toast.LENGTH_SHORT).show()
                        } else if (setSuccess) {
                            Toast.makeText(this@ApiWallpaperPreviewActivity, getString(R.string.toast_wallpaper_set_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ApiWallpaperPreviewActivity, getString(R.string.toast_wallpaper_updated), Toast.LENGTH_SHORT).show()
                        }

                        val intent = Intent(this@ApiWallpaperPreviewActivity, SuccessActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }

        updateOptionsUI()
        dialog.show()
    }

    private fun loadBanner() {
        val frAds = findViewById<FrameLayout>(R.id.layoutBanner) ?: return
        val isEnabled = RemoteConfigs.banner_collap_preview

        Banner.requestBanner(
            activity = this,
            id = getString(R.string.banner_collap_preview),
            typeAds = Banner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM,
            adFrame = frAds,
            canShowAd = isEnabled
        )
    }

    private class ApiWallpaperPreviewAdapter(
        private val items: List<ApiWallpaperItem>
    ) : RecyclerView.Adapter<ApiWallpaperPreviewAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imagePreview: ImageView = view.findViewById(R.id.imagePreview)
            val itemProgressBar: LottieAnimationView = view.findViewById(R.id.itemProgressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preview_api_wallpaper, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val thumbnailUrl = item.thumbnailImageUrl
            val originalUrl = if (item.originalImageUrl.isNotEmpty()) item.originalImageUrl else thumbnailUrl

            // 1. Immediately load cached thumbnail without crossfade/delay
            holder.itemProgressBar.visibility = View.GONE
            holder.itemProgressBar.cancelAnimation()
            holder.imagePreview.load(thumbnailUrl) {
                crossfade(false)
            }

            // 2. Smoothly load original high-res image over the thumbnail if available
            if (originalUrl.isNotEmpty() && originalUrl != thumbnailUrl) {
                holder.imagePreview.load(originalUrl) {
                    crossfade(true)
                    placeholder(holder.imagePreview.drawable)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
