package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.adapter.LiveWallpaperAdapter
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.LiveWallpaperItem
import com.iconchanger.rollingicons.utils.AdsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class LiveWallpaperDetailActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var txtHeaderTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var liveWallpaperRecyclerView: RecyclerView
    private lateinit var btnContinue: Button
    private lateinit var progressBar: com.airbnb.lottie.LottieAnimationView

    private val wallpaperList = ArrayList<LiveWallpaperItem>()
    private var adapter: LiveWallpaperAdapter? = null
    private var targetCategory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_wallpaper_detail)

        preferenceRepository = PreferenceRepository(this)
        targetCategory = intent.getStringExtra("category_name") ?: ""

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle)
        btnBack = findViewById(R.id.btnBack)
        liveWallpaperRecyclerView = findViewById(R.id.liveWallpaperRecyclerView)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)

//        txtHeaderTitle.text = if (targetCategory.isNotEmpty()) targetCategory else getString(R.string.tab_live_wallpaper)

        btnBack.setOnClickListener {
            finish()
        }

        val layoutManager = GridLayoutManager(this, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isAdPosition(position) == true) 3 else 1
            }
        }
        liveWallpaperRecyclerView.layoutManager = layoutManager
        adapter = LiveWallpaperAdapter(wallpaperList, 3) { _ ->
            // Just select the item, do not navigate immediately
        }
        liveWallpaperRecyclerView.adapter = adapter

        btnContinue.setOnClickListener {
            val selectedItem = adapter?.getSelectedItem()
            if (selectedItem != null) {
                openVideoPreview(selectedItem)
            } else {
                Toast.makeText(this, getString(R.string.toast_select_wallpaper_first), Toast.LENGTH_SHORT).show()
            }
        }

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val passedList = intent.getSerializableExtra("category_files") as? ArrayList<LiveWallpaperItem>
        if (!passedList.isNullOrEmpty()) {
            wallpaperList.clear()
            wallpaperList.addAll(passedList)
            adapter?.resetSelection()
            adapter?.notifyDataSetChanged()
        } else {
            fetchCategoryLiveWallpapersFromApi()
        }
    }

    private fun openVideoPreview(selectedItem: LiveWallpaperItem) {
        val index = wallpaperList.indexOf(selectedItem)
        com.iconchanger.rollingicons.data.WallpaperDataHolder.liveWallpapers = wallpaperList
        val intent = Intent(this, LiveWallpaperPreviewActivity::class.java).apply {
            putExtra("live_wallpaper_item", selectedItem)
            putExtra("current_position", if (index >= 0) index else 0)
        }
        startActivity(intent)
    }

    private fun fetchCategoryLiveWallpapersFromApi() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()

        scope.launch(Dispatchers.IO) {
            val fetchedItems = ArrayList<LiveWallpaperItem>()
            try {
                val apiUrl = "https://api.1teps.com/uploadfile/files/g10_a25"
                val jsonText = URL(apiUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val dataArray = jsonObject.optJSONArray("data")

                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val catObj = dataArray.getJSONObject(i)
                        val catName = catObj.optString("categoryName").trim()
                        if (targetCategory.isEmpty() || catName.equals(targetCategory, ignoreCase = true)) {
                            val filesArray = catObj.optJSONArray("files")
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
                                        fetchedItems.add(
                                            LiveWallpaperItem(
                                                fileName = fileName,
                                                category = catName,
                                                videoUrl = videoUrl,
                                                thumbnailUrl = thumbUrl,
                                                views = views
                                            )
                                        )
                                    }
                                }
                            }
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

