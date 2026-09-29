package com.iconchanger.rollingicons.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferenceRepository(private val context: Context) {

    companion object {
        var isPendingWallpaperSuccess: Boolean = false
        val KEY_ICON_SIZE = floatPreferencesKey("icon_size")
        val KEY_DENSITY = floatPreferencesKey("density")
        val KEY_FRICTION = floatPreferencesKey("friction")
        val KEY_RESTITUTION = floatPreferencesKey("restitution")
        val KEY_ENABLE_SENSOR = booleanPreferencesKey("enable_sensor")
        val KEY_FPS_LIMIT = intPreferencesKey("fps_limit")

        // Cấu hình Nền
        val KEY_BG_TYPE = intPreferencesKey("bg_type")
        val KEY_BG_COLOR = stringPreferencesKey("bg_color")
        val KEY_BG_GRADIENT_START = stringPreferencesKey("bg_gradient_start")
        val KEY_BG_GRADIENT_END = stringPreferencesKey("bg_gradient_end")
        val KEY_BG_IMAGE_PATH = stringPreferencesKey("bg_image_path")
        val KEY_BG_VIDEO_PATH = stringPreferencesKey("bg_video_path")

        // Bộ sưu tập ảnh cá nhân chọn làm icon
        val KEY_SELECTED_PHOTOS = stringSetPreferencesKey("selected_photo_uris")

        // Bộ sưu tập Emoji chọn làm icon
        val KEY_SELECTED_EMOJIS = stringSetPreferencesKey("selected_emojis")
        val KEY_EMOJI_APP_BINDINGS = stringSetPreferencesKey("emoji_app_bindings")

        // Cấu hình float button và wallpaper touch
        val KEY_ENABLE_FLOAT_BUTTON = booleanPreferencesKey("enable_float_button")
        val KEY_ENABLE_WALLPAPER_TOUCH = booleanPreferencesKey("enable_wallpaper_touch")

        // Cấu hình Spinning Icon
        val KEY_WALLPAPER_MODE = stringPreferencesKey("wallpaper_mode")
        val KEY_SPINNING_PATTERN = stringPreferencesKey("spinning_pattern")
        val KEY_SELECTED_APPS_SPINNING = stringSetPreferencesKey("selected_apps_spinning")
        val KEY_SELECTED_PHOTOS_SPINNING = stringSetPreferencesKey("selected_photos_spinning")
        val KEY_SELECTED_EMOJIS_SPINNING = stringSetPreferencesKey("selected_emojis_spinning")
        val KEY_SPINNING_BG_IMAGE_PATH = stringPreferencesKey("spinning_bg_image_path")

        // Cấu hình Shape Path Icon
        val KEY_SHAPE_PATH_TYPE = stringPreferencesKey("shape_path_type")
        val KEY_SELECTED_APPS_SHAPE = stringSetPreferencesKey("selected_apps_shape")
        val KEY_SELECTED_PHOTOS_SHAPE = stringSetPreferencesKey("selected_photos_shape")
        val KEY_SELECTED_EMOJIS_SHAPE = stringSetPreferencesKey("selected_emojis_shape")
        val KEY_SHAPE_BG_IMAGE_PATH = stringPreferencesKey("shape_bg_image_path")
        val KEY_PREVIEW_BG_TYPE = intPreferencesKey("preview_bg_type")
        val KEY_PREVIEW_BG_IMAGE_PATH = stringPreferencesKey("preview_bg_image_path")
        val KEY_PREVIEW_BG_VIDEO_PATH = stringPreferencesKey("preview_bg_video_path")
    }

    suspend fun getIconSize(): Float = getValue(KEY_ICON_SIZE, 1.0f)
    suspend fun setIconSize(value: Float) = setValue(KEY_ICON_SIZE, value)

    suspend fun getDensity(): Float = getValue(KEY_DENSITY, 1.5f)
    suspend fun setDensity(value: Float) = setValue(KEY_DENSITY, value)

    suspend fun getFriction(): Float = getValue(KEY_FRICTION, 0.3f)
    suspend fun setFriction(value: Float) = setValue(KEY_FRICTION, value)

    suspend fun getRestitution(): Float = getValue(KEY_RESTITUTION, 0.5f)
    suspend fun setRestitution(value: Float) = setValue(KEY_RESTITUTION, value)

    suspend fun isSensorEnabled(): Boolean = getValue(KEY_ENABLE_SENSOR, true)
    suspend fun setSensorEnabled(value: Boolean) = setValue(KEY_ENABLE_SENSOR, value)

    suspend fun getFpsLimit(): Int = getValue(KEY_FPS_LIMIT, 60)
    suspend fun setFpsLimit(value: Int) = setValue(KEY_FPS_LIMIT, value)

    // Background Getters & Setters
    suspend fun getBgType(): Int = getValue(KEY_BG_TYPE, 0)
    suspend fun setBgType(value: Int) = setValue(KEY_BG_TYPE, value)

    suspend fun getBgColor(): String = getValue(KEY_BG_COLOR, "#121212")
    suspend fun setBgColor(value: String) = setValue(KEY_BG_COLOR, value)

    suspend fun getBgGradientStart(): String = getValue(KEY_BG_GRADIENT_START, "#1e272e")
    suspend fun setBgGradientStart(value: String) = setValue(KEY_BG_GRADIENT_START, value)

    suspend fun getBgGradientEnd(): String = getValue(KEY_BG_GRADIENT_END, "#0f0f10")
    suspend fun setBgGradientEnd(value: String) = setValue(KEY_BG_GRADIENT_END, value)

    suspend fun getBgImagePath(): String = getValue(KEY_BG_IMAGE_PATH, "")
    suspend fun setBgImagePath(value: String) = setValue(KEY_BG_IMAGE_PATH, value)

    suspend fun getBgVideoPath(): String = getValue(KEY_BG_VIDEO_PATH, "")
    suspend fun setBgVideoPath(value: String) = setValue(KEY_BG_VIDEO_PATH, value)

    // Preview Background Getters & Setters
    suspend fun getPreviewBgType(): Int = getValue(KEY_PREVIEW_BG_TYPE, -1)
    suspend fun setPreviewBgType(value: Int) = setValue(KEY_PREVIEW_BG_TYPE, value)

    suspend fun getPreviewBgImagePath(): String = getValue(KEY_PREVIEW_BG_IMAGE_PATH, "")
    suspend fun setPreviewBgImagePath(value: String) = setValue(KEY_PREVIEW_BG_IMAGE_PATH, value)

    suspend fun getPreviewBgVideoPath(): String = getValue(KEY_PREVIEW_BG_VIDEO_PATH, "")
    suspend fun setPreviewBgVideoPath(value: String) = setValue(KEY_PREVIEW_BG_VIDEO_PATH, value)

    suspend fun clearPreviewBg() {
        setValue(KEY_PREVIEW_BG_TYPE, -1)
        setValue(KEY_PREVIEW_BG_IMAGE_PATH, "")
        setValue(KEY_PREVIEW_BG_VIDEO_PATH, "")
    }

    // Selected Photos Set
    suspend fun getSelectedPhotos(): Set<String> = getValue(KEY_SELECTED_PHOTOS, emptySet())
    suspend fun setSelectedPhotos(uris: Set<String>) = setValue(KEY_SELECTED_PHOTOS, uris)

    // Selected Emojis Set
    suspend fun getSelectedEmojis(): Set<String> = getValue(KEY_SELECTED_EMOJIS, emptySet())
    suspend fun setSelectedEmojis(emojis: Set<String>) = setValue(KEY_SELECTED_EMOJIS, emojis)

    suspend fun getEmojiAppBindings(): Map<String, String> {
        val set = getValue(KEY_EMOJI_APP_BINDINGS, emptySet())
        val map = HashMap<String, String>()
        set.forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    suspend fun setEmojiAppBinding(emojiKey: String, packageName: String?) {
        val currentMap = getEmojiAppBindings().toMutableMap()
        if (packageName.isNullOrEmpty()) {
            currentMap.remove(emojiKey)
        } else {
            currentMap[emojiKey] = packageName
        }
        val set = currentMap.map { "${it.key}=${it.value}" }.toSet()
        setValue(KEY_EMOJI_APP_BINDINGS, set)
    }

    suspend fun isFloatButtonEnabled(): Boolean = getValue(KEY_ENABLE_FLOAT_BUTTON, true)
    suspend fun setFloatButtonEnabled(value: Boolean) = setValue(KEY_ENABLE_FLOAT_BUTTON, value)

    suspend fun isWallpaperTouchEnabled(): Boolean = getValue(KEY_ENABLE_WALLPAPER_TOUCH, true)
    suspend fun setWallpaperTouchEnabled(value: Boolean) = setValue(KEY_ENABLE_WALLPAPER_TOUCH, value)

    suspend fun getWallpaperMode(): String = getValue(KEY_WALLPAPER_MODE, "rolling")
    suspend fun setWallpaperMode(value: String) = setValue(KEY_WALLPAPER_MODE, value)

    suspend fun getSpinningPattern(): String = getValue(KEY_SPINNING_PATTERN, "dual_circle")
    suspend fun setSpinningPattern(value: String) = setValue(KEY_SPINNING_PATTERN, value)

    suspend fun getSelectedAppsSpinning(): Set<String> = getValue(KEY_SELECTED_APPS_SPINNING, emptySet())
    suspend fun setSelectedAppsSpinning(values: Set<String>) = setValue(KEY_SELECTED_APPS_SPINNING, values)

    suspend fun getSelectedPhotosSpinning(): Set<String> = getValue(KEY_SELECTED_PHOTOS_SPINNING, emptySet())
    suspend fun setSelectedPhotosSpinning(values: Set<String>) = setValue(KEY_SELECTED_PHOTOS_SPINNING, values)

    suspend fun getSelectedEmojisSpinning(): Set<String> = getValue(KEY_SELECTED_EMOJIS_SPINNING, emptySet())
    suspend fun setSelectedEmojisSpinning(values: Set<String>) = setValue(KEY_SELECTED_EMOJIS_SPINNING, values)

    suspend fun getSpinningBgImagePath(): String = getValue(KEY_SPINNING_BG_IMAGE_PATH, "")
    suspend fun setSpinningBgImagePath(value: String) = setValue(KEY_SPINNING_BG_IMAGE_PATH, value)

    suspend fun getShapePathType(): String = getValue(KEY_SHAPE_PATH_TYPE, "heart")
    suspend fun setShapePathType(value: String) = setValue(KEY_SHAPE_PATH_TYPE, value)

    suspend fun getSelectedAppsShape(): Set<String> = getValue(KEY_SELECTED_APPS_SHAPE, emptySet())
    suspend fun setSelectedAppsShape(values: Set<String>) = setValue(KEY_SELECTED_APPS_SHAPE, values)

    suspend fun getSelectedPhotosShape(): Set<String> = getValue(KEY_SELECTED_PHOTOS_SHAPE, emptySet())
    suspend fun setSelectedPhotosShape(values: Set<String>) = setValue(KEY_SELECTED_PHOTOS_SHAPE, values)

    suspend fun getSelectedEmojisShape(): Set<String> = getValue(KEY_SELECTED_EMOJIS_SHAPE, emptySet())
    suspend fun setSelectedEmojisShape(values: Set<String>) = setValue(KEY_SELECTED_EMOJIS_SHAPE, values)

    suspend fun getShapeBgImagePath(): String = getValue(KEY_SHAPE_BG_IMAGE_PATH, "")
    suspend fun setShapeBgImagePath(value: String) = setValue(KEY_SHAPE_BG_IMAGE_PATH, value)

    private suspend fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): T {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }.first() ?: defaultValue
    }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}

