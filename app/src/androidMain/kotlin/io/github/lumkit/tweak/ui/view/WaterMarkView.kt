package io.github.lumkit.tweak.ui.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import io.github.lumkit.tweak.R
import androidx.core.graphics.withRotation

class WaterMarkView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        const val DEFAULT_SEPARATOR = "///"
    }

    private val mPaint: Paint = Paint()
    private var mText: Array<String>? = null
    private var mImage: Bitmap? = null
    private var mDegrees: Int = -30
    private var mTextColor: Int = Color.parseColor("#33000000")
    private var mTextSize: Int = 42
    private var mDx: Int = 100
    private var mDy: Int = 240
    private var mAlign: Paint.Align = Paint.Align.CENTER
    private var textWidth = 0
    private var textHeight = 0

    init {
        attrs?.let {
            val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.WaterMarkView)
            mDegrees = typedArray.getInt(R.styleable.WaterMarkView_water_mark_degree, -30)
            val text = typedArray.getString(R.styleable.WaterMarkView_water_mark_text)
            val imageRes = typedArray.getResourceId(R.styleable.WaterMarkView_water_mark_image, R.mipmap.ic_tweak_logo_round)
            if (text != null) {
                mText = text.split(DEFAULT_SEPARATOR).toTypedArray()
            } else {
                // mImage = (getDrawableExt(imageRes) as BitmapDrawable).bitmap
            }
            mTextColor = typedArray.getColor(R.styleable.WaterMarkView_water_mark_textColor, Color.parseColor("#33000000"))
            mTextSize = typedArray.getDimensionPixelSize(R.styleable.WaterMarkView_water_mark_textSize, 42)
            mDx = typedArray.getDimensionPixelSize(R.styleable.WaterMarkView_water_mark_dx, 100)
            mDy = typedArray.getDimensionPixelSize(R.styleable.WaterMarkView_water_mark_dy, 240)
            val align = typedArray.getInt(R.styleable.WaterMarkView_water_mark_align, 1)
            mAlign = when (align) {
                0 -> Paint.Align.LEFT
                2 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            typedArray.recycle()
        }

        setBackgroundColor(Color.TRANSPARENT)
        mPaint.isAntiAlias = true
        mPaint.flags = Paint.ANTI_ALIAS_FLAG
        mPaint.color = mTextColor
        mPaint.textSize = mTextSize.toFloat()
        mPaint.textAlign = mAlign

        // Calculate text width and height
        if (mText != null && mText!!.isNotEmpty()) {
            for (s in mText!!) {
                val tvRect = Rect()
                mPaint.getTextBounds(s, 0, s.length, tvRect)
                textWidth = maxOf(textWidth, tvRect.width())
                textHeight += tvRect.height() + 10
            }
        }

        setText("水印测试")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mText != null && mText!!.isNotEmpty()) {
            val measuredWidth = measuredWidth
            val measuredHeight = measuredHeight
            if (measuredWidth == 0 || measuredHeight == 0) {
                return
            }
            val canvasLength = maxOf(measuredWidth, measuredHeight)

            canvas.withRotation(mDegrees.toFloat(), measuredWidth / 2f, measuredHeight / 2f) {
                var y = 0
                var odd = true
                while (y < canvasLength + textHeight) {
                    var x = if (odd) 0 else -(textWidth + mDx) / 2
                    while (x < canvasLength + textWidth) {
                        drawTexts(mText!!, mPaint, this, x, y)
                        x += textWidth + mDx
                    }
                    y += textHeight + mDy
                    odd = !odd
                }
            }
        } else {
            val measuredWidth = measuredWidth
            val measuredHeight = measuredHeight
            if (measuredWidth == 0 || measuredHeight == 0) {
                return
            }
            val canvasLength = maxOf(measuredWidth, measuredHeight)

            canvas.withRotation(mDegrees.toFloat(), measuredWidth / 2f, measuredHeight / 2f) {
                var y = 0
                var odd = true
                while (y < canvasLength) {
                    var x = if (odd) 0 else -mDx / 2
                    while (x < canvasLength) {
                        drawPhoto(mImage, mPaint, this, x, y)
                        x += mDx
                    }
                    y += mDy
                    odd = !odd
                }

            }
        }
    }

    private fun drawTexts(ss: Array<String>, paint: Paint, canvas: Canvas, x: Int, y: Int) {
        val fontMetrics = paint.fontMetrics
        val top = fontMetrics.top
        val bottom = fontMetrics.bottom
        val length = ss.size
        val total = (length - 1) * (bottom - top) + (fontMetrics.descent - fontMetrics.ascent)
        val offset = total / 2 - bottom
        for (i in ss.indices) {
            val yAxis = -(length - i - 1) * (bottom - top) + offset
            canvas.drawText(ss[i], x.toFloat(), (y + yAxis + 10).toFloat(), paint)
        }
    }

    private fun drawPhoto(image: Bitmap?, paint: Paint, canvas: Canvas, x: Int, y: Int) {
        val fontMetrics = paint.fontMetrics
        val top = fontMetrics.top
        val bottom = fontMetrics.bottom
        val total = (bottom - top) + (fontMetrics.descent - fontMetrics.ascent)
        val offset = total / 2 - bottom
        val yAxis = (bottom - top) + offset
        if (image != null) {
            canvas.drawBitmap(image, x.toFloat(), (y + yAxis + 10).toFloat(), paint)
        }
    }

    // Setters
    fun setDegrees(degrees: Int) {
        mDegrees = degrees
        postInvalidate()
    }

    fun setText(vararg text: String) {
        mText = text.toList().toTypedArray()
        textWidth = 0
        textHeight = 0
        if (mText?.isNotEmpty() == true) {
            for (s in mText!!) {
                val tvRect = Rect()
                mPaint.getTextBounds(s, 0, s.length, tvRect)
                textWidth = maxOf(textWidth, tvRect.width())
                textHeight += tvRect.height() + 10
            }
        }
        postInvalidate()
    }

    fun setPhoto(image: Int) {
        // mImage = (getDrawableExt(image) as BitmapDrawable).bitmap
        postInvalidate()
    }

    fun setDx(dx: Int) {
        mDx = dx
        postInvalidate()
    }

    fun setTextColor(textColor: Int) {
        mTextColor = textColor
        mPaint.color = mTextColor
        postInvalidate()
    }

    fun setTextSize(textSize: Int) {
        mTextSize = textSize
        mPaint.textSize = mTextSize.toFloat()
        postInvalidate()
    }

    fun setDy(dy: Int) {
        mDy = dy
        postInvalidate()
    }

    fun setAlign(align: Paint.Align) {
        mAlign = align
        postInvalidate()
    }
}
