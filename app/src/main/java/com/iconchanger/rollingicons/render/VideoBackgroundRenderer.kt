package com.iconchanger.rollingicons.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import java.io.File

class VideoBackgroundRenderer(private val context: Context) {

    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var mediaPlayer: MediaPlayer? = null
    private var shaderProgram: ShaderProgram? = null
    private var mesh: Mesh? = null
    private var isPrepared = false
    private var currentVideoPath: String = ""

    private val vertexShader = """
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        varying vec2 v_texCoords;
        void main() {
            v_texCoords = a_texCoord0;
            gl_Position = a_position;
        }
    """.trimIndent()

    private val fragmentShader = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 v_texCoords;
        uniform samplerExternalOES u_sampler;
        void main() {
            gl_FragColor = texture2D(u_sampler, v_texCoords);
        }
    """.trimIndent()

    fun initGL() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        surfaceTexture = SurfaceTexture(textureId)
        surface = Surface(surfaceTexture)

        ShaderProgram.pedantic = false
        shaderProgram = ShaderProgram(vertexShader, fragmentShader)

        mesh = Mesh(
            true, 4, 6,
            VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
        )

        val vertices = floatArrayOf(
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
             1f,  1f, 1f, 0f,
            -1f,  1f, 0f, 0f
        )
        val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

        mesh?.setVertices(vertices)
        mesh?.setIndices(indices)
    }

    fun setVideoPath(videoPath: String) {
        if (videoPath.isEmpty() || (videoPath == currentVideoPath && isPrepared)) return
        currentVideoPath = videoPath

        try {
            isPrepared = false
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            if (surface == null) return

            val file = File(videoPath)
            if (!file.exists() && !videoPath.startsWith("http")) return

            mediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                if (file.exists()) {
                    setDataSource(context, Uri.fromFile(file))
                } else {
                    setDataSource(videoPath)
                }
                isLooping = true
                setVolume(0f, 0f)
                try {
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                } catch (e: Exception) {
                    // ignore
                }
                setOnPreparedListener { mp ->
                    isPrepared = true
                    mp.start()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun render(): Boolean {
        if (!isPrepared || surfaceTexture == null || shaderProgram == null || mesh == null) {
            return false
        }

        try {
            surfaceTexture?.updateTexImage()

            GLES20.glDisable(GL20.GL_DEPTH_TEST)
            GLES20.glDisable(GL20.GL_BLEND)

            shaderProgram?.begin()
            GLES20.glActiveTexture(GL20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            shaderProgram?.setUniformi("u_sampler", 0)

            mesh?.render(shaderProgram, GL20.GL_TRIANGLES)
            shaderProgram?.end()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun onPause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun onResume() {
        try {
            if (isPrepared) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun dispose() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null

        surface?.release()
        surface = null

        surfaceTexture?.release()
        surfaceTexture = null

        shaderProgram?.dispose()
        shaderProgram = null

        mesh?.dispose()
        mesh = null

        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }
        isPrepared = false
        currentVideoPath = ""
    }
}

