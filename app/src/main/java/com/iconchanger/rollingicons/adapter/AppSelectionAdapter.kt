package com.iconchanger.rollingicons.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.model.AppInfo
import kotlinx.coroutines.launch

class AppSelectionAdapter(
    private val appList: List<AppInfo>,
    private val selectedAppsSet: MutableSet<String>,
    private val canSelectMore: (() -> Boolean)? = null,
    private val onItemClick: ((AppInfo, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.app_picker_icon_view)
        val titleView: TextView = view.findViewById(R.id.app_picker_title_view)
        val checkedView: ImageView = view.findViewById(R.id.app_picker_checked_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.titleView.text = app.appName

        val context = holder.itemView.context
        val cachedIcon = com.iconchanger.rollingicons.data.AppIconCache.get(app.packageName)
        if (cachedIcon != null) {
            holder.iconView.setImageDrawable(cachedIcon)
        } else {
            holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            holder.iconView.tag = app.packageName
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val icon = context.packageManager.getApplicationIcon(app.packageName)
                    com.iconchanger.rollingicons.data.AppIconCache.put(app.packageName, icon)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (holder.iconView.tag == app.packageName) {
                            holder.iconView.setImageDrawable(icon)
                        }
                    }
                } catch (e: Exception) {
                    // ignore, fallback already set
                }
            }
        }

        val isSelected = selectedAppsSet.contains(app.packageName)
        holder.itemView.isSelected = isSelected
        holder.checkedView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

        holder.itemView.setOnClickListener {
            val currentlySelected = selectedAppsSet.contains(app.packageName)
            if (!currentlySelected && canSelectMore != null && !canSelectMore.invoke()) {
                return@setOnClickListener
            }

            val nowSelected = if (currentlySelected) {
                selectedAppsSet.remove(app.packageName)
                false
            } else {
                selectedAppsSet.add(app.packageName)
                true
            }
            holder.itemView.isSelected = nowSelected
            holder.checkedView.setImageResource(if (nowSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)
            onItemClick?.invoke(app, nowSelected)
        }
    }

    override fun getItemCount(): Int = appList.size
}
