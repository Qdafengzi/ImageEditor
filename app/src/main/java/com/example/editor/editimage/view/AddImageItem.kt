package com.example.editor.editimage.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.editor.R
import com.example.editor.editimage.utils.RectUtil
import kotlin.math.acos
import kotlin.math.min
import kotlin.math.sqrt

class AddImageItem(context: Context) {
    var bitmap: Bitmap? = null
    private lateinit var srcRect: Rect // 原始图片坐标
    private lateinit var dstRect: RectF // 绘制目标坐标
    private var helpToolsRect: Rect? = null
    private lateinit var deleteRect: RectF// 删除按钮位置
    private lateinit var rotateRect: RectF// 旋转按钮位置
    private lateinit var scaleRect: RectF// 旋转按钮位置
    lateinit var helpBox: RectF
    private lateinit var matrix: Matrix// 变化矩阵
    var rotateAngle = 0f
    var isDrawHelpTool = false
    private val helpBoxPaint = Paint()
    private var initWidth = 0f // 加入屏幕时原始宽度
    lateinit var detectDeleteRect: RectF
    lateinit var detectRotateRect: RectF
    lateinit var detectScaleRect: RectF

    private var deleteBit: Bitmap
    private var rotateBit: Bitmap
    private var scaleBit: Bitmap

    init {
        helpBoxPaint.color = ContextCompat.getColor(context, R.color.rect_frame)
        helpBoxPaint.style = Paint.Style.STROKE
        helpBoxPaint.isAntiAlias = true
        helpBoxPaint.strokeWidth = 4f

        // 导入工具按钮位图
        deleteBit = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_delete
        )
        rotateBit = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_rotate
        )
        scaleBit = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_scale
        )
    }

    fun init(addBit: Bitmap, parentView: View) {
        bitmap = addBit
        srcRect = Rect(0, 0, addBit.width, addBit.height)
        val bitWidth = min(addBit.width, parentView.width shr 1)
        val bitHeight = bitWidth * addBit.height / addBit.width
        val left = (parentView.width shr 1) - (bitWidth shr 1)
        val top = (parentView.height shr 1) - (bitHeight shr 1)
        dstRect = RectF(left.toFloat(), top.toFloat(), (left + bitWidth).toFloat(), (top + bitHeight).toFloat())
        matrix = Matrix()
        matrix.postTranslate(dstRect.left, dstRect.top)
        matrix.postScale(
            bitWidth.toFloat() / addBit.width,
            bitHeight.toFloat() / addBit.height, dstRect.left,
            dstRect.top
        )
        initWidth = dstRect.width() // 记录原始宽度
        // item.matrix.setScale((float)bitWidth/addBit.getWidth(),
        // (float)bitHeight/addBit.getHeight());
        isDrawHelpTool = true
        helpBox = RectF(dstRect)
        updateHelpBoxRect()
        helpToolsRect = Rect(0, 0, deleteBit.width, deleteBit.height)
        deleteRect = RectF(
            helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.top
                    + BUTTON_WIDTH
        )
        rotateRect = RectF(
            helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.bottom
                    + BUTTON_WIDTH
        )
        scaleRect = RectF(
            helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH, helpBox.right + BUTTON_WIDTH, helpBox.bottom
                    + BUTTON_WIDTH
        )
        detectDeleteRect = RectF(deleteRect)
        detectRotateRect = RectF(rotateRect)
        detectScaleRect = RectF(scaleRect)
    }

    private fun updateHelpBoxRect() {
        helpBox.left -= HELP_BOX_PAD.toFloat()
        helpBox.right += HELP_BOX_PAD.toFloat()
        helpBox.top -= HELP_BOX_PAD.toFloat()
        helpBox.bottom += HELP_BOX_PAD.toFloat()
    }

    /**
     * 位置更新
     *
     * @param dx
     * @param dy
     */
    fun updatePos(dx: Float, dy: Float) {
        matrix.postTranslate(dx, dy) // 记录到矩阵中
        dstRect.offset(dx, dy)

        // 工具按钮随之移动
        helpBox.offset(dx, dy)
        deleteRect.offset(dx, dy)
        rotateRect.offset(dx, dy)
        scaleRect.offset(dx, dy)
        detectDeleteRect.offset(dx, dy)
        detectRotateRect.offset(dx, dy)
        detectScaleRect.offset(dx, dy)
    }

    fun updateRotate( dx: Float, dy: Float) {
        val cX = dstRect.centerX()
        val cY = dstRect.centerY()
        val x = detectScaleRect.centerX()
        val y = detectScaleRect.centerY()

        // float x = oldx;
        // float y = oldy;
        val nX = x + dx
        val nY = y + dy
        val xa = x - cX
        val ya = y - cY
        val xb = nX - cX
        val yb = nY - cY
        val srcLen = sqrt((xa * xa + ya * ya).toDouble()).toFloat()
        val curLen = sqrt((xb * xb + yb * yb).toDouble()).toFloat()

        // 重新计算工具箱坐标
        helpBox.set(dstRect)
        updateHelpBoxRect() // 重新计算
        deleteRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH
        )
        rotateRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        scaleRect.offsetTo(
            helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        detectDeleteRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH
        )
        detectRotateRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        detectScaleRect.offsetTo(
            helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        val cos = ((xa * xb + ya * yb) / (srcLen * curLen)).toDouble()
        if (cos > 1 || cos < -1) return
        var angle = Math.toDegrees(acos(cos)).toFloat()
        // System.out.println("angle--->" + angle);

        // 定理
        val calMatrix = xa * yb - xb * ya // 行列式计算 确定转动方向
        val flag = if (calMatrix > 0) 1 else -1
        angle *= flag

        // System.out.println("angle--->" + angle);
        rotateAngle += angle
        matrix.postRotate(
            angle, dstRect.centerX(),
            dstRect.centerY()
        )
        RectUtil.rotateRect(
            detectDeleteRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
        RectUtil.rotateRect(
            detectRotateRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
        Log.d("旋转", "======>$detectRotateRect")
        RectUtil.rotateRect(
            detectScaleRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
    }

    fun updateScale(dx: Float, dy: Float) {
        val cX = dstRect.centerX()
        val cY = dstRect.centerY()
        val x = detectScaleRect.centerX()
        val y = detectScaleRect.centerY()

        // float x = oldx;
        // float y = oldy;
        val nX = x + dx

        val nY = y + dy
        val xa = x - cX
        val ya = y - cY
        val xb = nX - cX
        val yb = nY - cY
        val srcLen = sqrt((xa * xa + ya * ya).toDouble()).toFloat()
        val curLen = sqrt((xb * xb + yb * yb).toDouble()).toFloat()

        // System.out.println("srcLen--->" + srcLen + "   curLen---->" +
        // curLen);
        val scale = curLen / srcLen // 计算缩放比
        val newWidth = dstRect.width() * scale
        if (newWidth / initWidth < MIN_SCALE) { // 最小缩放值检测
            return
        }
        matrix.postScale(
            scale, scale, dstRect.centerX(),
            dstRect.centerY()
        ) // 存入scale矩阵
        // this.matrix.postRotate(5, this.dstRect.centerX(),
        // this.dstRect.centerY());
        RectUtil.scaleRect(dstRect, scale) // 缩放目标矩形

        // 重新计算工具箱坐标
        helpBox.set(dstRect)
        updateHelpBoxRect() // 重新计算
        deleteRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH
        )
        rotateRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        scaleRect.offsetTo(
            helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        detectDeleteRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.top
                    - BUTTON_WIDTH
        )
        detectRotateRect.offsetTo(
            helpBox.left - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        detectScaleRect.offsetTo(
            helpBox.right - BUTTON_WIDTH, helpBox.bottom
                    - BUTTON_WIDTH
        )
        RectUtil.rotateRect(
            detectDeleteRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
        RectUtil.rotateRect(
            detectRotateRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
        Log.d("旋转", "======>$detectRotateRect")
        RectUtil.rotateRect(
            detectScaleRect, dstRect.centerX(),
            dstRect.centerY(), rotateAngle
        )
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap!!, matrix, null) // 贴图元素绘制
        if (isDrawHelpTool) { // 绘制辅助工具线
            canvas.save()
            canvas.rotate(rotateAngle, helpBox.centerX(), helpBox.centerY())
            canvas.drawRect(helpBox, helpBoxPaint)
            // 绘制工具按钮
            canvas.drawBitmap(deleteBit, helpToolsRect, deleteRect, null)
            Log.d("drawBitmap", "s=s==s=s=s==s=s=s=s==deleteRect:$deleteRect rotateRect:$rotateRect scaleRect:$scaleRect")
            canvas.drawBitmap(rotateBit, helpToolsRect, rotateRect, null)
            canvas.drawBitmap(scaleBit, helpToolsRect, scaleRect, null)
            canvas.restore()
            // canvas.drawRect(deleteRect, dstPaint);
            //canvas.drawRect(rotateRect, dstPaint);

            //debug
//             canvas.drawRect(detectRotateRect, debugPaint);
//             canvas.drawRect(detectDeleteRect, debugPaint);
//             canvas.drawRect(helpBox , debugPaint);
        }

        // detectRotateRect
    }

    companion object {
        private const val MIN_SCALE = 0.15f
        private const val HELP_BOX_PAD = 25
        private const val BUTTON_WIDTH = Constants.IMAGE_BTN_HALF_SIZE

    }
}
