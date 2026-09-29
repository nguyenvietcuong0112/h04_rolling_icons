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

class SpinningModeStrategy(
    private val context: Context,
    private val scope: CoroutineScope
) : WallpaperModeStrategy {

    private val spinningIcons = ArrayList<GameRenderer.IconData>()
    private var spinningAngle = 0f
    private var spinningPattern = "dual_circle"

    override fun reload() {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val loader = IconLoader(context)
            val prefs = PreferenceRepository(context)

            spinningPattern = prefs.getSpinningPattern()
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

                synchronized(spinningIcons) {
                    for (icon in spinningIcons) {
                        icon.texture.dispose()
                    }
                    spinningIcons.clear()
                    spinningIcons.addAll(newIcons)
                }
            }
        }
    }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera, delta: Float) {
        spinningAngle = (spinningAngle + 0.45f * delta) % (2f * MathUtils.PI)
        val count = synchronized(spinningIcons) { spinningIcons.size }
        if (count == 0) return

        synchronized(spinningIcons) {
            val cx = 0f
            val cy = 0f
            val baseR = camera.viewportWidth * 0.32f

            when (spinningPattern) {
                "single_circle" -> {
                    val step = (2f * MathUtils.PI) / count
                    for (i in 0 until count) {
                        val angle = spinningAngle + i * step
                        val x = cx + baseR * MathUtils.cos(angle)
                        val y = cy + baseR * MathUtils.sin(angle)
                        drawSpinningIcon(batch, spinningIcons[i], x, y)
                    }
                }

                "vortex" -> {
                    val totalTurns = MathUtils.PI * 5f
                    for (i in 0 until count) {
                        val progress = i.toFloat() / count.toFloat()
                        val factor = 0.38f + 1.12f * progress
                        val r = baseR * factor
                        val angle = spinningAngle * (1f + 0.6f * progress) + progress * totalTurns
                        val x = cx + r * MathUtils.cos(angle)
                        val y = cy + r * MathUtils.sin(angle)
                        drawSpinningIcon(batch, spinningIcons[i], x, y)
                    }
                }

                else -> { // dual_circle
                    val half = count / 2
                    val outerCount = if (half > 0) count - half else count
                    val innerCount = half

                    val outerR = baseR * 1.15f
                    val innerR = baseR * 0.65f

                    if (outerCount > 0) {
                        val stepOuter = (2f * MathUtils.PI) / outerCount
                        for (i in 0 until outerCount) {
                            val angle = spinningAngle + i * stepOuter
                            val x = cx + outerR * MathUtils.cos(angle)
                            val y = cy + outerR * MathUtils.sin(angle)
                            drawSpinningIcon(batch, spinningIcons[i], x, y)
                        }
                    }

                    if (innerCount > 0) {
                        val stepInner = (2f * MathUtils.PI) / innerCount
                        for (i in 0 until innerCount) {
                            val angle = -spinningAngle * 1.3f + i * stepInner
                            val x = cx + innerR * MathUtils.cos(angle)
                            val y = cy + innerR * MathUtils.sin(angle)
                            drawSpinningIcon(batch, spinningIcons[outerCount + i], x, y)
                        }
                    }
                }
            }
        }
    }

    override fun checkTouch(touchPoint: Vector3, camera: OrthographicCamera): GameRenderer.IconData? {
        val count = synchronized(spinningIcons) { spinningIcons.size }
        if (count == 0) return null

        synchronized(spinningIcons) {
            val cx = 0f
            val cy = 0f
            val baseR = camera.viewportWidth * 0.32f

            when (spinningPattern) {
                "single_circle" -> {
                    val step = (2f * MathUtils.PI) / count
                    for (i in 0 until count) {
                        val angle = spinningAngle + i * step
                        val x = cx + baseR * MathUtils.cos(angle)
                        val y = cy + baseR * MathUtils.sin(angle)
                        val size = spinningIcons[i].size
                        if (touchPoint.x >= x - size / 2f && touchPoint.x <= x + size / 2f &&
                            touchPoint.y >= y - size / 2f && touchPoint.y <= y + size / 2f
                        ) {
                            return spinningIcons[i]
                        }
                    }
                }

                "vortex" -> {
                    val totalTurns = MathUtils.PI * 5f
                    for (i in 0 until count) {
                        val progress = i.toFloat() / count.toFloat()
                        val factor = 0.38f + 1.12f * progress
                        val r = baseR * factor
                        val angle = spinningAngle * (1f + 0.6f * progress) + progress * totalTurns
                        val x = cx + r * MathUtils.cos(angle)
                        val y = cy + r * MathUtils.sin(angle)
                        val size = spinningIcons[i].size
                        if (touchPoint.x >= x - size / 2f && touchPoint.x <= x + size / 2f &&
                            touchPoint.y >= y - size / 2f && touchPoint.y <= y + size / 2f
                        ) {
                            return spinningIcons[i]
                        }
                    }
                }

                else -> { // dual_circle
                    val half = count / 2
                    val outerCount = if (half > 0) count - half else count
                    val innerCount = half
                    val outerR = baseR * 1.15f
                    val innerR = baseR * 0.65f

                    if (outerCount > 0) {
                        val stepOuter = (2f * MathUtils.PI) / outerCount
                        for (i in 0 until outerCount) {
                            val angle = spinningAngle + i * stepOuter
                            val x = cx + outerR * MathUtils.cos(angle)
                            val y = cy + outerR * MathUtils.sin(angle)
                            val size = spinningIcons[i].size
                            if (touchPoint.x >= x - size / 2f && touchPoint.x <= x + size / 2f &&
                                touchPoint.y >= y - size / 2f && touchPoint.y <= y + size / 2f
                            ) {
                                return spinningIcons[i]
                            }
                        }
                    }

                    if (innerCount > 0) {
                        val stepInner = (2f * MathUtils.PI) / innerCount
                        for (i in 0 until innerCount) {
                            val angle = -spinningAngle * 1.3f + i * stepInner
                            val x = cx + innerR * MathUtils.cos(angle)
                            val y = cy + innerR * MathUtils.sin(angle)
                            val size = spinningIcons[outerCount + i].size
                            if (touchPoint.x >= x - size / 2f && touchPoint.x <= x + size / 2f &&
                                touchPoint.y >= y - size / 2f && touchPoint.y <= y + size / 2f
                            ) {
                                return spinningIcons[outerCount + i]
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun drawSpinningIcon(batch: SpriteBatch, icon: GameRenderer.IconData, x: Float, y: Float) {
        val size = icon.size
        batch.draw(icon.texture, x - size / 2f, y - size / 2f, size, size)
    }

    override fun dispose() {
        synchronized(spinningIcons) {
            for (icon in spinningIcons) {
                icon.texture.dispose()
            }
            spinningIcons.clear()
        }
    }
}
