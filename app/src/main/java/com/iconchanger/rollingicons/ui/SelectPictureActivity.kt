package com.iconchanger.rollingicons.ui

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.rollingicons.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SelectPictureActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var gridRecyclerView: RecyclerView
    private lateinit var selectedRecyclerView: RecyclerView
    private lateinit var txtSelectedCount: TextView
    private lateinit var layoutBottomSelectedBar: View
    private lateinit var btnNext: View
    private lateinit var btnBack: View
    private lateinit var tabAllPhoto: View
    private lateinit var tabFolder: View
    private lateinit var txtTabAllPhoto: TextView
    private lateinit var txtTabFolder: TextView
    private lateinit var indicatorAllPhoto: View
    private lateinit var indicatorFolder: View

    private val allPhotosList = ArrayList<Uri>()
    private val displayedPhotosList = ArrayList<Uri>()
    private val foldersList = ArrayList<FolderItem>()
    private val selectedPhotosList = ArrayList<Uri>()
    private val initialSelectedStrings = ArrayList<String>()

    private lateinit var gridAdapter: PhotoGridAdapter
    private lateinit var folderAdapter: FolderGridAdapter
    private lateinit var selectedAdapter: SelectedPreviewAdapter

    private var isFolderTabActive = false

    data class FolderItem(
        val folderName: String,
        val coverUri: Uri,
        val photos: List<Uri>
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            loadDevicePhotos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_picture)

        intent.getStringArrayListExtra("already_selected")?.let {
            initialSelectedStrings.addAll(it)
        }

        gridRecyclerView = findViewById(R.id.gridRecyclerView)
        selectedRecyclerView = findViewById(R.id.selectedRecyclerView)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        layoutBottomSelectedBar = findViewById(R.id.layoutBottomSelectedBar)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        tabAllPhoto = findViewById(R.id.tabAllPhoto)
        tabFolder = findViewById(R.id.tabFolder)
        txtTabAllPhoto = findViewById(R.id.txtTabAllPhoto)
        txtTabFolder = findViewById(R.id.txtTabFolder)
        indicatorAllPhoto = findViewById(R.id.indicatorAllPhoto)
        indicatorFolder = findViewById(R.id.indicatorFolder)

        btnBack.setOnClickListener {
            if (isFolderTabActive && gridRecyclerView.adapter is PhotoGridAdapter) {
                // Back from folder photo view to folder list
                gridRecyclerView.adapter = folderAdapter
                layoutBottomSelectedBar.visibility = View.GONE
            } else {
                finish()
            }
        }

        gridRecyclerView.layoutManager = GridLayoutManager(this, 3)
        gridAdapter = PhotoGridAdapter()
        folderAdapter = FolderGridAdapter()
        gridRecyclerView.adapter = gridAdapter

        selectedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        selectedAdapter = SelectedPreviewAdapter()
        selectedRecyclerView.adapter = selectedAdapter

        tabAllPhoto.setOnClickListener {
            switchToAllPhotosTab()
        }

        tabFolder.setOnClickListener {
            switchToFolderTab()
        }

        btnNext.setOnClickListener {
            saveAndFinish()
        }

        checkPermissionAndLoad()
        updateSelectedUI()
    }

    private fun switchToAllPhotosTab() {
        isFolderTabActive = false
        txtTabAllPhoto.setTextColor(ContextCompat.getColor(this, R.color.cosmic_accent))
        txtTabAllPhoto.setTypeface(null, android.graphics.Typeface.BOLD)
        indicatorAllPhoto.visibility = View.VISIBLE

        txtTabFolder.setTextColor(ContextCompat.getColor(this, R.color.cosmic_text_primary))
        txtTabFolder.setTypeface(null, android.graphics.Typeface.NORMAL)
        indicatorFolder.visibility = View.INVISIBLE

        displayedPhotosList.clear()
        displayedPhotosList.addAll(allPhotosList)
        gridRecyclerView.adapter = gridAdapter
        gridAdapter.notifyDataSetChanged()
        layoutBottomSelectedBar.visibility = View.VISIBLE
    }

    private fun switchToFolderTab() {
        isFolderTabActive = true
        txtTabFolder.setTextColor(ContextCompat.getColor(this, R.color.cosmic_accent))
        txtTabFolder.setTypeface(null, android.graphics.Typeface.BOLD)
        indicatorFolder.visibility = View.VISIBLE

        txtTabAllPhoto.setTextColor(ContextCompat.getColor(this, R.color.cosmic_text_primary))
        txtTabAllPhoto.setTypeface(null, android.graphics.Typeface.NORMAL)
        indicatorAllPhoto.visibility = View.INVISIBLE

        gridRecyclerView.adapter = folderAdapter
        folderAdapter.notifyDataSetChanged()
        layoutBottomSelectedBar.visibility = View.GONE
    }

    private fun checkPermissionAndLoad() {
        loadDevicePhotos()
    }

    private fun loadDevicePhotos() {
        scope.launch {
            val (allPhotos, folders) = withContext(Dispatchers.IO) {
                val photoList = ArrayList<Uri>()
                val folderMap = LinkedHashMap<String, ArrayList<Uri>>()

                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                try {
                    contentResolver.query(
                        queryUri,
                        projection,
                        null,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val folderColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val contentUri = ContentUris.withAppendedId(queryUri, id)
                            photoList.add(contentUri)

                            val folderName = if (folderColumn != -1 && !cursor.isNull(folderColumn)) {
                                cursor.getString(folderColumn) ?: "Internal"
                            } else {
                                "Gallery"
                            }

                            if (!folderMap.containsKey(folderName)) {
                                folderMap[folderName] = ArrayList()
                            }
                            folderMap[folderName]?.add(contentUri)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val folderList = ArrayList<FolderItem>()
                folderMap.forEach { (name, uris) ->
                    if (uris.isNotEmpty()) {
                        folderList.add(FolderItem(name, uris[0], uris))
                    }
                }

                Pair(photoList, folderList)
            }

            allPhotosList.clear()
            allPhotosList.addAll(allPhotos)

            displayedPhotosList.clear()
            displayedPhotosList.addAll(allPhotos)

            foldersList.clear()
            foldersList.addAll(folders)

            // Match initial selected strings if any
            if (initialSelectedStrings.isNotEmpty()) {
                allPhotos.forEach { uri ->
                    val str = uri.toString()
                    if (initialSelectedStrings.contains(str) && !selectedPhotosList.contains(uri)) {
                        selectedPhotosList.add(uri)
                    }
                }
                initialSelectedStrings.forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val fileUri = Uri.fromFile(file)
                        if (!selectedPhotosList.contains(fileUri)) {
                            selectedPhotosList.add(fileUri)
                        }
                    }
                }
            }

            if (isFolderTabActive) {
                folderAdapter.notifyDataSetChanged()
            } else {
                gridAdapter.notifyDataSetChanged()
            }
            updateSelectedUI()
        }
    }

    private fun updateSelectedUI() {
        txtSelectedCount.text = getString(R.string.selected_files_count_only_format, selectedPhotosList.size)
        selectedAdapter.notifyDataSetChanged()
    }

    private fun saveAndFinish() {
        val savedPaths = ArrayList<String>()
        selectedPhotosList.forEach { uri ->
            if (uri.scheme == "file" && uri.path != null) {
                savedPaths.add(uri.path!!)
            } else {
                savedPaths.add(uri.toString())
            }
        }

        val resultIntent = Intent().apply {
            putStringArrayListExtra("selected_photos", savedPaths)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    // Photo Grid Adapter
    private inner class PhotoGridAdapter : RecyclerView.Adapter<PhotoGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rootContainer: View = view.findViewById(R.id.rootContainer)
            val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
            val checkView: ImageView = view.findViewById(R.id.checkView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_picture_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = displayedPhotosList[position]
            val isSelected = selectedPhotosList.contains(uri)

            holder.imgPhoto.load(uri) {
                placeholder(android.R.drawable.ic_menu_gallery)
            }

            holder.rootContainer.isSelected = isSelected
            holder.checkView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedPhotosList.contains(uri)) {
                    selectedPhotosList.remove(uri)
                } else {
                    selectedPhotosList.add(uri)
                }
                notifyItemChanged(position)
                updateSelectedUI()
            }
        }

        override fun getItemCount(): Int = displayedPhotosList.size
    }

    // Folder Grid Adapter
    private inner class FolderGridAdapter : RecyclerView.Adapter<FolderGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgFolderCover: ImageView = view.findViewById(R.id.imgFolderCover)
            val txtFolderName: TextView = view.findViewById(R.id.txtFolderName)
            val txtFolderCount: TextView = view.findViewById(R.id.txtFolderCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_picture_folder, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val folder = foldersList[position]

            holder.txtFolderName.text = folder.folderName
            holder.txtFolderCount.text = getString(R.string.photos_count_format, folder.photos.size)
            holder.imgFolderCover.load(folder.coverUri) {
                placeholder(android.R.drawable.ic_menu_gallery)
            }

            holder.itemView.setOnClickListener {
                displayedPhotosList.clear()
                displayedPhotosList.addAll(folder.photos)
                gridRecyclerView.adapter = gridAdapter
                gridAdapter.notifyDataSetChanged()
                layoutBottomSelectedBar.visibility = View.VISIBLE
            }
        }

        override fun getItemCount(): Int = foldersList.size
    }

    // Horizontal Selected Preview Adapter
    private inner class SelectedPreviewAdapter : RecyclerView.Adapter<SelectedPreviewAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
            val btnRemove: View = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_picture_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = selectedPhotosList[position]
            holder.imgPreview.load(uri)

            holder.btnRemove.setOnClickListener {
                val indexInDisplayed = displayedPhotosList.indexOf(uri)
                selectedPhotosList.removeAt(position)
                notifyDataSetChanged()
                if (indexInDisplayed != -1 && gridRecyclerView.adapter is PhotoGridAdapter) {
                    gridAdapter.notifyItemChanged(indexInDisplayed)
                }
                updateSelectedUI()
            }
        }

        override fun getItemCount(): Int = selectedPhotosList.size
    }
}
