package com.iconchanger.rollingicons.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BindAppDialog(
    context: Context,
    private val emojiDrawableRes: Int,
    private val initialPackageName: String?,
    private val onSaveBinding: (packageName: String?) -> Unit
) : Dialog(context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val appRepository = AppRepository(context)

    private lateinit var btnClose: View
    private lateinit var imgEmojiPreview: ImageView
    private lateinit var layoutAppPreview: View
    private lateinit var imgAppPreview: ImageView
    private lateinit var imgPlusPlaceholder: View
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var btnSave: View

    private val appsList = ArrayList<AppInfo>()
    private var selectedPackageName: String? = initialPackageName
    private lateinit var adapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_bind_app)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        btnClose = findViewById(R.id.btnClose)
        imgEmojiPreview = findViewById(R.id.imgEmojiPreview)
        layoutAppPreview = findViewById(R.id.layoutAppPreview)
        imgAppPreview = findViewById(R.id.imgAppPreview)
        imgPlusPlaceholder = findViewById(R.id.imgPlusPlaceholder)
        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        btnSave = findViewById(R.id.btnSave)

        imgEmojiPreview.setImageResource(emojiDrawableRes)

        btnClose.setOnClickListener { dismiss() }

        layoutAppPreview.setOnClickListener {
            // Click on app preview box toggles clearing the selected app
            if (selectedPackageName != null) {
                selectedPackageName = null
                updateHeaderAppPreview()
                adapter.notifyDataSetChanged()
            }
        }

        btnSave.setOnClickListener {
            onSaveBinding(selectedPackageName)
            dismiss()
        }

        appsRecyclerView.layoutManager = GridLayoutManager(context, 4)
        adapter = AppsAdapter()
        appsRecyclerView.adapter = adapter

        updateHeaderAppPreview()
        loadInstalledApps()
    }

    private fun updateHeaderAppPreview() {
        val pkg = selectedPackageName
        if (!pkg.isNullOrEmpty()) {
            try {
                val icon = context.packageManager.getApplicationIcon(pkg)
                imgAppPreview.setImageDrawable(icon)
                imgAppPreview.visibility = View.VISIBLE
                imgPlusPlaceholder.visibility = View.GONE
                layoutAppPreview.setBackgroundResource(R.drawable.bg_card_selected)
            } catch (e: Exception) {
                imgAppPreview.visibility = View.GONE
                imgPlusPlaceholder.visibility = View.VISIBLE
                layoutAppPreview.setBackgroundResource(R.drawable.bg_dashed_purple_box)
            }
        } else {
            imgAppPreview.visibility = View.GONE
            imgPlusPlaceholder.visibility = View.VISIBLE
            layoutAppPreview.setBackgroundResource(R.drawable.bg_dashed_purple_box)
        }
    }

    private fun loadInstalledApps() {
        scope.launch {
            val installed = withContext(Dispatchers.IO) {
                appRepository.getInstalledApps()
            }
            appsList.clear()
            appsList.addAll(installed)
            adapter.notifyDataSetChanged()
        }
    }

    private inner class AppsAdapter : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rootContainer: View = view.findViewById(R.id.rootContainer)
            val imgAppIcon: ImageView = view.findViewById(R.id.imgAppIcon)
            val txtAppName: TextView = view.findViewById(R.id.txtAppName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bind_app_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = appsList[position]
            holder.txtAppName.text = app.appName

            val cachedIcon = com.iconchanger.rollingicons.data.AppIconCache.get(app.packageName)
            if (cachedIcon != null) {
                holder.imgAppIcon.setImageDrawable(cachedIcon)
            } else {
                holder.imgAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                holder.imgAppIcon.tag = app.packageName
                scope.launch(Dispatchers.IO) {
                    try {
                        val icon = context.packageManager.getApplicationIcon(app.packageName)
                        com.iconchanger.rollingicons.data.AppIconCache.put(app.packageName, icon)
                        withContext(Dispatchers.Main) {
                            if (holder.imgAppIcon.tag == app.packageName) {
                                holder.imgAppIcon.setImageDrawable(icon)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore, fallback set
                    }
                }
            }

            val isSelected = (app.packageName == selectedPackageName)
            holder.rootContainer.isSelected = isSelected
            holder.rootContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_card_selected else R.drawable.bg_select_picture_item_unselected
            )

            holder.itemView.setOnClickListener {
                selectedPackageName = if (isSelected) null else app.packageName
                updateHeaderAppPreview()
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = appsList.size
    }
}
