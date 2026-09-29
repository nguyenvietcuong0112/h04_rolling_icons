package com.iconchanger.rollingicons.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.mobi.libraryads.ads.native_ads.NativeManager

data class CategoryItem(
    val name: String,
    val coverUrl: String
)

class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_AD = 1
        private const val AD_POSITION = 2
    }

    private fun isAdPosition(position: Int): Boolean {
        return RemoteConfigs.native_all && categories.size >= 2 && position == AD_POSITION
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) VIEW_TYPE_AD else VIEW_TYPE_ITEM
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCategoryCover: ImageView = view.findViewById(R.id.imgCategoryCover)
        val txtCategoryTitle: TextView = view.findViewById(R.id.txtCategoryTitle)
    }

    inner class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val frAds: FrameLayout = view.findViewById(R.id.layoutAds)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_category, parent, false)
            CategoryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdViewHolder) {
            val context = holder.itemView.context
            val isEnabled = RemoteConfigs.native_all
            NativeManager.showNative(
                adFrame = holder.frAds,
                adName = "native_all",
                adId = context.getString(R.string.native_all),
                adLayout = R.layout.layout_native_media_medium,
                canShowAd = isEnabled
            )
        } else if (holder is CategoryViewHolder) {
            val categoryIndex = if (RemoteConfigs.native_all && categories.size >= 2 && position > AD_POSITION) {
                position - 1
            } else {
                position
            }

            if (categoryIndex in categories.indices) {
                val item = categories[categoryIndex]
                holder.txtCategoryTitle.text = item.name

                holder.imgCategoryCover.load(item.coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_card_border)
                    error(R.drawable.bg_card_border)
                }

                holder.itemView.setOnClickListener {
                    onCategoryClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (RemoteConfigs.native_all && categories.size >= 2) {
            categories.size + 1
        } else {
            categories.size
        }
    }
}
