package com.iconchanger.rollingicons.wallpaper

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.iconchanger.rollingicons.render.GameRenderer

class RollingWallpaperService : AndroidLiveWallpaperService() {

    override fun onCreateEngine(): Engine {
        return object : AndroidWallpaperEngine() {
            override fun onPause() {
                try {
                    if (app != null) {
                        super.onPause()
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            override fun onResume() {
                try {
                    if (app != null) {
                        super.onResume()
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            override fun onVisibilityChanged(visible: Boolean) {
                try {
                    if (app != null) {
                        super.onVisibilityChanged(visible)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            override fun onDestroy() {
                try {
                    super.onDestroy()
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    override fun onCreateApplication() {
        super.onCreateApplication()
        
        val config = AndroidApplicationConfiguration().apply {
            getTouchEventsForLiveWallpaper = true
            useAccelerometer = true
            // Hỗ trợ RGBA8888 để nền suốt hoặc mượt
            r = 8
            g = 8
            b = 8
            a = 8
        }
        
        try {
            val renderer = GameRenderer(applicationContext)
            initialize(renderer, config)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

