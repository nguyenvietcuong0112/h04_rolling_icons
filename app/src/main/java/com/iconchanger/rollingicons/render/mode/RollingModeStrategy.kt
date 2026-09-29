package com.iconchanger.rollingicons.render.mode

import android.content.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.QueryCallback
import com.badlogic.gdx.physics.box2d.joints.MouseJoint
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.data.IconLoader
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.model.AppInfo
import com.iconchanger.rollingicons.physics.PhysicsWorld
import com.iconchanger.rollingicons.render.GameRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class RollingModeStrategy(
    private val context: Context,
    private val scope: CoroutineScope,
    private val physicsWorld: PhysicsWorld
) : WallpaperModeStrategy {

    private val activeBodies = ArrayList<Body>()
    private var currentIconSize = 1.0f
    private var mouseJoint: MouseJoint? = null

    override fun reload() {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val prefs = PreferenceRepository(context)

            val newSize = prefs.getIconSize()
            val newDensity = prefs.getDensity()
            val newFriction = prefs.getFriction()
            val newRestitution = prefs.getRestitution()

            val selectedApps = repository.getSelectedApps()
            val selectedPhotos = prefs.getSelectedPhotos()
            val selectedEmojis = prefs.getSelectedEmojis()

            val newKeys = selectedApps.map { it.packageName }.toSet() + selectedPhotos + selectedEmojis.map { if (it.startsWith("emoji_")) it else "emoji_$it" }

            Gdx.app.postRunnable {
                val currentKeys = synchronized(activeBodies) {
                    activeBodies.mapNotNull { (it.userData as? GameRenderer.IconData)?.appInfo?.packageName }.toSet()
                }

                val listChanged = currentKeys != newKeys
                val sizeChanged = abs(currentIconSize - newSize) > 0.01f

                if (sizeChanged || listChanged) {
                    currentIconSize = newSize
                    reloadAllIcons(newDensity, newFriction, newRestitution)
                } else {
                    synchronized(activeBodies) {
                        for (body in activeBodies) {
                            val fixtures = body.fixtureList
                            for (fixture in fixtures) {
                                fixture.density = newDensity
                                fixture.friction = newFriction
                                fixture.restitution = newRestitution
                            }
                            body.resetMassData()
                        }
                    }
                }
            }
        }
    }

    private fun reloadAllIcons(density: Float, friction: Float, restitution: Float) {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val loader = IconLoader(context)
            val prefs = PreferenceRepository(context)

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
                synchronized(activeBodies) {
                    for (body in activeBodies) {
                        val data = body.userData as? GameRenderer.IconData
                        data?.texture?.dispose()
                        try {
                            if (!physicsWorld.world.isLocked) {
                                physicsWorld.world.destroyBody(body)
                            }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    activeBodies.clear()

                    val baseSize = 1.2f * currentIconSize
                    val spanX = 6.0f
                    val startX = -spanX / 2.0f
                    var currentX = startX
                    var currentY = 3.0f

                    // Apps
                    appBitmaps.forEach { (app, bitmap) ->
                        val texture = loader.bitmapToTexture(bitmap)
                        val iconData = GameRenderer.IconData(texture, baseSize, app)

                        val body = physicsWorld.createIconBody(
                            x = currentX,
                            y = currentY,
                            size = baseSize,
                            userData = iconData,
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        activeBodies.add(body)

                        currentX += baseSize + 0.3f
                        if (currentX > spanX / 2.0f) {
                            currentX = startX
                            currentY += baseSize + 0.3f
                        }
                    }

                    // Photos
                    photoBitmaps.forEach { (photoUri, bitmap) ->
                        val texture = loader.bitmapToTexture(bitmap)
                        val photoApp = AppInfo(
                            packageName = photoUri,
                            appName = "Photo",
                            iconCacheKey = photoUri,
                            type = AppInfo.TYPE_PHOTO
                        )
                        val iconData = GameRenderer.IconData(texture, baseSize, photoApp)

                        val body = physicsWorld.createIconBody(
                            x = currentX,
                            y = currentY,
                            size = baseSize,
                            userData = iconData,
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        activeBodies.add(body)

                        currentX += baseSize + 0.3f
                        if (currentX > spanX / 2.0f) {
                            currentX = startX
                            currentY += baseSize + 0.3f
                        }
                    }

                    // Emojis
                    emojiBitmaps.forEach { (emoji, bitmap) ->
                        val texture = loader.bitmapToTexture(bitmap)
                        val emojiApp = AppInfo(
                            packageName = emoji,
                            appName = "Emoji",
                            iconCacheKey = emoji,
                            type = AppInfo.TYPE_EMOJI
                        )
                        val iconData = GameRenderer.IconData(texture, baseSize, emojiApp)

                        val body = physicsWorld.createIconBody(
                            x = currentX,
                            y = currentY,
                            size = baseSize,
                            userData = iconData,
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        activeBodies.add(body)

                        currentX += baseSize + 0.3f
                        if (currentX > spanX / 2.0f) {
                            currentX = startX
                            currentY += baseSize + 0.3f
                        }
                    }
                }
            }
        }
    }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera, delta: Float) {
        synchronized(activeBodies) {
            val iterator = activeBodies.iterator()
            while (iterator.hasNext()) {
                val body = iterator.next()
                val pos = body.position
                val angle = body.angle * MathUtils.radiansToDegrees
                val iconData = body.userData as? GameRenderer.IconData

                if (iconData != null) {
                    val size = iconData.size
                    batch.draw(
                        iconData.texture,
                        pos.x - size / 2f,
                        pos.y - size / 2f,
                        size / 2f,
                        size / 2f,
                        size,
                        size,
                        1f,
                        1f,
                        angle,
                        0,
                        0,
                        iconData.texture.width,
                        iconData.texture.height,
                        false,
                        false
                    )
                }
            }
        }
    }

    override fun checkTouch(touchPoint: Vector3, camera: OrthographicCamera): GameRenderer.IconData? {
        var clickedIcon: GameRenderer.IconData? = null
        physicsWorld.world.QueryAABB(object : QueryCallback {
            override fun reportFixture(fixture: Fixture): Boolean {
                if (fixture.testPoint(touchPoint.x, touchPoint.y)) {
                    val body = fixture.body
                    if (body.userData is GameRenderer.IconData) {
                        clickedIcon = body.userData as GameRenderer.IconData
                        return false
                    }
                }
                return true
            }
        }, touchPoint.x - 0.1f, touchPoint.y - 0.1f, touchPoint.x + 0.1f, touchPoint.y + 0.1f)
        return clickedIcon
    }

    fun resize(viewportWidth: Float, viewportHeight: Float) {
        val halfW = viewportWidth / 2f
        val halfH = viewportHeight / 2f
        synchronized(activeBodies) {
            for (body in activeBodies) {
                val pos = body.position
                val iconData = body.userData as? GameRenderer.IconData
                val size = iconData?.size ?: 1.0f
                val padding = size / 2f + 0.2f
                val clampX = pos.x.coerceIn(-halfW + padding, halfW - padding)
                val clampY = pos.y.coerceIn(-halfH + padding, halfH - padding)
                body.setTransform(clampX, clampY, body.angle)
                body.isAwake = true
            }
        }
    }

    override fun dispose() {
        synchronized(activeBodies) {
            for (body in activeBodies) {
                val data = body.userData as? GameRenderer.IconData
                data?.texture?.dispose()
                try {
                    if (!physicsWorld.world.isLocked) {
                        physicsWorld.world.destroyBody(body)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            activeBodies.clear()
        }
    }
}
