package com.iconchanger.rollingicons.data

import com.iconchanger.rollingicons.model.ApiWallpaperItem
import com.iconchanger.rollingicons.model.LiveWallpaperItem

object WallpaperDataHolder {
    var staticWallpapers: List<ApiWallpaperItem> = emptyList()
    var liveWallpapers: List<LiveWallpaperItem> = emptyList()

    fun clear() {
        staticWallpapers = emptyList()
        liveWallpapers = emptyList()
    }
}
