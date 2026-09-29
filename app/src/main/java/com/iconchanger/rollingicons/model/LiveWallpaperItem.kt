package com.iconchanger.rollingicons.model

import java.io.Serializable

data class LiveWallpaperItem(
    val fileName: String,
    val category: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val views: Long = 0,
    var isSelected: Boolean = false
) : Serializable

data class LiveCategoryItem(
    val categoryName: String,
    val categoryCode: String,
    val coverUrl: String,
    val files: List<LiveWallpaperItem>
) : Serializable

