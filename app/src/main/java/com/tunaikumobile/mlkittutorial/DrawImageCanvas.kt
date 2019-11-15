package com.tunaikumobile.mlkittutorial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View


/**
 * Created by Franz Andel on 2019-11-14.
 * Android Engineer
 */

class DrawImageCanvas(context: Context) : View(context) {
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.apply {
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeWidth = 5F
        }

        val left = left + (right - left) / 4F
        val top = top + (bottom - top) / 7F
        val right = right - (right - left) / 4F
        val bottom = bottom - (bottom - top) / 4F

        canvas.drawRect(left, top, right, bottom, paint)
    }
}