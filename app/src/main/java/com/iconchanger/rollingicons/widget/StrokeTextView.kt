package com.iconchanger.rollingicons.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView

class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeTextView: TextView? = null

    init {
        strokeTextView = TextView(context, attrs, defStyleAttr).apply {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            setTextColor(0xFF000000.toInt()) // Default black outline
        }
    }

    override fun setLayoutParams(params: android.view.ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        strokeTextView?.layoutParams = params
    }

    fun setStrokeColor(color: Int) {
        strokeTextView?.setTextColor(color)
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        strokeTextView?.paint?.strokeWidth = width
        invalidate()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        strokeTextView?.text = text
    }

    override fun setGravity(gravity: Int) {
        super.setGravity(gravity)
        strokeTextView?.gravity = gravity
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (strokeTextView?.text != text) {
            strokeTextView?.text = text
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        strokeTextView?.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        strokeTextView?.layout(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        strokeTextView?.draw(canvas)
        super.onDraw(canvas)
    }
}

