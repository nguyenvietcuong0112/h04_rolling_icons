package com.iconchanger.rollingicons.wallpaper

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class VideoWallpaperService : WallpaperService() {

    companion object {
        const val VIDEO_FILE_NAME = "current_live_wallpaper.mp4"
    }

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private var currentHolder: SurfaceHolder? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            playVideo(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                if (mediaPlayer == null && currentHolder != null) {
                    playVideo(currentHolder!!)
                } else {
                    try {
                        mediaPlayer?.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                try {
                    mediaPlayer?.pause()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            releasePlayer()
        }

        private fun playVideo(holder: SurfaceHolder) {
            releasePlayer()
            val videoFile = File(filesDir, VIDEO_FILE_NAME)
            if (!videoFile.exists() || videoFile.length() == 0L) {
                return
            }

            try {
                mediaPlayer = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, Uri.fromFile(videoFile))
                    isLooping = true
                    setVolume(0f, 0f) // Mute audio for live wallpaper
                    try {
                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    } catch (e: Exception) {
                        // ignore
                    }
                    setOnPreparedListener { mp ->
                        mp.start()
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun releasePlayer() {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // ignore
            } finally {
                mediaPlayer = null
            }
        }
    }
}

