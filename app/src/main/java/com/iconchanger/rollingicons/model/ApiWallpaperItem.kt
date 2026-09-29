package com.iconchanger.rollingicons.model

import java.io.Serializable

data class ApiWallpaperItem(
    val id: String,
    val category: String,
    val name: String,
    val originalImageUrl: String,
    val thumbnailImageUrl: String,
    val views: Long
) : Serializable
