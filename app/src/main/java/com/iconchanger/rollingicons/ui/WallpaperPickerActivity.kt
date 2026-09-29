package com.iconchanger.rollingicons.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.wallpaper.RollingWallpaperService
import com.mobi.libraryads.ads.utils.StatusShowAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WallpaperPickerActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnApply: Button
    private var wallpaperMode = "rolling" // "rolling", "spinning", or "shape_path"
    private var selectedPosition = 0

    // List of local wallpaper drawables
    private val wallpapers = listOf(
        R.drawable.bg_wallpaper_00,
        R.drawable.bg_wallpaper_01,
        R.drawable.bg_wallpaper_02,
        R.drawable.bg_wallpaper_03,
        R.drawable.bg_wallpaper_04,
        R.drawable.bg_wallpaper_05,
        R.drawable.bg_wallpaper_06,
        R.drawable.bg_wallpaper_07,
        R.drawable.bg_wallpaper_08
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_picker)

        preferenceRepository = PreferenceRepository(this)
        wallpaperMode = intent.getStringExtra("mode") ?: "rolling"

        recyclerView = findViewById(R.id.wallpaperRecyclerView)
        btnApply = findViewById(R.id.btnApply)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        val adapter = WallpaperAdapter()
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Apply selected wallpaper when Apply button is clicked
        btnApply.setOnClickListener {
            com.iconchanger.rollingicons.utils.AdsConfig.showInterApplyAd(this, it) {
                if (selectedPosition in wallpapers.indices) {
                    val resId = wallpapers[selectedPosition]
                    val uriString = "android.resource://$packageName/$resId"

                    scope.launch {
                        if (wallpaperMode == "spinning") {
                            preferenceRepository.setSpinningBgImagePath(uriString)
                            preferenceRepository.setWallpaperMode("spinning")
                        } else if (wallpaperMode == "shape_path") {
                            preferenceRepository.setShapeBgImagePath(uriString)
                            preferenceRepository.setWallpaperMode("shape_path")
                        } else {
                            preferenceRepository.setBgImagePath(uriString)
                            preferenceRepository.setBgType(2) // Image background
                            preferenceRepository.setWallpaperMode("rolling")
                        }

                        val fromSettings = intent.getBooleanExtra("from_settings", false)
                        if (fromSettings) {
                            Toast.makeText(this@WallpaperPickerActivity, getString(R.string.toast_bg_updated), Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            openLiveWallpaperPreview()
                        }
                    }
                }
            }
        }
    }

    private var wasWallpaperAlreadyApplied = false

    private val liveWallpaperLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        val isApplied = wallpaperInfo?.packageName == packageName

        if (isApplied) {
            val intent = Intent(this, SuccessActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun openLiveWallpaperPreview() {
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        wasWallpaperAlreadyApplied = (wallpaperInfo?.packageName == packageName)

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@WallpaperPickerActivity, RollingWallpaperService::class.java)
            )
        }
        try {
            liveWallpaperLauncher.launch(intent)
            Toast.makeText(this@WallpaperPickerActivity, getString(R.string.toast_apply_wallpaper_tip), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            try {
                liveWallpaperLauncher.launch(chooserIntent)
            } catch (ex: Exception) {
                Toast.makeText(this@WallpaperPickerActivity, getString(R.string.toast_unsupported_wallpaper), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class WallpaperAdapter : RecyclerView.Adapter<WallpaperAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardWallpaper: MaterialCardView = view.findViewById(R.id.cardWallpaper)
            val imgView: ImageView = view.findViewById(R.id.imgWallpaper)
            val imgCheck: ImageView = view.findViewById(R.id.imgCheck)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val resId = wallpapers[position]
            holder.imgView.setImageResource(resId)

            val isSelected = position == selectedPosition
            if (isSelected) {
                holder.cardWallpaper.strokeColor = ContextCompat.getColor(this@WallpaperPickerActivity, R.color.cosmic_accent)
                holder.imgCheck.setImageResource(R.drawable.ic_check_circle)
            } else {
                holder.cardWallpaper.strokeColor = Color.TRANSPARENT
                holder.imgCheck.setImageResource(R.drawable.ic_circle_unselected)
            }

            holder.itemView.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = holder.bindingAdapterPosition
                if (previous >= 0) notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
            }
        }

        override fun getItemCount(): Int = wallpapers.size
    }
}
