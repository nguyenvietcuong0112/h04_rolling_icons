package com.iconchanger.rollingicons.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.adapter.AppSelectionAdapter
import com.iconchanger.rollingicons.adapter.EmojiSelectionAdapter
import com.iconchanger.rollingicons.adapter.PhotoSelectionAdapter
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.AppInfo
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.utils.StatusShowAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShapeSelectionActivity : BaseActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    // Tab Views
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutTabApps: LinearLayout
    private lateinit var layoutTabEmojis: LinearLayout
    private lateinit var layoutTabPhotos: LinearLayout
    private lateinit var btnNext: Button

    // Apps tab data & views
    private lateinit var appRecyclerView: RecyclerView
    private lateinit var appSearchEdit: EditText
    private lateinit var appSearchClear: ImageView
    private lateinit var progressBar: android.view.View
    private var allAppsList = listOf<AppInfo>()
    private var filteredAppsList = mutableListOf<AppInfo>()
    private val selectedAppsSet = HashSet<String>()
    private lateinit var appAdapter: AppSelectionAdapter

    // Emojis tab data & views
    private lateinit var emojiRecyclerView: RecyclerView
    private lateinit var emojiTabSmileys: ImageView
    private lateinit var emojiTabAnimals: ImageView
    private lateinit var emojiTabLove: ImageView
    private lateinit var emojiTabJokes: ImageView
    private val selectedEmojisSet = HashSet<String>()
    private val emojiAppBindingsMap = HashMap<String, String>()
    private val emojiList = ArrayList<String>()
    private lateinit var emojiAdapter: EmojiSelectionAdapter

    private val emojiGroup = (1..83).map { String.format("emoji_emoji_%02d", it) }
    private val animalGroup = (1..28).map { String.format("emoji_animal_%02d", it) }
    private val loveGroup = (1..20).map { String.format("emoji_love_%02d", it) }
    private val jokeGroup = (1..22).map { String.format("emoji_joke_%02d", it) }

    // Photos tab data & views
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var txtPhotoCount: TextView
    private val selectedPhotosList = ArrayList<String>()
    private lateinit var photoAdapter: PhotoSelectionAdapter

    private lateinit var txtItemLimitInfo: TextView

    private val maxItemLimit = 20

    private fun updateItemLimitInfo() {
        if (::txtItemLimitInfo.isInitialized) {
            val total = selectedAppsSet.size + selectedEmojisSet.size + selectedPhotosList.size
            txtItemLimitInfo.text = getString(R.string.selected_count_format, total, maxItemLimit)
        }
    }

    private fun canSelectMore(): Boolean {
        val total = selectedAppsSet.size + selectedEmojisSet.size + selectedPhotosList.size
        if (total >= maxItemLimit) {
            Toast.makeText(this, getString(R.string.toast_max_item_limit, maxItemLimit), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private val pickMultipleVisualMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val total = selectedAppsSet.size + selectedEmojisSet.size + selectedPhotosList.size
            val remaining = maxItemLimit - total
            val urisToAdd = if (remaining > 0) uris.take(remaining) else emptyList()

            scope.launch(Dispatchers.IO) {
                val loader = com.iconchanger.rollingicons.data.IconLoader(this@ShapeSelectionActivity)
                urisToAdd.forEach { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val uriStr = uri.toString()
                    loader.loadCustomPhotoIcon(uriStr)
                    if (!selectedPhotosList.contains(uriStr)) {
                        selectedPhotosList.add(uriStr)
                    }
                }
                withContext(Dispatchers.Main) {
                    updatePhotoCount()
                    photoAdapter.notifyDataSetChanged()
                    updateItemLimitInfo()
                    preferenceRepository.setSelectedPhotos(selectedPhotosList.toSet())
                }
            }
        }
    }

    private fun openCustomPicturePicker() {
        if (!canSelectMore()) return
        pickMultipleVisualMediaLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rolling_selection)

        appRepository = AppRepository(this)
        preferenceRepository = PreferenceRepository(this)

        val defaultTab = intent.getIntExtra("default_tab", 0)
        val singleMode = intent.getBooleanExtra("single_mode", false)

        val txtHeaderTitle = findViewById<TextView>(R.id.txtHeaderTitle)
        txtHeaderTitle.text = getString(R.string.shape_path_title)

        // Bind layouts
        tabLayout = findViewById(R.id.tabLayout)
        layoutTabApps = findViewById(R.id.layoutTabApps)
        layoutTabEmojis = findViewById(R.id.layoutTabEmojis)
        layoutTabPhotos = findViewById(R.id.layoutTabPhotos)
        btnNext = findViewById(R.id.btnNext)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup Tabs
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_apps)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_emoji)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_photos)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTabApps.visibility = View.VISIBLE
                        layoutTabEmojis.visibility = View.GONE
                        layoutTabPhotos.visibility = View.GONE
                    }

                    1 -> {
                        layoutTabApps.visibility = View.GONE
                        layoutTabEmojis.visibility = View.VISIBLE
                        layoutTabPhotos.visibility = View.GONE
                    }

                    2 -> {
                        layoutTabApps.visibility = View.GONE
                        layoutTabEmojis.visibility = View.GONE
                        layoutTabPhotos.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        txtItemLimitInfo = findViewById(R.id.txtItemLimitInfo)
        updateItemLimitInfo()

        // 1. Setup Apps Tab
        appRecyclerView = findViewById(R.id.appRecyclerView)
        appSearchEdit = findViewById(R.id.appSearchEdit)
        appSearchClear = findViewById(R.id.appSearchClear)
        progressBar = findViewById(R.id.progressBar)

        appRecyclerView.layoutManager = GridLayoutManager(this, 3)
        appAdapter = AppSelectionAdapter(filteredAppsList, selectedAppsSet, canSelectMore = { canSelectMore() }, onItemClick = { _, _ -> updateItemLimitInfo() })
        appRecyclerView.adapter = appAdapter

        appSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                filterApps(query)
                appSearchClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        appSearchClear.setOnClickListener {
            appSearchEdit.setText("")
        }

        // 2. Setup Emojis Tab
        emojiRecyclerView = findViewById(R.id.emojiRecyclerView)
        emojiTabSmileys = findViewById(R.id.emojiTabSmileys)
        emojiTabAnimals = findViewById(R.id.emojiTabAnimals)
        emojiTabLove = findViewById(R.id.emojiTabLove)
        emojiTabJokes = findViewById(R.id.emojiTabJokes)

        emojiRecyclerView.layoutManager = GridLayoutManager(this, 4)
        emojiAdapter = EmojiSelectionAdapter(
            emojiList = emojiList,
            selectedEmojisSet = selectedEmojisSet,
            emojiAppBindingsMap = emojiAppBindingsMap,
            onLinkAppClick = { emojiName, resId, boundPackage ->
                val dialog = BindAppDialog(this, resId, boundPackage) { newPackage ->
                    scope.launch {
                        preferenceRepository.setEmojiAppBinding(emojiName, newPackage)
                    }
                    if (newPackage.isNullOrEmpty()) {
                        emojiAppBindingsMap.remove(emojiName)
                    } else {
                        emojiAppBindingsMap[emojiName] = newPackage
                    }
                    val pos = emojiList.indexOf(emojiName)
                    if (pos >= 0) emojiAdapter.notifyItemChanged(pos)
                }
                dialog.show()
            },
            canSelectMore = { canSelectMore() },
            onItemClick = { _, _ -> updateItemLimitInfo() }
        )
        emojiRecyclerView.adapter = emojiAdapter

        setupEmojiTabs()

        // 3. Setup Photos Tab
        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        txtPhotoCount = findViewById(R.id.txtPhotoCount)
        findViewById<View>(R.id.btnEmptyAddPhoto)?.setOnClickListener {
            openCustomPicturePicker()
        }

        photoRecyclerView.layoutManager = GridLayoutManager(this, 3)
        photoAdapter = PhotoSelectionAdapter(
            selectedPhotosList,
            onAddPhotoClick = { openCustomPicturePicker() },
            onDeletePhotoClick = { photoIndex -> showDeletePhotoConfirmationDialog(photoIndex) }
        )
        photoRecyclerView.adapter = photoAdapter

        // Load all data
        loadData()

        // Click Tiếp tục
        btnNext.setOnClickListener {
            val total = selectedAppsSet.size + selectedEmojisSet.size + selectedPhotosList.size
            if (total == 0) {
                Toast.makeText(this, getString(R.string.toast_select_one_app), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (total > maxItemLimit) {
                Toast.makeText(this, getString(R.string.toast_max_item_limit, maxItemLimit), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            com.iconchanger.rollingicons.utils.AdsConfig.showInterNextAd(this, it) {
                scope.launch {
                    // Save everything
                    withContext(Dispatchers.IO) {
                        appRepository.saveSelectedApps(selectedAppsSet)
                        preferenceRepository.setSelectedEmojis(selectedEmojisSet)
                        preferenceRepository.setSelectedPhotos(selectedPhotosList.toSet())
                    }

                    if (singleMode) {
                        withContext(Dispatchers.IO) {
                            preferenceRepository.setWallpaperMode("shape_path")
                        }
                        openLiveWallpaperPreview()
                    } else {
                        val intent = Intent(this@ShapeSelectionActivity, WallpaperPickerActivity::class.java).apply {
                            putExtra("mode", "shape_path")
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        if (defaultTab in 0..2) {
            tabLayout.getTabAt(defaultTab)?.select()
        }

        if (singleMode) {
            tabLayout.visibility = View.GONE
            if (defaultTab == 1) {
                txtHeaderTitle.text = getString(R.string.emoji_icon_title)
            } else if (defaultTab == 2) {
                txtHeaderTitle.text = getString(R.string.photo_icon_title)
            }
        }
    }

    private var wasWallpaperAlreadyApplied = false

    private val liveWallpaperLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        StatusShowAd.ignoreAOA = true
        val wallpaperInfo = android.app.WallpaperManager.getInstance(this).wallpaperInfo
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
        val wallpaperInfo = android.app.WallpaperManager.getInstance(this).wallpaperInfo
        wasWallpaperAlreadyApplied = (wallpaperInfo?.packageName == packageName)

        val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                android.content.ComponentName(this@ShapeSelectionActivity, com.iconchanger.rollingicons.wallpaper.RollingWallpaperService::class.java)
            )
        }
        try {
            liveWallpaperLauncher.launch(intent)
            Toast.makeText(this, getString(R.string.toast_apply_wallpaper_tip), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val chooserIntent = Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            try {
                liveWallpaperLauncher.launch(chooserIntent)
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.toast_unsupported_wallpaper), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEmojiTabs() {
        emojiTabSmileys.setOnClickListener { selectEmojiTab(1) }
        emojiTabAnimals.setOnClickListener { selectEmojiTab(2) }
        emojiTabLove.setOnClickListener { selectEmojiTab(3) }
        emojiTabJokes.setOnClickListener { selectEmojiTab(4) }
    }

    private fun selectEmojiTab(group: Int) {
        val activeBg = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_emoji_tab_selected)
        val inactiveBg = null

        emojiTabSmileys.background = if (group == 1) activeBg else inactiveBg
        emojiTabSmileys.alpha = if (group == 1) 1.0f else 0.55f

        emojiTabAnimals.background = if (group == 2) activeBg else inactiveBg
        emojiTabAnimals.alpha = if (group == 2) 1.0f else 0.55f

        emojiTabLove.background = if (group == 3) activeBg else inactiveBg
        emojiTabLove.alpha = if (group == 3) 1.0f else 0.55f

        emojiTabJokes.background = if (group == 4) activeBg else inactiveBg
        emojiTabJokes.alpha = if (group == 4) 1.0f else 0.55f

        emojiList.clear()
        when (group) {
            1 -> emojiList.addAll(emojiGroup)
            2 -> emojiList.addAll(animalGroup)
            3 -> emojiList.addAll(loveGroup)
            4 -> emojiList.addAll(jokeGroup)
        }
        emojiAdapter.notifyDataSetChanged()
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        scope.launch {
            // Load Apps
            val (installed, selectedApps) = withContext(Dispatchers.IO) {
                val inst = appRepository.getInstalledApps()
                val sel = appRepository.getSelectedApps().map { it.packageName }.toSet()
                Pair(inst, sel)
            }
            progressBar.visibility = View.GONE
            allAppsList = installed
            selectedAppsSet.clear()
            selectedAppsSet.addAll(selectedApps)
            filterApps("")

            // Load Emojis
            val selectedEmojis = preferenceRepository.getSelectedEmojis()
            val emojiBindings = preferenceRepository.getEmojiAppBindings()
            selectedEmojisSet.clear()
            selectedEmojisSet.addAll(selectedEmojis)
            emojiAppBindingsMap.clear()
            emojiAppBindingsMap.putAll(emojiBindings)
            selectEmojiTab(1)

            // Load Photos
            val savedPhotos = preferenceRepository.getSelectedPhotos()
            selectedPhotosList.clear()
            selectedPhotosList.addAll(savedPhotos)

            enforceMaxItemLimitOnLoad()

            updatePhotoCount()
            photoAdapter.notifyDataSetChanged()
            updateItemLimitInfo()
        }
    }

    private fun enforceMaxItemLimitOnLoad() {
        val total = selectedAppsSet.size + selectedEmojisSet.size + selectedPhotosList.size
        if (total <= maxItemLimit) return

        var count = 0
        val trimmedApps = HashSet<String>()
        for (app in selectedAppsSet) {
            if (count < maxItemLimit) {
                trimmedApps.add(app)
                count++
            }
        }
        selectedAppsSet.clear()
        selectedAppsSet.addAll(trimmedApps)

        val trimmedEmojis = HashSet<String>()
        for (emoji in selectedEmojisSet) {
            if (count < maxItemLimit) {
                trimmedEmojis.add(emoji)
                count++
            }
        }
        selectedEmojisSet.clear()
        selectedEmojisSet.addAll(trimmedEmojis)

        val trimmedPhotos = ArrayList<String>()
        for (photo in selectedPhotosList) {
            if (count < maxItemLimit) {
                trimmedPhotos.add(photo)
                count++
            }
        }
        selectedPhotosList.clear()
        selectedPhotosList.addAll(trimmedPhotos)
    }

    private fun filterApps(query: String) {
        filteredAppsList.clear()
        if (query.isEmpty()) {
            filteredAppsList.addAll(allAppsList)
        } else {
            allAppsList.forEach { app ->
                if (app.appName.lowercase().contains(query)) {
                    filteredAppsList.add(app)
                }
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun updatePhotoCount() {
        txtPhotoCount.text = getString(R.string.photos_count_format, selectedPhotosList.size)
        val isEmpty = selectedPhotosList.isEmpty()
        findViewById<View>(R.id.layoutEmptyPhotoState)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
        findViewById<View>(R.id.photoRecyclerView)?.visibility =
            if (isEmpty) View.GONE else View.VISIBLE
        updateItemLimitInfo()
    }

    private fun showDeletePhotoConfirmationDialog(photoIndex: Int) {
        if (photoIndex !in 0 until selectedPhotosList.size) return

        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_delete_photo, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val txtTitle = view.findViewById<TextView>(R.id.txtDialogTitle)
        val txtMessage = view.findViewById<TextView>(R.id.txtDialogMessage)
        val btnCancel = view.findViewById<Button>(R.id.btnDialogCancel)
        val btnDelete = view.findViewById<Button>(R.id.btnDialogDelete)

        txtTitle.text = getString(R.string.delete_photo_title)
        txtMessage.text = getString(R.string.delete_photo_message)
        btnDelete.text = getString(R.string.btn_delete)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            if (photoIndex in 0 until selectedPhotosList.size) {
                selectedPhotosList.removeAt(photoIndex)
                updatePhotoCount()
                photoAdapter.notifyDataSetChanged()
                scope.launch(Dispatchers.IO) {
                    preferenceRepository.setSelectedPhotos(selectedPhotosList.toSet())
                }
            }
        }

        val frAds = view.findViewById<android.widget.FrameLayout>(R.id.layoutAds)
        if (frAds != null) {
            NativeManager.showNative(
                adFrame = frAds,
                adName = "native_popup",
                adId = getString(R.string.native_all),
                adLayout = R.layout.layout_native_media,
                canShowAd = com.iconchanger.rollingicons.utils.RemoteConfigs.native_popup
            )
        }

        dialog.show()
    }
}
