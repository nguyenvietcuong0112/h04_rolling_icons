package com.iconchanger.rollingicons.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout

class TextureVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoUri: Uri? = null

    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var isPrepared = false
    private var shouldPlayWhenPrepared = false
    private var hasFirstFrameRendered = false

    private var preparedListener: MediaPlayer.OnPreparedListener? = null
    private var errorListener: MediaPlayer.OnErrorListener? = null
    private var firstFrameListener: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        textureView.layoutParams = params
        textureView.surfaceTextureListener = this
        addView(textureView)
    }

    fun setVideoURI(uri: Uri) {
        videoUri = uri
        hasFirstFrameRendered = false
        openVideo()
    }

    fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener) {
        preparedListener = listener
    }

    fun setOnErrorListener(listener: MediaPlayer.OnErrorListener) {
        errorListener = listener
    }

    fun setOnFirstFrameListener(listener: (() -> Unit)?) {
        firstFrameListener = listener
    }

    private fun notifyFirstFrame() {
        if (!hasFirstFrameRendered) {
            hasFirstFrameRendered = true
            post {
                firstFrameListener?.invoke()
            }
        }
    }

    private fun openVideo() {
        val uri = videoUri ?: return
        val currentSurface = surface ?: return

        releaseMediaPlayer()

        try {
            isPrepared = false
            hasFirstFrameRendered = false
            mediaPlayer = MediaPlayer().apply {
                setSurface(currentSurface)
                setDataSource(context, uri)
                isLooping = true
                setVolume(0f, 0f)

                setOnVideoSizeChangedListener { mp, w, h ->
                    if (w > 0 && h > 0) {
                        this@TextureVideoView.mVideoWidth = w
                        this@TextureVideoView.mVideoHeight = h
                        adjustAspectRatio()
                    }
                }

                setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        notifyFirstFrame()
                    }
                    false
                }

                setOnPreparedListener { mp ->
                    isPrepared = true
                    if (this@TextureVideoView.mVideoWidth == 0 || this@TextureVideoView.mVideoHeight == 0) {
                        this@TextureVideoView.mVideoWidth = mp.videoWidth
                        this@TextureVideoView.mVideoHeight = mp.videoHeight
                    }
                    adjustAspectRatio()
                    preparedListener?.onPrepared(mp)
                    if (shouldPlayWhenPrepared) {
                        mp.start()
                    }
                }

                setOnErrorListener { mp, what, extra ->
                    errorListener?.onError(mp, what, extra) ?: false
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorListener?.onError(mediaPlayer, 1, 0)
        }
    }

    fun start() {
        shouldPlayWhenPrepared = true
        if (isPrepared) {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        shouldPlayWhenPrepared = false
        if (isPrepared) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopPlayback() {
        shouldPlayWhenPrepared = false
        hasFirstFrameRendered = false
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPrepared = false
    }

    private fun adjustAspectRatio() {
        val viewWidth = width
        val viewHeight = height
        if (mVideoWidth <= 0 || mVideoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

        val scaleX: Float
        val scaleY: Float

        val videoRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()

        if (videoRatio > viewRatio) {
            // Video is wider than view: fit height, crop horizontal sides
            scaleY = 1f
            scaleX = (mVideoWidth.toFloat() / viewWidth.toFloat()) * (viewHeight.toFloat() / mVideoHeight.toFloat())
        } else {
            // Video is taller than view: fit width, crop vertical ends
            scaleX = 1f
            scaleY = (mVideoHeight.toFloat() / viewHeight.toFloat()) * (viewWidth.toFloat() / mVideoWidth.toFloat())
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, (viewWidth / 2).toFloat(), (viewHeight / 2).toFloat())
        textureView.setTransform(matrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustAspectRatio()
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(surfaceTexture)
        if (videoUri != null) {
            openVideo()
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        adjustAspectRatio()
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        surface?.release()
        surface = null
        releaseMediaPlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        if (isPlaying) {
            notifyFirstFrame()
        }
    }
}
