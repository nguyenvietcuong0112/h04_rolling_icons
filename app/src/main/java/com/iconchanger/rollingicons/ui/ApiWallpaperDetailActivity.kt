package com.iconchanger.rollingicons.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.adapter.ApiWallpaperAdapter
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.ApiWallpaperItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class ApiWallpaperDetailActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var txtHeaderTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var apiWallpaperRecyclerView: RecyclerView
    private lateinit var btnContinue: Button
    private lateinit var progressBar: com.airbnb.lottie.LottieAnimationView

    private val wallpaperList = ArrayList<ApiWallpaperItem>()
    private var adapter: ApiWallpaperAdapter? = null
    private var targetCategory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_wallpaper_detail)

        preferenceRepository = PreferenceRepository(this)
        targetCategory = intent.getStringExtra("category_name") ?: ""

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle)
        btnBack = findViewById(R.id.btnBack)
        apiWallpaperRecyclerView = findViewById(R.id.apiWallpaperRecyclerView)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)

        txtHeaderTitle.text = if (targetCategory.isNotEmpty()) targetCategory else getString(R.string.tab_wallpaper)

        btnBack.setOnClickListener {
            finish()
        }

        val layoutManager = GridLayoutManager(this, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isAdPosition(position) == true) 3 else 1
            }
        }
        apiWallpaperRecyclerView.layoutManager = layoutManager
        adapter = ApiWallpaperAdapter(wallpaperList, 3) { _ -> }
        apiWallpaperRecyclerView.adapter = adapter

        // Open Preview Activity when Continue is clicked
        btnContinue.setOnClickListener {
            val selectedItem = adapter?.getSelectedItem()
            if (selectedItem != null) {
                val index = wallpaperList.indexOf(selectedItem)
                com.iconchanger.rollingicons.data.WallpaperDataHolder.staticWallpapers = wallpaperList
                val intent = Intent(this, ApiWallpaperPreviewActivity::class.java).apply {
                    putExtra("api_wallpaper_item", selectedItem)
                    putExtra("current_position", if (index >= 0) index else 0)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.toast_select_wallpaper_first), Toast.LENGTH_SHORT).show()
            }
        }

        fetchCategoryWallpapersFromApi()
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

        val btnSetWallpaper = dialog.findViewById<Button>(R.id.btnSetWallpaper)

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

        btnSetWallpaper.setOnClickListener {
            dialog.dismiss()
            com.iconchanger.rollingicons.utils.AdsConfig.showInterApplyAd(this, it) {
                progressBar.visibility = View.VISIBLE
                progressBar.playAnimation()

                scope.launch(Dispatchers.IO) {
                    // 1. Save preference for Live Wallpaper engine
                    preferenceRepository.setBgImagePath(selectedItem.originalImageUrl)
                    preferenceRepository.setBgType(2)

                    val wallpaperManager = android.app.WallpaperManager.getInstance(applicationContext)
                    val isLiveWallpaperActive = wallpaperManager.wallpaperInfo?.packageName == packageName

                    var setSuccess = false
                    if (!isLiveWallpaperActive) {
                        // Only set static bitmap wallpaper if Live Wallpaper is not currently active
                        try {
                            val url = URL(selectedItem.originalImageUrl)
                            val inputStream = url.openStream()
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream.close()

                            if (bitmap != null) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    val flag = when (selectedTargetOption) {
                                        1 -> android.app.WallpaperManager.FLAG_LOCK
                                        2 -> android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK
                                        else -> android.app.WallpaperManager.FLAG_SYSTEM
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
                            Toast.makeText(this@ApiWallpaperDetailActivity, getString(R.string.toast_wallpaper_updated_rolling), Toast.LENGTH_SHORT).show()
                        } else if (setSuccess) {
                            Toast.makeText(this@ApiWallpaperDetailActivity, getString(R.string.toast_wallpaper_set_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ApiWallpaperDetailActivity, getString(R.string.toast_wallpaper_updated), Toast.LENGTH_SHORT).show()
                        }

                        val intent = Intent(this@ApiWallpaperDetailActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun fetchCategoryWallpapersFromApi() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()

        scope.launch(Dispatchers.IO) {
            val fetchedItems = ArrayList<ApiWallpaperItem>()
            try {
                val apiUrl = "https://api.1teps.com/wallapi/images?image_type=BG&category=all&pageNumber=1675"
                val jsonText = URL(apiUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val imagesArray = jsonObject.optJSONArray("images")

                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val obj = imagesArray.getJSONObject(i)
                        val cat = obj.optString("category")
                        val normalizedCat = normalizeCategory(cat)
                        if (targetCategory.isEmpty() || normalizedCat.equals(targetCategory, ignoreCase = true)) {
                            fetchedItems.add(
                                ApiWallpaperItem(
                                    id = obj.optString("id"),
                                    category = cat,
                                    name = obj.optString("name"),
                                    originalImageUrl = obj.optString("original_image_url"),
                                    thumbnailImageUrl = obj.optString("thumbnail_image_url"),
                                    views = obj.optLong("views")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                progressBar.cancelAnimation()
                progressBar.visibility = View.GONE

                wallpaperList.clear()
                wallpaperList.addAll(fetchedItems)
                adapter?.resetSelection()
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun normalizeCategory(cat: String): String {
        val clean = cat.trim().replaceFirstChar { it.uppercase(Locale.ROOT) }
        return when (clean.lowercase(Locale.ROOT)) {
            "sport", "sports" -> "Sports"
            else -> clean
        }
    }
}
