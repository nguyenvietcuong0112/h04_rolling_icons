package com.iconchanger.rollingicons.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.model.LiveWallpaperItem
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.mobi.libraryads.ads.native_ads.NativeManager

class LiveWallpaperAdapter(
    private val items: List<LiveWallpaperItem>,
    val spanCount: Int = 2,
    private val onItemClick: (LiveWallpaperItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_AD = 1
    }

    private var selectedPosition = 0

    fun resetSelection() {
        selectedPosition = 0
    }

    fun getSelectedItem(): LiveWallpaperItem? {
        val realIndex = getRealItemIndex(selectedPosition)
        return if (realIndex in items.indices) items[realIndex] else null
    }

    fun isAdEnabled(): Boolean {
        return RemoteConfigs.native_all && items.size >= spanCount
    }

    fun isAdPosition(position: Int): Boolean {
        return isAdEnabled() && position == spanCount
    }

    fun getRealItemIndex(position: Int): Int {
        return if (isAdEnabled() && position > spanCount) {
            position - 1
        } else {
            position
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) VIEW_TYPE_AD else VIEW_TYPE_ITEM
    }

    inner class WallpaperViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardWallpaper: MaterialCardView = view.findViewById(R.id.cardWallpaper)
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val imgSelectedOverlay: ImageView? = view.findViewById(R.id.imgSelectedOverlay)
        val icVideoBadge: ImageView? = view.findViewById(R.id.icVideoBadge)
    }

    inner class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val frAds: FrameLayout = view.findViewById(R.id.layoutAds)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_live_wallpaper, parent, false)
            WallpaperViewHolder(view)
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
        } else if (holder is WallpaperViewHolder) {
            val realIndex = getRealItemIndex(position)
            if (realIndex in items.indices) {
                val item = items[realIndex]

                holder.imgThumbnail.load(item.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_card_border)
                    error(R.drawable.bg_card_border)
                }

                holder.cardWallpaper.strokeColor = Color.TRANSPARENT
                holder.imgSelectedOverlay?.visibility = View.GONE

                holder.itemView.setOnClickListener {
                    selectedPosition = holder.bindingAdapterPosition
                    onItemClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (isAdEnabled()) items.size + 1 else items.size
    }
}
