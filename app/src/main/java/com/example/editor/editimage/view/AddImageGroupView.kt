package com.example.editor.editimage.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.editor.R
import com.example.editor.XLogger.d
import com.example.editor.editimage.utils.RectUtil

class AddImageGroupView : View {
    private var imageCount = 0 // 已加入照片的数量
    private var currentStatus = 0 // 当前状态
    private var currentItem: AddImageItem? = null // 当前操作的贴图数据
    private var oldx = 0f
    private var oldy = 0f
    private var shaderRec = RectF()
    private val shaderPaint = Paint()
    private val imageQueue = LinkedHashMap<Int, AddImageItem>() // 存贮每层贴图数据
    private val mPoint = Point(0, 0)

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        currentStatus = STATUS_IDLE
    }

   private var screenPath = Path()
   private var cropPath = Path()
   private var combinedPath = Path()
    fun setRootImageRect(bitmapRect: RectF, width: Int, height: Int) {
        shaderPaint.color = ContextCompat.getColor(this.context, R.color.white)
        shaderRec = bitmapRect
        screenPath.addRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), Path.Direction.CCW)
        val cropRect = RectF(
            shaderRec.left,
            shaderRec.top,
            shaderRec.right,
            shaderRec.bottom
        )
        cropPath.addRect(cropRect, Path.Direction.CW)
    }

    fun addBitImage(addBit: Bitmap) {
        d(" addBitImage getWidth:" + addBit.width + " getHeight:" + addBit.height + "currentItem:" + currentItem)
        val addImageItem = AddImageItem(this.context)
        addImageItem.init(addBit, this)
        if (currentItem != null) {
            currentItem!!.isDrawHelpTool = false
        }
        imageCount += 1
        imageQueue[imageCount] = addImageItem
        invalidate() // 重绘视图
    }

    /**
     * 绘制客户页面
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // System.out.println("on draw!!~");
        for (id in imageQueue.keys) {
            val item = imageQueue[id]
            item?.draw(canvas)
        }
        combinedPath.op(screenPath, cropPath, Path.Op.DIFFERENCE)
        //绘制遮蔽层
        canvas.drawPath(combinedPath, shaderPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // System.out.println(w + "   " + h + "    " + oldw + "   " + oldh);
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var ret = super.onTouchEvent(event) // 是否向下传递事件标志 true为消耗
        val action = event.action
        val x = event.x
        val y = event.y
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                var deleteId = -1
                for (id in imageQueue.keys) {
                    val item = imageQueue[id] ?: break
                    if (item.detectDeleteRect.contains(x, y)) { // 删除模式
                        // ret = true;
                        deleteId = id
                        currentStatus = STATUS_DELETE
                    } else if (item.detectRotateRect.contains(x, y)) {
                        ret = true
                        if (currentItem != null) {
                            currentItem!!.isDrawHelpTool = false
                        }
                        currentItem = item
                        currentItem!!.isDrawHelpTool = true
                        currentStatus = STATUS_ROTATE
                        oldx = x
                        oldy = y
                    } else if (item.detectScaleRect.contains(x, y)) { // 点击了旋转按钮
                        ret = true
                        if (currentItem != null) {
                            currentItem!!.isDrawHelpTool = false
                        }
                        currentItem = item
                        currentItem!!.isDrawHelpTool = true
                        currentStatus = STATUS_SCALE
                        oldx = x
                        oldy = y
                    } else if (detectInItemContent(item, x, y)) { // 移动模式
                        // 被选中一张贴图
                        ret = true
                        if (currentItem != null) {
                            currentItem!!.isDrawHelpTool = false
                        }
                        currentItem = item
                        currentItem!!.isDrawHelpTool = true
                        currentStatus = STATUS_MOVE
                        oldx = x
                        oldy = y
                    }
                    // end if
                } // end for each
                if (!ret && currentItem != null && currentStatus == STATUS_IDLE) { // 没有贴图被选择
                    currentItem!!.isDrawHelpTool = false
                    currentItem = null
                    invalidate()
                }
                if (deleteId > 0 && currentStatus == STATUS_DELETE) { // 删除选定贴图
                    imageQueue.remove(deleteId)
                    currentStatus = STATUS_IDLE // 返回空闲状态
                    invalidate()
                } // end if
            }

            MotionEvent.ACTION_MOVE -> {
                ret = true
                if (currentStatus == STATUS_MOVE) { // 移动贴图
                    val dx = x - oldx
                    val dy = y - oldy
                    if (currentItem != null) {
                        currentItem!!.updatePos(dx, dy)
                        invalidate()
                    } // end if
                    oldx = x
                    oldy = y
                } else if (currentStatus == STATUS_SCALE) { // 旋转 缩放图片操作
                    // System.out.println("旋转");
                    val dx = x - oldx
                    val dy = y - oldy
                    if (currentItem != null) {
                        currentItem!!.updateScale(dx, dy) // 旋转
                        invalidate()
                    } // end if
                    oldx = x
                    oldy = y
                } else if (currentStatus == STATUS_ROTATE) {
                    val dx = x - oldx
                    val dy = y - oldy
                    if (currentItem != null) {
                        currentItem!!.updateRotate( dx, dy) // 旋转
                        invalidate()
                    } // end if
                    oldx = x
                    oldy = y
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                ret = false
                currentStatus = STATUS_IDLE
            }
        }
        return ret
    }

    /**
     * 判定点击点是否在内容范围之内  需考虑旋转
     * @param item
     * @param x
     * @param y
     * @return
     */
    private fun detectInItemContent(item: AddImageItem, x: Float, y: Float): Boolean {
        //reset
        mPoint[x.toInt()] = y.toInt()
        //旋转点击点
        RectUtil.rotatePoint(mPoint, item.helpBox.centerX(), item.helpBox.centerY(), -item.rotateAngle)
        return item.helpBox.contains(mPoint.x.toFloat(), mPoint.y.toFloat())
    }

    fun clear() {
        imageQueue.clear()
        this.invalidate()
    }

    companion object {
        private const val STATUS_IDLE = 0
        private const val STATUS_MOVE = 1 // 移动状态
        private const val STATUS_DELETE = 2 // 删除状态
        private const val STATUS_SCALE = 3 // 图片旋转状态
        private const val STATUS_ROTATE = 4 // 图片缩放模式
    }
}
