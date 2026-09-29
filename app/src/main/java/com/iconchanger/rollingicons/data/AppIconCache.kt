package com.iconchanger.rollingicons.data

import android.graphics.drawable.Drawable
import androidx.collection.LruCache

object AppIconCache {
    private val memoryCache = LruCache<String, Drawable>(250)

    fun get(packageName: String): Drawable? = memoryCache.get(packageName)

    fun put(packageName: String, drawable: Drawable) {
        memoryCache.put(packageName, drawable)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
