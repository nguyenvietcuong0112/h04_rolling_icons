package com.iconchanger.rollingicons.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatImageView

class AnimatedSplashImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var currentAnimatorSet: AnimatorSet? = null
    private var shineAnimator: ValueAnimator? = null

    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private var shineGradient: LinearGradient? = null
    private val shineMatrix = Matrix()
    private var shineOffset = 0f
    private var isShineActive = false

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val shineWidth = w * 0.45f
            shineGradient = LinearGradient(
                0f, 0f, shineWidth, shineWidth,
                intArrayOf(
                    0x00FFFFFF,
                    0x50FFFFFF,
                    0xEDFFFFFF.toInt(),
                    0x50FFFFFF,
                    0x00FFFFFF
                ),
                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            shinePaint.shader = shineGradient
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSplashAnimation()
    }

    override fun onDetachedFromWindow() {
        currentAnimatorSet?.cancel()
        shineAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun startSplashAnimation() {
        alpha = 0f
        scaleX = 0.4f
        scaleY = 0.4f
        rotation = -8f

        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            duration = 500
        }
        val scaleXIn = ObjectAnimator.ofFloat(this, "scaleX", 0.4f, 1.0f).apply {
            duration = 650
            interpolator = OvershootInterpolator(2.0f)
        }
        val scaleYIn = ObjectAnimator.ofFloat(this, "scaleY", 0.4f, 1.0f).apply {
            duration = 650
            interpolator = OvershootInterpolator(2.0f)
        }
        val rotateIn = ObjectAnimator.ofFloat(this, "rotation", -8f, 0f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.5f)
        }

        val entranceSet = AnimatorSet().apply {
            playTogether(fadeIn, scaleXIn, scaleYIn, rotateIn)
        }

        val scaleXPulse = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 1.07f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleYPulse = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 1.07f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val floatPulse = ObjectAnimator.ofFloat(this, "translationY", 0f, -14f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pulseSet = AnimatorSet().apply {
            playTogether(scaleXPulse, scaleYPulse, floatPulse)
        }

        entranceSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                pulseSet.start()
                startShineSweepAnimation()
            }
        })

        currentAnimatorSet = entranceSet
        entranceSet.start()
    }

    private fun startShineSweepAnimation() {
        isShineActive = true
        shineAnimator = ValueAnimator.ofFloat(-1.5f, 2.5f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                shineOffset = fraction * width
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isShineActive && shineGradient != null && width > 0 && height > 0) {
            shineMatrix.reset()
            shineMatrix.setTranslate(shineOffset, shineOffset * 0.5f)
            shineGradient?.setLocalMatrix(shineMatrix)

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shinePaint)
        }
    }
}
