package com.iconchanger.rollingicons.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class IconLoader(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "app_icons").apply {
        if (!exists()) mkdirs()
    }

    private val iconSize = 128

    fun loadAppIcon(packageName: String): Bitmap? {
        val cacheFile = File(cacheDir, "${packageName}.png")
        if (cacheFile.exists()) {
            try {
                return BitmapFactory.decodeFile(cacheFile.absolutePath)
            } catch (e: Exception) {
                cacheFile.delete()
            }
        }

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = pm.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(drawable, iconSize, iconSize)
            saveBitmapToCache(bitmap, cacheFile)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Nạp ảnh cá nhân của người dùng, center crop và lưu cache dưới dạng hình vuông.
     */
    fun loadCustomPhotoIcon(uriString: String): Bitmap? {
        val cacheKey = "custom_photo_${uriString.hashCode()}"
        val cacheFile = File(cacheDir, "${cacheKey}.png")
        if (cacheFile.exists()) {
            try {
                return BitmapFactory.decodeFile(cacheFile.absolutePath)
            } catch (e: Exception) {
                cacheFile.delete()
            }
        }

        try {
            val file = File(uriString)
            val originalBitmap = if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val b = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                b
            }

            if (originalBitmap != null) {
                val size = Math.min(originalBitmap.width, originalBitmap.height)
                val x = (originalBitmap.width - size) / 2
                val y = (originalBitmap.height - size) / 2
                val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, iconSize, iconSize, true)
                
                if (croppedBitmap != originalBitmap) {
                    croppedBitmap.recycle()
                }
                originalBitmap.recycle()

                saveBitmapToCache(scaledBitmap, cacheFile)
                return scaledBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Chuyển đổi ký tự Emoji thành Bitmap hình tròn trong suốt và lưu cache.
     */
    fun loadEmojiIcon(emojiName: String): Bitmap? {
        val cacheKey = "emoji_${emojiName.hashCode()}"
        val cacheFile = File(cacheDir, "${cacheKey}.png")
        if (cacheFile.exists()) {
            try {
                return BitmapFactory.decodeFile(cacheFile.absolutePath)
            } catch (e: Exception) {
                cacheFile.delete()
            }
        }

        try {
            val resId = context.resources.getIdentifier(emojiName, "drawable", context.packageName)
            if (resId != 0) {
                val drawable = context.resources.getDrawable(resId, null)
                val bitmap = drawableToBitmap(drawable, iconSize, iconSize)
                saveBitmapToCache(bitmap, cacheFile)
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
        return bitmap
    }

    private fun saveBitmapToCache(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun bitmapToTexture(bitmap: Bitmap): com.badlogic.gdx.graphics.Texture {
        val width = bitmap.width
        val height = bitmap.height
        val pixmap = com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        val argbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        pixmap.pixels.clear()
        argbBitmap.copyPixelsToBuffer(pixmap.pixels)
        pixmap.pixels.position(0)

        if (argbBitmap != bitmap) {
            argbBitmap.recycle()
        }

        val texture = com.badlogic.gdx.graphics.Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

