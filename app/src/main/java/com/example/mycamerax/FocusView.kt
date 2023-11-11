package com.example.mycamerax

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

/**
 * 聚焦框
 */
class FocusView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var focusRect: RectF? = null
    private val path = Path()

    private val paint = Paint().apply {
        color = Color.parseColor("#FFA500") // 设置为橙色
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    // 设置焦点框的位置
    fun setFocusRect(left: Float, top: Float, right: Float, bottom: Float) {
        focusRect = RectF(left, top, right, bottom)
        updatePath()
        invalidate()
    }

    private fun updatePath() {
        focusRect?.let {
            path.reset()
            val cornerSize = 50f // 设置小直角的大小
            // 绘制四个角是小直角的路径
            path.moveTo(it.left, it.top + cornerSize)
            path.lineTo(it.left, it.top)
            path.lineTo(it.left + cornerSize, it.top)

            path.moveTo(it.right - cornerSize, it.top)
            path.lineTo(it.right, it.top)
            path.lineTo(it.right, it.top + cornerSize)

            path.moveTo(it.right, it.bottom - cornerSize)
            path.lineTo(it.right, it.bottom)
            path.lineTo(it.right - cornerSize, it.bottom)

            path.moveTo(it.left + cornerSize, it.bottom)
            path.lineTo(it.left, it.bottom)
            path.lineTo(it.left, it.bottom - cornerSize)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制焦点框
        focusRect?.let {
            canvas.drawPath(path, paint)
        }
    }

    // 变暗或取消焦点框
    fun cancelFocusRect() {
        // 使用 Handler 延迟执行变暗或取消焦点框的操作
        Handler(Looper.getMainLooper()).postDelayed({
            focusRect = null // 取消焦点框
            invalidate() // 重绘视图
        }, 2000) // 两秒后执行
    }

    // 变暗焦点框
    fun dimFocusRect() {
        val animator = ValueAnimator.ofInt(255, 100) // 从完全不透明到半透明
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            paint.alpha = value
            invalidate() // 重绘视图
        }
        animator.duration = 2000 // 两秒
        animator.start()
    }
}
