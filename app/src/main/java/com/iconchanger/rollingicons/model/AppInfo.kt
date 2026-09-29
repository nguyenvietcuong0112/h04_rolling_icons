package com.iconchanger.rollingicons.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val className: String = "",
    val iconCacheKey: String,
    val type: Int = TYPE_APP,
    var isSelected: Boolean = true
) {
    companion object {
        const val TYPE_APP = 1
        const val TYPE_PHOTO = 2
        const val TYPE_EMOJI = 3
    }
}

