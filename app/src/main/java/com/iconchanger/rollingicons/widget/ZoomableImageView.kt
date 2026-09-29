package com.iconchanger.rollingicons.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener,
    ScaleGestureDetector.OnScaleGestureListener {

    private var myMatrix = Matrix()
    private var mode = NONE

    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var m: FloatArray = FloatArray(9)

    private var viewWidth = 0
    private var viewHeight = 0
    private var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f

    private var scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private var doubleTapDetector: GestureDetector

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val CLICK = 3
    }

    init {
        super.setClickable(true)
        setOnTouchListener(this)
        scaleType = ScaleType.MATRIX
        
        doubleTapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val targetScale = if (saveScale > 1f) 1f else 2.5f
                val scaleFactor = targetScale / saveScale
                saveScale = targetScale
                myMatrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                fixTrans()
                imageMatrix = myMatrix
                invalidate()
                return true
            }
        })
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        doubleTapDetector.onTouchEvent(event)
        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                    myMatrix.postTranslate(fixTransX, fixTransY)
                    fixTrans()
                    last.set(curr.x, curr.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                mode = NONE
                val xDiff = Math.abs(curr.x - start.x).toInt()
                val yDiff = Math.abs(curr.y - start.y).toInt()
                if (xDiff < CLICK && yDiff < CLICK) {
                    performClick()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        imageMatrix = myMatrix
        invalidate()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        mode = ZOOM
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var mScaleFactor = detector.scaleFactor
        val origScale = saveScale
        saveScale *= mScaleFactor
        if (saveScale > maxScale) {
            saveScale = maxScale
            mScaleFactor = maxScale / origScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            mScaleFactor = minScale / origScale
        }

        if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
            myMatrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
        } else {
            myMatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
        }

        fixTrans()
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    private fun fixTrans() {
        myMatrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) {
            myMatrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        if (contentSize <= viewSize) return 0f
        return delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return

        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight

        val scaleX = viewWidth.toFloat() / w.toFloat()
        val scaleY = viewHeight.toFloat() / h.toFloat()
        val scale = Math.min(scaleX, scaleY)
        myMatrix.setScale(scale, scale)

        val redundantYSpace = viewHeight.toFloat() - scale * h.toFloat()
        val redundantXSpace = viewWidth.toFloat() - scale * w.toFloat()
        myMatrix.postTranslate(redundantXSpace / 2f, redundantYSpace / 2f)

        origWidth = viewWidth - redundantXSpace
        origHeight = viewHeight - redundantYSpace
        imageMatrix = myMatrix
    }
}

