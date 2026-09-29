package com.iconchanger.rollingicons.render.mode

import android.content.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.data.IconLoader
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.AppInfo
import com.iconchanger.rollingicons.render.GameRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShapePathModeStrategy(
    private val context: Context,
    private val scope: CoroutineScope
) : WallpaperModeStrategy {

    private val shapePathIcons = ArrayList<GameRenderer.IconData>()
    private var shapePathAngle = 0f
    private var shapePathType = "heart"

    override fun reload() {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val loader = IconLoader(context)
            val prefs = PreferenceRepository(context)

            shapePathType = prefs.getShapePathType()
            val iconScale = prefs.getIconSize()
            val baseSize = 1.2f * iconScale

            val selectedApps = repository.getSelectedApps()
            val selectedPhotos = prefs.getSelectedPhotos()
            val selectedEmojis = prefs.getSelectedEmojis()

            val appBitmaps = selectedApps.mapNotNull { app ->
                val bitmap = loader.loadAppIcon(app.packageName)
                if (bitmap != null) Pair(app, bitmap) else null
            }

            val photoBitmaps = selectedPhotos.mapNotNull { photoUri ->
                val bitmap = loader.loadCustomPhotoIcon(photoUri)
                if (bitmap != null) Pair(photoUri, bitmap) else null
            }

            val emojiBitmaps = selectedEmojis.mapNotNull { emoji ->
                val emojiName = if (emoji.startsWith("emoji_")) emoji else "emoji_$emoji"
                val bitmap = loader.loadEmojiIcon(emojiName)
                if (bitmap != null) Pair(emojiName, bitmap) else null
            }

            Gdx.app.postRunnable {
                val newIcons = ArrayList<GameRenderer.IconData>()

                appBitmaps.forEach { (app, bitmap) ->
                    val texture = loader.bitmapToTexture(bitmap)
                    newIcons.add(GameRenderer.IconData(texture, baseSize, app))
                }

                photoBitmaps.forEach { (photoUri, bitmap) ->
                    val texture = loader.bitmapToTexture(bitmap)
                    val photoApp = AppInfo(
                        packageName = photoUri,
                        appName = "Photo",
                        iconCacheKey = photoUri,
                        type = AppInfo.TYPE_PHOTO
                    )
                    newIcons.add(GameRenderer.IconData(texture, baseSize, photoApp))
                }

                emojiBitmaps.forEach { (emoji, bitmap) ->
                    val texture = loader.bitmapToTexture(bitmap)
                    val emojiApp = AppInfo(
                        packageName = emoji,
                        appName = "Emoji",
                        iconCacheKey = emoji,
                        type = AppInfo.TYPE_EMOJI
                    )
                    newIcons.add(GameRenderer.IconData(texture, baseSize, emojiApp))
                }

                synchronized(shapePathIcons) {
                    for (icon in shapePathIcons) {
                        icon.texture.dispose()
                    }
                    shapePathIcons.clear()
                    shapePathIcons.addAll(newIcons)
                }
            }
        }
    }

    private fun computeShapeCoordinates(t: Float, R: Float): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        when (shapePathType) {
            "heart" -> {
                val sinT = kotlin.math.sin(t)
                x = (R * 16f * sinT * sinT * sinT) / 16f
                y = (R * (13f * kotlin.math.cos(t) - 5f * kotlin.math.cos(2f * t) - 2f * kotlin.math.cos(3f * t) - kotlin.math.cos(4f * t))) / 16f
            }
            "infinity" -> {
                val cosT = kotlin.math.cos(t)
                val sinT = kotlin.math.sin(t)
                val denom = 1f + sinT * sinT
                x = (R * 1.5f * cosT) / denom
                y = (R * 1.5f * sinT * cosT) / denom
            }
            "star" -> {
                val r = R * (0.65f + 0.35f * kotlin.math.cos(5f * t))
                x = r * kotlin.math.cos(t)
                y = r * kotlin.math.sin(t)
            }
            "flower" -> {
                val r = R * kotlin.math.cos(5f * t)
                x = r * kotlin.math.cos(t)
                y = r * kotlin.math.sin(t)
            }
            "clover" -> {
                val r = R * kotlin.math.cos(2f * t)
                x = r * kotlin.math.cos(t)
                y = r * kotlin.math.sin(t)
            }
            "butterfly" -> {
                val sinT = kotlin.math.sin(t)
                val cosT = kotlin.math.cos(t)
                val expCos = kotlin.math.exp(cosT.toDouble()).toFloat()
                val sin12Double = kotlin.math.sin(t / 12f).toDouble()
                val r = R * (expCos - 2f * kotlin.math.cos(4f * t) - java.lang.Math.pow(sin12Double, 5.0).toFloat()) / 4f
                x = r * sinT
                y = r * cosT
            }
            "crown" -> {
                val normT = (t % (2f * MathUtils.PI))
                if (normT > MathUtils.PI) {
                    val u = (normT - MathUtils.PI) / MathUtils.PI
                    x = R * 0.9f * (1f - 2f * u)
                    y = -R * 0.45f
                } else {
                    val u = normT / MathUtils.PI
                    x = R * 0.9f * (2f * u - 1f)
                    val spikeHeight = kotlin.math.abs(kotlin.math.cos(3.5f * MathUtils.PI * u))
                    val centerArch = 0.55f - 0.25f * (2f * u - 1f) * (2f * u - 1f)
                    y = -R * 0.45f + R * (centerArch + 0.45f * spikeHeight)
                }
            }
            "diamond" -> {
                val sinT = kotlin.math.sin(t)
                val cosT = kotlin.math.cos(t)
                x = R * 1.1f * cosT * cosT * cosT
                y = R * 1.1f * sinT * sinT * sinT
            }
        }
        return Pair(x, y)
    }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera, delta: Float) {
        shapePathAngle = (shapePathAngle + 0.35f * delta) % (2f * MathUtils.PI)
        val count = synchronized(shapePathIcons) { shapePathIcons.size }
        if (count == 0) return

        synchronized(shapePathIcons) {
            val step = (2f * MathUtils.PI) / count
            val R = camera.viewportWidth * 0.35f
            val cx = 0f
            val cy = 0f

            for (i in 0 until count) {
                val t = shapePathAngle + i * step
                val (x, y) = computeShapeCoordinates(t, R)
                drawIcon(batch, shapePathIcons[i], cx + x, cy + y)
            }
        }
    }

    override fun checkTouch(touchPoint: Vector3, camera: OrthographicCamera): GameRenderer.IconData? {
        val count = synchronized(shapePathIcons) { shapePathIcons.size }
        if (count == 0) return null

        synchronized(shapePathIcons) {
            val step = (2f * MathUtils.PI) / count
            val R = camera.viewportWidth * 0.35f
            val cx = 0f
            val cy = 0f

            for (i in 0 until count) {
                val t = shapePathAngle + i * step
                val (ix, iy) = computeShapeCoordinates(t, R)
                val actualX = cx + ix
                val actualY = cy + iy
                val size = shapePathIcons[i].size
                if (touchPoint.x >= actualX - size / 2f && touchPoint.x <= actualX + size / 2f &&
                    touchPoint.y >= actualY - size / 2f && touchPoint.y <= actualY + size / 2f
                ) {
                    return shapePathIcons[i]
                }
            }
        }
        return null
    }

    private fun drawIcon(batch: SpriteBatch, icon: GameRenderer.IconData, x: Float, y: Float) {
        val size = icon.size
        batch.draw(icon.texture, x - size / 2f, y - size / 2f, size, size)
    }

    override fun dispose() {
        synchronized(shapePathIcons) {
            for (icon in shapePathIcons) {
                icon.texture.dispose()
            }
            shapePathIcons.clear()
        }
    }
}
