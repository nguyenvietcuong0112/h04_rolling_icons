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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.adapter.ApiWallpaperAdapter
import com.iconchanger.rollingicons.adapter.CategoryTabAdapter
import com.iconchanger.rollingicons.adapter.LiveWallpaperAdapter
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.ApiWallpaperItem
import com.iconchanger.rollingicons.model.LiveWallpaperItem
import com.iconchanger.rollingicons.utils.AdsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class ApiWallpaperActivity : BaseActivity() {

    companion object {
        const val MODE_LIVE = 0
        const val MODE_STATIC = 1

        val ALLOWED_LIVE_CATEGORIES = listOf(
            "Trending",
            "Anime",
            "Comics",
            "City",
            "Fantasy",
            "Game",
            "Nature",
            "Cute",
            "Silly Smile",
            "Cars",
            "Life-style",
            "Sci-Fi"
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var preferenceRepository: PreferenceRepository

    // UI Views
    private lateinit var btnBack: ImageView
    private lateinit var txtHeaderTitle: TextView
    private lateinit var categoryTabRecyclerView: RecyclerView
    private lateinit var wallpaperRecyclerView: RecyclerView
    private lateinit var progressBar: com.airbnb.lottie.LottieAnimationView
    private lateinit var layoutErrorOrEmpty: LinearLayout
    private lateinit var txtErrorMessage: TextView
    private lateinit var btnRetry: Button

    // Bottom Segmented Tab Views
    private lateinit var tabLive: FrameLayout
    private lateinit var txtTabLive: TextView
    private lateinit var tabStatic: FrameLayout
    private lateinit var txtTabStatic: TextView

    private var currentMode = MODE_LIVE

    // Category Tabs
    private val currentCategoryTabs = ArrayList<String>()
    private var categoryTabAdapter: CategoryTabAdapter? = null
    private var selectedCategory = ""

    // Static Data
    private val staticAllWallpapers = ArrayList<ApiWallpaperItem>()
    private val staticDisplayedWallpapers = ArrayList<ApiWallpaperItem>()
    private val staticCategories = ArrayList<String>()
    private var staticWallpaperAdapter: ApiWallpaperAdapter? = null

    // Live Data
    private val liveAllWallpapers = ArrayList<LiveWallpaperItem>()
    private val liveDisplayedWallpapers = ArrayList<LiveWallpaperItem>()
    private val liveCategoriesMap = LinkedHashMap<String, ArrayList<LiveWallpaperItem>>()
    private val liveCategories = ArrayList<String>()
    private var liveWallpaperAdapter: LiveWallpaperAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_wallpaper)

        preferenceRepository = PreferenceRepository(this)

        btnBack = findViewById(R.id.btnBack)
        txtHeaderTitle = findViewById(R.id.txtHeaderTitle)
        categoryTabRecyclerView = findViewById(R.id.categoryTabRecyclerView)
        wallpaperRecyclerView = findViewById(R.id.wallpaperRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        layoutErrorOrEmpty = findViewById(R.id.layoutErrorOrEmpty)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)

        tabLive = findViewById(R.id.tabLive)
        txtTabLive = findViewById(R.id.txtTabLive)
        tabStatic = findViewById(R.id.tabStatic)
        txtTabStatic = findViewById(R.id.txtTabStatic)

        btnBack.setOnClickListener {
            finish()
        }

        btnRetry.setOnClickListener {
            if (currentMode == MODE_LIVE) {
                fetchLiveWallpapersFromApi()
            } else {
                fetchStaticWallpapersFromApi()
            }
        }

        setupCategoryTabs()
        setupWallpaperGrid()
        setupBottomSegmentedSwitch()

        // Default to Live Mode (as in the screenshot mockup)
        switchMode(MODE_LIVE)
    }

    private fun setupCategoryTabs() {
        categoryTabRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryTabAdapter = CategoryTabAdapter(currentCategoryTabs) { category, _ ->
            onCategoryTabClicked(category)
        }
        categoryTabRecyclerView.adapter = categoryTabAdapter
    }

    private fun setupWallpaperGrid() {
        val layoutManager = GridLayoutManager(this, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val isStatic = (currentMode == MODE_STATIC)
                val isAd = if (isStatic) {
                    staticWallpaperAdapter?.isAdPosition(position) == true
                } else {
                    liveWallpaperAdapter?.isAdPosition(position) == true
                }
                return if (isAd) 2 else 1
            }
        }
        wallpaperRecyclerView.layoutManager = layoutManager

        staticWallpaperAdapter = ApiWallpaperAdapter(staticDisplayedWallpapers, 2) { selectedItem ->
            val index = staticDisplayedWallpapers.indexOf(selectedItem)
            com.iconchanger.rollingicons.data.WallpaperDataHolder.staticWallpapers = staticDisplayedWallpapers
            val intent = Intent(this@ApiWallpaperActivity, ApiWallpaperPreviewActivity::class.java).apply {
                putExtra("api_wallpaper_item", selectedItem)
                putExtra("current_position", if (index >= 0) index else 0)
            }
            startActivity(intent)
        }

        liveWallpaperAdapter = LiveWallpaperAdapter(liveDisplayedWallpapers, 2) { selectedItem ->
            val index = liveDisplayedWallpapers.indexOf(selectedItem)
            com.iconchanger.rollingicons.data.WallpaperDataHolder.liveWallpapers = liveDisplayedWallpapers
            val intent = Intent(this@ApiWallpaperActivity, LiveWallpaperPreviewActivity::class.java).apply {
                putExtra("live_wallpaper_item", selectedItem)
                putExtra("current_position", if (index >= 0) index else 0)
            }
            startActivity(intent)
        }
    }

    private fun setupBottomSegmentedSwitch() {
        tabLive.setOnClickListener {
            if (currentMode != MODE_LIVE) {
                switchMode(MODE_LIVE)
            }
        }

        tabStatic.setOnClickListener {
            if (currentMode != MODE_STATIC) {
                switchMode(MODE_STATIC)
            }
        }
    }

    private fun switchMode(mode: Int) {
        currentMode = mode
        val purpleColor = ContextCompat.getColor(this, R.color.cosmic_accent)
        val whiteColor = ContextCompat.getColor(this, R.color.cosmic_white)

        if (mode == MODE_LIVE) {
            tabLive.setBackgroundResource(R.drawable.bg_segmented_tab_active)
            txtTabLive.setTextColor(whiteColor)

            tabStatic.background = null
            txtTabStatic.setTextColor(purpleColor)

            txtHeaderTitle.text = getString(R.string.tab_live_wallpaper)
            wallpaperRecyclerView.adapter = liveWallpaperAdapter

            if (liveAllWallpapers.isEmpty()) {
                fetchLiveWallpapersFromApi()
            } else {
                refreshLiveCategoriesAndGrid()
            }
        } else {
            tabStatic.setBackgroundResource(R.drawable.bg_segmented_tab_active)
            txtTabStatic.setTextColor(whiteColor)

            tabLive.background = null
            txtTabLive.setTextColor(purpleColor)

            txtHeaderTitle.text = getString(R.string.tab_wallpaper)
            wallpaperRecyclerView.adapter = staticWallpaperAdapter

            if (staticAllWallpapers.isEmpty()) {
                fetchStaticWallpapersFromApi()
            } else {
                refreshStaticCategoriesAndGrid()
            }
        }
    }

    private fun onCategoryTabClicked(category: String) {
        selectedCategory = category
        val allLabel = getString(R.string.category_all)

        if (currentMode == MODE_LIVE) {
            liveDisplayedWallpapers.clear()
            val list = liveCategoriesMap[category]
            if (list != null) {
                liveDisplayedWallpapers.addAll(list)
            } else {
                liveDisplayedWallpapers.addAll(liveAllWallpapers)
            }
            liveWallpaperAdapter?.notifyDataSetChanged()
        } else {
            staticDisplayedWallpapers.clear()
            if (category == allLabel || category.isEmpty()) {
                staticDisplayedWallpapers.addAll(staticAllWallpapers)
            } else {
                val filtered = staticAllWallpapers.filter {
                    normalizeCategory(it.category).equals(category, ignoreCase = true)
                }
                staticDisplayedWallpapers.addAll(filtered)
            }
            staticWallpaperAdapter?.notifyDataSetChanged()
        }

        wallpaperRecyclerView.scrollToPosition(0)
    }

    private fun refreshLiveCategoriesAndGrid() {
        val firstCategory = liveCategories.firstOrNull() ?: ""
        currentCategoryTabs.clear()
        currentCategoryTabs.addAll(liveCategories)
        categoryTabAdapter?.setSelectedIndex(0)
        categoryTabAdapter?.notifyDataSetChanged()

        selectedCategory = firstCategory
        liveDisplayedWallpapers.clear()
        val list = liveCategoriesMap[firstCategory]
        if (list != null) {
            liveDisplayedWallpapers.addAll(list)
        } else {
            liveDisplayedWallpapers.addAll(liveAllWallpapers)
        }
        liveWallpaperAdapter?.notifyDataSetChanged()

        layoutErrorOrEmpty.visibility = View.GONE
        wallpaperRecyclerView.visibility = View.VISIBLE
    }

    private fun refreshStaticCategoriesAndGrid() {
        val allLabel = getString(R.string.category_all)
        currentCategoryTabs.clear()
        currentCategoryTabs.addAll(staticCategories)
        categoryTabAdapter?.setSelectedIndex(0)
        categoryTabAdapter?.notifyDataSetChanged()

        selectedCategory = allLabel
        staticDisplayedWallpapers.clear()
        staticDisplayedWallpapers.addAll(staticAllWallpapers)
        staticWallpaperAdapter?.notifyDataSetChanged()

        layoutErrorOrEmpty.visibility = View.GONE
        wallpaperRecyclerView.visibility = View.VISIBLE
    }

    private fun matchAllowedLiveCategory(rawCat: String): String? {
        val clean = rawCat.trim()
        for (allowed in ALLOWED_LIVE_CATEGORIES) {
            if (allowed.equals(clean, ignoreCase = true) ||
                allowed.replace("-", "").replace(" ", "").equals(clean.replace("-", "").replace(" ", ""), ignoreCase = true)) {
                return allowed
            }
            if (allowed.equals("Game", ignoreCase = true) && clean.equals("Games", ignoreCase = true)) {
                return "Game"
            }
        }
        return null
    }

    private fun fetchLiveWallpapersFromApi() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()
        layoutErrorOrEmpty.visibility = View.GONE
        wallpaperRecyclerView.visibility = View.GONE

        scope.launch(Dispatchers.IO) {
            val allFiles = ArrayList<LiveWallpaperItem>()
            val categoryMap = LinkedHashMap<String, ArrayList<LiveWallpaperItem>>()
            val allLabel = getString(R.string.category_all)

            var fetchError = false

            try {
                val apiUrl = "https://api.1teps.com/uploadfile/files/g10_a25"
                val jsonText = URL(apiUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val dataArray = jsonObject.optJSONArray("data")

                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val catObj = dataArray.getJSONObject(i)
                        val rawCatName = catObj.optString("categoryName").trim()
                        val matchedCat = matchAllowedLiveCategory(rawCatName)

                        if (matchedCat != null) {
                            val filesArray = catObj.optJSONArray("files")
                            val filesList = ArrayList<LiveWallpaperItem>()

                            if (filesArray != null) {
                                for (j in 0 until filesArray.length()) {
                                    val fileObj = filesArray.getJSONObject(j)
                                    val origObj = fileObj.optJSONObject("original")
                                    val thumbObj = fileObj.optJSONObject("thumbnail")

                                    val fileName = origObj?.optString("fileName") ?: ""
                                    val videoUrl = sanitizeUrl(origObj?.optString("downloadLink") ?: origObj?.optString("viewLink") ?: "")
                                    val thumbUrl = sanitizeUrl(thumbObj?.optString("viewLink") ?: origObj?.optString("viewLink") ?: "")
                                    val views = origObj?.optLong("views") ?: 0L

                                    if (thumbUrl.isNotEmpty()) {
                                        val item = LiveWallpaperItem(
                                            fileName = fileName,
                                            category = matchedCat,
                                            videoUrl = videoUrl,
                                            thumbnailUrl = thumbUrl,
                                            views = views
                                        )
                                        filesList.add(item)
                                        allFiles.add(item)
                                    }
                                }
                            }

                            if (filesList.isNotEmpty()) {
                                if (categoryMap.containsKey(matchedCat)) {
                                    categoryMap[matchedCat]?.addAll(filesList)
                                } else {
                                    categoryMap[matchedCat] = filesList
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fetchError = true
            }

            val categoriesList = ArrayList<String>()
            for (cat in ALLOWED_LIVE_CATEGORIES) {
                if (categoryMap.containsKey(cat)) {
                    categoriesList.add(cat)
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.cancelAnimation()
                progressBar.visibility = View.GONE

                if (fetchError || allFiles.isEmpty()) {
                    wallpaperRecyclerView.visibility = View.GONE
                    layoutErrorOrEmpty.visibility = View.VISIBLE
                    txtErrorMessage.text = if (fetchError) {
                        getString(R.string.no_internet_or_wallpapers)
                    } else {
                        "No live wallpapers found"
                    }
                } else {
                    liveAllWallpapers.clear()
                    liveAllWallpapers.addAll(allFiles)
                    liveCategoriesMap.clear()
                    liveCategoriesMap.putAll(categoryMap)
                    liveCategories.clear()
                    liveCategories.addAll(categoriesList)

                    if (currentMode == MODE_LIVE) {
                        refreshLiveCategoriesAndGrid()
                    }
                }
            }
        }
    }

    private fun fetchStaticWallpapersFromApi() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()
        layoutErrorOrEmpty.visibility = View.GONE
        wallpaperRecyclerView.visibility = View.GONE

        scope.launch(Dispatchers.IO) {
            val fetchedItems = ArrayList<ApiWallpaperItem>()
            val categoriesSet = LinkedHashSet<String>()
            val allLabel = getString(R.string.category_all)
            var fetchError = false

            try {
                val apiUrl = "https://api.1teps.com/wallapi/images?image_type=BG&category=all&pageNumber=1675"
                val jsonText = URL(apiUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val imagesArray = jsonObject.optJSONArray("images")

                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val obj = imagesArray.getJSONObject(i)
                        val cat = obj.optString("category")
                        val normalized = normalizeCategory(cat)
                        if (normalized.isNotEmpty()) {
                            categoriesSet.add(normalized)
                        }

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
            } catch (e: Exception) {
                e.printStackTrace()
                fetchError = true
            }

            val catList = ArrayList<String>()
            catList.add(allLabel)
            catList.addAll(categoriesSet)

            withContext(Dispatchers.Main) {
                progressBar.cancelAnimation()
                progressBar.visibility = View.GONE

                if (fetchError || fetchedItems.isEmpty()) {
                    wallpaperRecyclerView.visibility = View.GONE
                    layoutErrorOrEmpty.visibility = View.VISIBLE
                    txtErrorMessage.text = if (fetchError) {
                        getString(R.string.no_internet_or_wallpapers)
                    } else {
                        "No wallpapers found"
                    }
                } else {
                    staticAllWallpapers.clear()
                    staticAllWallpapers.addAll(fetchedItems)
                    staticCategories.clear()
                    staticCategories.addAll(catList)

                    if (currentMode == MODE_STATIC) {
                        refreshStaticCategoriesAndGrid()
                    }
                }
            }
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
            AdsConfig.showInterApplyAd(this, it) {
                progressBar.visibility = View.VISIBLE
                progressBar.playAnimation()

                scope.launch(Dispatchers.IO) {
                    preferenceRepository.setBgImagePath(selectedItem.originalImageUrl)
                    preferenceRepository.setBgType(2)

                    val wallpaperManager = android.app.WallpaperManager.getInstance(applicationContext)
                    val isLiveWallpaperActive = wallpaperManager.wallpaperInfo?.packageName == packageName

                    var setSuccess = false
                    if (!isLiveWallpaperActive) {
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
                            Toast.makeText(this@ApiWallpaperActivity, getString(R.string.toast_wallpaper_updated_rolling), Toast.LENGTH_SHORT).show()
                        } else if (setSuccess) {
                            Toast.makeText(this@ApiWallpaperActivity, getString(R.string.toast_wallpaper_set_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ApiWallpaperActivity, getString(R.string.toast_wallpaper_updated), Toast.LENGTH_SHORT).show()
                        }

                        val intent = Intent(this@ApiWallpaperActivity, MainActivity::class.java).apply {
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

    private fun normalizeCategory(cat: String): String {
        val clean = cat.trim().replaceFirstChar { it.uppercase(Locale.ROOT) }
        return when (clean.lowercase(Locale.ROOT)) {
            "sport", "sports" -> "Sports"
            else -> clean
        }
    }

    private fun sanitizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("https:/") && !url.startsWith("https://")) {
            url = url.replaceFirst("https:/", "https://")
        } else if (url.startsWith("http:/") && !url.startsWith("http://")) {
            url = url.replaceFirst("http:/", "http://")
        }
        return url.replace(" ", "%20")
    }
}

