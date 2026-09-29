package com.iconchanger.rollingicons.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.airbnb.lottie.LottieAnimationView

import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.LiveWallpaperItem
import com.iconchanger.rollingicons.utils.AdsConfig
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.iconchanger.rollingicons.wallpaper.RollingWallpaperService
import com.iconchanger.rollingicons.widget.TextureVideoView
import com.mobi.libraryads.ads.banner_ads.Banner
import com.mobi.libraryads.ads.utils.StatusShowAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class LiveWallpaperPreviewActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: ImageView
    private lateinit var btnSetLiveWallpaper: Button
    private lateinit var progressBar: LottieAnimationView

    private lateinit var layoutHeader: View
    private lateinit var cardPreviewContainer: FrameLayout
    private lateinit var cardPreview: MaterialCardView
    private lateinit var bottomShadow: View
    private lateinit var btnFullScreen: ImageView
    private lateinit var viewFullscreenOverlay: View
    private var isFullScreen = false

    private val wallpaperList = ArrayList<LiveWallpaperItem>()
    private var currentPosition = 0
    private var pagerAdapter: LiveWallpaperPreviewAdapter? = null

    private var downloadJob: Job? = null
    private var isCurrentDownloaded = false
    private var previousPosition = -1

    private var pendingTargetFile: File? = null
    private var pendingItem: LiveWallpaperItem? = null

    private val liveWallpaperLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        val isApplied = wallpaperInfo?.packageName == packageName &&
                wallpaperInfo.serviceName == RollingWallpaperService::class.java.name

        if (result.resultCode == RESULT_OK || (result.resultCode != RESULT_CANCELED && isApplied)) {
            val target = pendingTargetFile
            val item = pendingItem
            if (target != null && item != null && target.exists() && target.length() > 0) {
                scope.launch(Dispatchers.IO) {
                    val prefRepo = PreferenceRepository(applicationContext)
                    prefRepo.setBgType(3)
                    prefRepo.setBgVideoPath(target.absolutePath)
                    prefRepo.setBgImagePath(item.thumbnailUrl)
                    prefRepo.clearPreviewBg()
                }
            }

            val intent = Intent(this, SuccessActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        } else {
            // User backed out / canceled from system preview without clicking "Đặt hình nền"
            scope.launch(Dispatchers.IO) {
                val prefRepo = PreferenceRepository(applicationContext)
                prefRepo.clearPreviewBg()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_wallpaper_preview)

        val holderList = com.iconchanger.rollingicons.data.WallpaperDataHolder.liveWallpapers
        @Suppress("DEPRECATION")
        val singleItem = intent.getSerializableExtra("live_wallpaper_item") as? LiveWallpaperItem
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
        btnSetLiveWallpaper = findViewById(R.id.btnSetLiveWallpaper)
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

        pagerAdapter = LiveWallpaperPreviewAdapter(wallpaperList, ::getLocalFile)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2

        if (wallpaperList.isNotEmpty()) {
            viewPager.setCurrentItem(currentPosition, false)
            previousPosition = currentPosition
            onWallpaperSelected(currentPosition)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (previousPosition != -1 && previousPosition != position) {
                    pagerAdapter?.getHolderAt(previousPosition)?.stopVideo()
                }
                previousPosition = position
                currentPosition = position
                onWallpaperSelected(position)
            }
        })

        btnSetLiveWallpaper.setOnClickListener {
            val item = if (currentPosition in wallpaperList.indices) wallpaperList[currentPosition] else null
            if (item != null) {
                val target = getLocalFile(item)
                AdsConfig.showInterApplyAd(this, it) {
                    applyVideoWallpaper(item, target)
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
            btnSetLiveWallpaper.visibility = View.GONE
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
            btnSetLiveWallpaper.visibility = View.VISIBLE
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

    private fun getLocalFile(item: LiveWallpaperItem): File {
        val rawFileName = if (item.fileName.isNotEmpty()) item.fileName else "live_wallpaper_${item.videoUrl.hashCode()}.mp4"
        val cleanFileName = "live_wallpaper_" + rawFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(filesDir, cleanFileName)
    }

    private fun setButtonReady(isReady: Boolean) {
        btnSetLiveWallpaper.isEnabled = isReady
        btnSetLiveWallpaper.alpha = if (isReady) 1.0f else 0.5f
    }

    private fun onWallpaperSelected(position: Int) {
        if (position !in wallpaperList.indices) return
        val item = wallpaperList[position]
        val target = getLocalFile(item)

        val alreadyCached = target.exists() && target.length() > 1024L
        isCurrentDownloaded = alreadyCached
        setButtonReady(alreadyCached)

        viewPager.post {
            pagerAdapter?.getHolderAt(position)?.playVideo()
        }

        startBackgroundPreDownload(item, target, position)
    }

    private fun startBackgroundPreDownload(item: LiveWallpaperItem, target: File, targetPosition: Int) {
        val videoUrl = item.videoUrl
        if (videoUrl.isEmpty()) return

        if (target.exists() && target.length() > 1024L) {
            isCurrentDownloaded = true
            setButtonReady(true)
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val url = URL(videoUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 20000
                connection.connect()

                val input = BufferedInputStream(connection.getInputStream(), 65536)
                val tempFile = File(filesDir, "${target.name}.tmp")
                val output = BufferedOutputStream(FileOutputStream(tempFile), 65536)

                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }

                output.flush()
                output.close()
                input.close()

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (target.exists()) target.delete()
                    tempFile.renameTo(target)
                    withContext(Dispatchers.Main) {
                        if (currentPosition == targetPosition) {
                            isCurrentDownloaded = true
                            setButtonReady(true)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyVideoWallpaper(item: LiveWallpaperItem, target: File) {
        val videoUrl = item.videoUrl

        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()

        scope.launch(Dispatchers.IO) {
            if (!isCurrentDownloaded || !target.exists() || target.length() == 0L) {
                if (downloadJob?.isActive == true) {
                    downloadJob?.join()
                } else {
                    // Fallback manual download
                    try {
                        val url = URL(videoUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 15000
                        val input = BufferedInputStream(connection.getInputStream(), 65536)
                        val output = BufferedOutputStream(FileOutputStream(target), 65536)
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                        output.close()
                        input.close()
                        isCurrentDownloaded = target.exists() && target.length() > 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val downloadSuccess = target.exists() && target.length() > 0
            if (downloadSuccess) {
                pendingTargetFile = target
                pendingItem = item

                val prefRepo = PreferenceRepository(applicationContext)
                // Set ONLY preview keys so system preview chooser plays it without affecting home screen!
                prefRepo.setPreviewBgType(3)
                prefRepo.setPreviewBgVideoPath(target.absolutePath)
                prefRepo.setPreviewBgImagePath(item.thumbnailUrl)
            }

            withContext(Dispatchers.Main) {
                progressBar.cancelAnimation()
                progressBar.visibility = View.GONE

                if (downloadSuccess) {
                    openLiveWallpaperChooser()
                } else {
                    Toast.makeText(this@LiveWallpaperPreviewActivity, getString(R.string.toast_failed_prepare_video), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openLiveWallpaperChooser() {
        StatusShowAd.ignoreAOA = true
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@LiveWallpaperPreviewActivity, RollingWallpaperService::class.java)
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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pagerAdapter?.getHolderAt(currentPosition)?.playVideo()
    }

    override fun onPause() {
        super.onPause()
        pagerAdapter?.getHolderAt(currentPosition)?.pauseVideo()
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

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        pagerAdapter?.releaseAll()
    }

    private class LiveWallpaperPreviewAdapter(
        private val items: List<LiveWallpaperItem>,
        private val getLocalFile: (LiveWallpaperItem) -> File
    ) : RecyclerView.Adapter<LiveWallpaperPreviewAdapter.ViewHolder>() {

        private val attachedHolders = mutableMapOf<Int, ViewHolder>()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageThumbnail: ImageView = view.findViewById(R.id.imageThumbnail)
            val videoPreview: TextureVideoView = view.findViewById(R.id.videoPreview)
            val itemProgressBar: LottieAnimationView = view.findViewById(R.id.itemProgressBar)
            var currentItem: LiveWallpaperItem? = null

            fun bind(item: LiveWallpaperItem) {
                currentItem = item
                imageThumbnail.animate().cancel()
                imageThumbnail.alpha = 1.0f
                imageThumbnail.visibility = View.VISIBLE
                itemProgressBar.visibility = View.GONE
                itemProgressBar.cancelAnimation()
                imageThumbnail.load(item.thumbnailUrl) {
                    crossfade(false)
                }
            }

            fun playVideo() {
                val item = currentItem ?: return
                val localFile = getLocalFile(item)
                val uri = if (localFile.exists() && localFile.length() > 1024L) {
                    Uri.fromFile(localFile)
                } else if (item.videoUrl.isNotEmpty()) {
                    Uri.parse(item.videoUrl)
                } else {
                    null
                }

                if (uri == null) return

                videoPreview.setOnFirstFrameListener {
                    imageThumbnail.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            imageThumbnail.visibility = View.GONE
                            imageThumbnail.alpha = 1.0f
                        }
                        .start()
                    itemProgressBar.cancelAnimation()
                    itemProgressBar.visibility = View.GONE
                }

                videoPreview.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    try {
                        mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    } catch (e: Exception) {
                        // ignore
                    }
                    videoPreview.start()
                }

                videoPreview.setOnErrorListener { _, _, _ ->
                    itemProgressBar.cancelAnimation()
                    itemProgressBar.visibility = View.GONE
                    imageThumbnail.alpha = 1.0f
                    imageThumbnail.visibility = View.VISIBLE
                    false
                }

                videoPreview.setVideoURI(uri)
            }

            fun pauseVideo() {
                videoPreview.pause()
            }

            fun stopVideo() {
                imageThumbnail.animate().cancel()
                imageThumbnail.alpha = 1.0f
                imageThumbnail.visibility = View.VISIBLE
                itemProgressBar.cancelAnimation()
                itemProgressBar.visibility = View.GONE
                videoPreview.setOnFirstFrameListener(null)
                videoPreview.stopPlayback()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preview_live_wallpaper, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            super.onViewAttachedToWindow(holder)
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                attachedHolders[pos] = holder
            }
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            super.onViewDetachedFromWindow(holder)
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                attachedHolders.remove(pos)
            }
            holder.stopVideo()
        }

        override fun onViewRecycled(holder: ViewHolder) {
            super.onViewRecycled(holder)
            holder.stopVideo()
        }

        override fun getItemCount(): Int = items.size

        fun getHolderAt(position: Int): ViewHolder? = attachedHolders[position]

        fun releaseAll() {
            attachedHolders.values.forEach { it.stopVideo() }
            attachedHolders.clear()
        }
    }
}
