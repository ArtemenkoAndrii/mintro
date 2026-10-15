package com.mas.mobile.presentation.activity.converter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable


class TextDrawable(
    var context: Context,
    var text: String
) : Drawable() {
    private val paint = Paint()

    init {
        with(paint) {
            color = Color.GRAY
            textSize = 50f
            isAntiAlias = true
            isFakeBoldText = true
            style = Paint.Style.FILL
            textAlign = Paint.Align.LEFT
            setShadowLayer(6f, 0f, 0f, Color.WHITE)
        }
    }

    override fun draw(canvas: Canvas) {
        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top

        val x = bounds.centerX() - textWidth / 2
        val y = bounds.centerY() - (textHeight / 2 + fontMetrics.top)

        canvas.drawText(text, x, y, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}