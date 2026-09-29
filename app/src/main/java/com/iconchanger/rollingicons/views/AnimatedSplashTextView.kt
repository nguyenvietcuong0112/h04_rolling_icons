package com.iconchanger.rollingicons.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatTextView

class AnimatedSplashTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTextAnimation()
    }

    private fun startTextAnimation() {
        alpha = 0f
        translationY = 30f

        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 200
        }
        val slideUp = ObjectAnimator.ofFloat(this, "translationY", 30f, 0f).apply {
            duration = 600
            startDelay = 200
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(fadeIn, slideUp)
            start()
        }
    }
}
