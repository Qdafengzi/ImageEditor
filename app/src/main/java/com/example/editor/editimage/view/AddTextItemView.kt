package com.example.editor.editimage.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.editor.R
import com.example.editor.editimage.utils.ListUtil
import com.example.editor.editimage.utils.RectUtil
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class AddTextItemView : View {
    //public static final int CHAR_MIN_HEIGHT = 60;
    //private String mText;
    private val mPaint = TextPaint()
    private val debugPaint = Paint()
    private val mHelpPaint = Paint()
    private val mTextRect = Rect() // warp text rect record
    private val mHelpBoxRect = RectF()
    private val mDeleteRect = Rect() //删除按钮位置
    private val mRotateRect = Rect() //旋转按钮位置
    private val mScaleRect = Rect() //旋转按钮位置
    private var mDeleteDstRect = RectF()
    private var mRotateDstRect = RectF()
    private var mScaleDstRect = RectF()
    private lateinit var mDeleteBitmap: Bitmap
    private lateinit var mRotateBitmap: Bitmap
    private lateinit var mScaleBitmap: Bitmap
    private var mCurrentMode = IDLE_MODE

    //    private EditText mEditText;//输入控件
    private var layoutX = 0
    private var layoutY = 0
    private var lastX = 0f
    private var lastY = 0f
    private var rotateAngle = 0f
    private var scale = 1f
    private var isInitLayout = true
    private var isShowHelpBox = true
    private val shaderPaint = Paint()
    private var isAutoNewLine = false //是否需要自动换行
        private set
    private val mTextContents: MutableList<String?> = ArrayList(2) //存放所写的文字内容
    private var mText: String? = null
    private val mPoint = Point(0, 0)

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context)
    }

    fun setEditText(textView: EditText?) {
//        this.mEditText = textView;
    }

    private fun initView(context: Context) {
        shaderPaint.color = ContextCompat.getColor(context, R.color.shader)
        debugPaint.color = Color.parseColor("#66ff0000")
        mDeleteBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_delete
        )
        mRotateBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_rotate
        )
        mScaleBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_scale
        )
        mDeleteRect[0, 0, mDeleteBitmap.width] = mDeleteBitmap.height
        mRotateRect[0, 0, mRotateBitmap.width] = mRotateBitmap.height
        mScaleRect[0, 0, mScaleBitmap.width] = mScaleBitmap.height
        mDeleteDstRect = RectF(0f, 0f, (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat(), (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat())
        mRotateDstRect = RectF(0f, 0f, (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat(), (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat())
        mScaleDstRect = RectF(0f, 0f, (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat(), (Constants.IMAGE_BTN_HALF_SIZE shl 1).toFloat())
        mPaint.color = Color.WHITE
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.textSize = TEXT_SIZE_DEFAULT
        mPaint.isAntiAlias = true
        mPaint.textAlign = Paint.Align.LEFT
        mHelpPaint.color = ContextCompat.getColor(context, R.color.rect_frame)
        mHelpPaint.style = Paint.Style.STROKE
        mHelpPaint.isAntiAlias = true
        mHelpPaint.strokeWidth = 4f
    }

    private var screenPath = Path()
    private var cropPath = Path()
    private var combinedPath = Path()
    private var shaderRec = RectF()
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

    var example = TextView(context)
    fun setText(text: String?) {
        mText = text
        example.text = text
        example.textSize = TEXT_SIZE_DEFAULT
        invalidate()
    }

    fun setTextColor(newColor: Int) {
        mPaint.color = newColor
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (isInitLayout) {
            isInitLayout = false
            resetView()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (TextUtils.isEmpty(mText)) return
        parseText()
        drawContent(canvas)


        //绘制遮蔽层
        combinedPath.op(screenPath, cropPath, Path.Op.DIFFERENCE)
        canvas.drawPath(combinedPath, shaderPaint)
    }

    protected fun parseText() {
        if (TextUtils.isEmpty(mText)) return
        mTextContents.clear()
        val splits = mText!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        //end for each
        mTextContents.addAll(Arrays.asList(*splits))
    }

    private fun drawContent(canvas: Canvas) {
        drawText(canvas)
        //draw x and rotate button
        val offsetValue = mDeleteDstRect.width().toInt() shr 1
        mDeleteDstRect.offsetTo(mHelpBoxRect.left - offsetValue, mHelpBoxRect.top - offsetValue)
        mRotateDstRect.offsetTo(mHelpBoxRect.left - offsetValue, mHelpBoxRect.bottom - offsetValue)
        mScaleDstRect.offsetTo(mHelpBoxRect.right - offsetValue, mHelpBoxRect.bottom - offsetValue)
        RectUtil.rotateRect(
            mDeleteDstRect, mHelpBoxRect.centerX(),
            mHelpBoxRect.centerY(), rotateAngle
        )
        RectUtil.rotateRect(
            mRotateDstRect, mHelpBoxRect.centerX(),
            mHelpBoxRect.centerY(), rotateAngle
        )
        RectUtil.rotateRect(
            mScaleDstRect, mHelpBoxRect.centerX(),
            mHelpBoxRect.centerY(), rotateAngle
        )
        if (!isShowHelpBox) {
            return
        }
        canvas.save()
        canvas.rotate(rotateAngle, mHelpBoxRect.centerX(), mHelpBoxRect.centerY())
        canvas.drawRect(mHelpBoxRect, mHelpPaint)
        canvas.restore()
        canvas.drawBitmap(mDeleteBitmap, mDeleteRect, mDeleteDstRect, null)
        canvas.drawBitmap(mRotateBitmap, mRotateRect, mRotateDstRect, null)
        canvas.drawBitmap(mScaleBitmap, mScaleRect, mScaleDstRect, null)

        //debug
//        canvas.drawRect(mRotateDstRect, debugPaint);
//        canvas.drawRect(mDeleteDstRect, debugPaint);
//        canvas.drawRect(mHelpBoxRect, debugPaint);
    }

    private fun drawText(canvas: Canvas) {
        drawText(canvas, layoutX, layoutY, scale, rotateAngle)
    }

    fun getTextBounds(paint: Paint, text: String?): Rect {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text!!.length, bounds)
        return bounds
    }

    private fun drawText(canvas: Canvas, _x: Int, _y: Int, scale: Float, rotate: Float) {
        if (ListUtil.isEmpty(mTextContents)) return
        var x = _x
        var y = _y
        val textHeight: Int
        //clear
        mTextRect.setEmpty()
        val tempRect = Rect()
        val fontMetrics = mPaint.fontMetricsInt
        val charMinHeight = abs(fontMetrics.top) + abs(fontMetrics.bottom) //字体高度
        //        int charMinHeight =Math.abs(fontMetrics.bottom-fontMetrics.top);//字体高度
//        val textBounds = getTextBounds(mPaint, mText)
//        val width = textBounds.width()
//        val height = textBounds.height()
        textHeight = charMinHeight
        var totalHeight = 0
        var maxWidth = 0
        //System.out.println("top = "+fontMetrics.top +"   bottom = "+fontMetrics.bottom);
        for (i in mTextContents.indices) {
            val text = mTextContents[i]
            mPaint.getTextBounds(text, 0, text!!.length, tempRect)
            //System.out.println(i + " ---> " + tempRect.height());
            //text_height = Math.max(charMinHeight, tempRect.height());
            if (tempRect.height() <= 0) { //处理此行文字为空的情况
                tempRect[0, 0, 0] = textHeight
            }
            totalHeight += textHeight
            if (tempRect.width() > maxWidth) {
                maxWidth = tempRect.width()
            }
            RectUtil.rectAddV(mTextRect, tempRect, 0, charMinHeight)
        }
        x -= maxWidth / 2
        y -= totalHeight / 2
        mTextRect.offset(x, y)

//        mHelpBoxRect.set(
//                mTextRect.left - PADDING,
//                mTextRect.top - PADDING,
//                mTextRect.right + PADDING,
//                mTextRect.bottom + PADDING
//        );
        mHelpBoxRect[(
                mTextRect.left - PADDING).toFloat(), (
                mTextRect.top - PADDING).toFloat(), (
                mTextRect.right + PADDING).toFloat()] = mTextRect.bottom
            .toFloat()
        RectUtil.scaleRect(mHelpBoxRect, scale)
        canvas.save()
        canvas.scale(scale, scale, mHelpBoxRect.centerX(), mHelpBoxRect.centerY())
        canvas.rotate(rotate, mHelpBoxRect.centerX(), mHelpBoxRect.centerY())


        //canvas.drawRect(mTextRect, debugPaint);
        //float left = mHelpBoxRect.left - mTextRect.left;
        //float right = mHelpBoxRect.right - mTextRect.right;

        //System.out.println("left = "+left +"   right = "+right);
//        int draw_text_y = y + (textHeight >> 1) + PADDING;
        var drawTextY = y + (textHeight shr 1)
        for (i in mTextContents.indices) {
            canvas.drawText(mTextContents[i]!!, x.toFloat(), drawTextY.toFloat(), mPaint)
            drawTextY += textHeight
        }

//        canvas.drawCircle(canvas.getWidth(),canvas.getHeight(),20f,mPaint);
//        canvas.drawLine(canvas.getWidth()/2,0,canvas.getWidth()/2,canvas.getHeight(),mPaint);
//        canvas.drawLine(0,canvas.getHeight()/2,canvas.getWidth(),canvas.getHeight()/2,mPaint);
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var ret = super.onTouchEvent(event) // 是否向下传递事件标志 true为消耗
        val action = event.action
        val x = event.x
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (mDeleteDstRect.contains(x, y)) { // 删除模式
                    isShowHelpBox = true
                    mCurrentMode = DELETE_MODE
                } else if (mScaleDstRect.contains(x, y)) { // 旋转按钮
                    isShowHelpBox = true
                    mCurrentMode = SCALE_MODE
                    lastX = mScaleDstRect.centerX()
                    lastY = mScaleDstRect.centerY()
                    ret = true
                } else if (mRotateDstRect.contains(x, y)) { // 旋转按钮
                    isShowHelpBox = true
                    mCurrentMode = ROTATE_MODE
                    lastX = mRotateDstRect.centerX()
                    lastY = mRotateDstRect.centerY()
                    ret = true
                } else if (detectInHelpBox(x, y)) { // 移动模式
                    isShowHelpBox = true
                    mCurrentMode = MOVE_MODE
                    lastX = x
                    lastY = y
                    ret = true
                } else {
                    isShowHelpBox = false
                    invalidate()
                } // end if
                if (mCurrentMode == DELETE_MODE) {
                    // 删除选定贴图
                    mCurrentMode = IDLE_MODE
                    // 返回空闲状态
                    clearTextContent()
                    invalidate()
                } // end if
            }

            MotionEvent.ACTION_MOVE -> {
                ret = true
                when (mCurrentMode) {
                    MOVE_MODE -> {
                        // 移动贴图
                        val dx = x - lastX
                        val dy = y - lastY
                        layoutX = (layoutX + dx).toInt()
                        layoutY = (layoutY + dy).toInt()
                        invalidate()
                        lastX = x
                        lastY = y
                    }
                    SCALE_MODE -> {
                        // 旋转 缩放文字操作
                        val dx = x - lastX
                        val dy = y - lastY
                        updateRotateAndScale(dx, dy, isScale = true)
                        invalidate()
                        lastX = x
                        lastY = y
                    }
                    ROTATE_MODE -> {
                        // 旋转 缩放文字操作
                        val dx = x - lastX
                        val dy = y - lastY
                        updateRotateAndScale(dx, dy, isRotate = true)
                        invalidate()
                        lastX = x
                        lastY = y
                    }
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                ret = false
                mCurrentMode = IDLE_MODE
            }
        }
        return ret
    }

    /**
     * 考虑旋转情况下 点击点是否在内容矩形内
     *
     * @param x
     * @param y
     * @return
     */
    private fun detectInHelpBox(x: Float, y: Float): Boolean {
        //mRotateAngle
        mPoint[x.toInt()] = y.toInt()
        //旋转点击点
        RectUtil.rotatePoint(mPoint, mHelpBoxRect.centerX(), mHelpBoxRect.centerY(), -rotateAngle)
        return mHelpBoxRect.contains(mPoint.x.toFloat(), mPoint.y.toFloat())
    }

    fun clearTextContent() {
//        if (mEditText != null) {
//            mEditText.setText(null);
//        }
        //setText(null);
        setText("")
    }

    /**
     * 旋转 缩放 更新
     *
     * @param dx
     * @param dy
     */
    private fun updateRotateAndScale(dx: Float, dy: Float, isScale: Boolean = false, isRotate: Boolean = false) {
        val cX = mHelpBoxRect.centerX()
        val cY = mHelpBoxRect.centerY()
        val x = mScaleDstRect.centerX()
        val y = mScaleDstRect.centerY()
        val nX = x + dx
        val nY = y + dy
        val xa = x - cX
        val ya = y - cY
        val xb = nX - cX
        val yb = nY - cY
        val srcLen = sqrt((xa * xa + ya * ya).toDouble()).toFloat()
        val curLen = sqrt((xb * xb + yb * yb).toDouble()).toFloat()

        if (isScale) {
            val scale = curLen / srcLen // 计算缩放比
            this.scale *= scale
            val newWidth = mHelpBoxRect.width() * this.scale
            if (newWidth < 70) {
                this.scale /= scale
                return
            }
        }

        if (isRotate) {
            val cos = ((xa * xb + ya * yb) / (srcLen * curLen)).toDouble()
            if (cos > 1 || cos < -1) return
            var angle = Math.toDegrees(acos(cos)).toFloat()
            val calMatrix = xa * yb - xb * ya // 行列式计算 确定转动方向
            val flag = if (calMatrix > 0) 1 else -1
            angle *= flag
            rotateAngle += angle
        }
    }


    fun resetView() {
        layoutX = measuredWidth / 2
        layoutY = measuredWidth / 2
        rotateAngle = 0f
        scale = 1f
        mTextContents.clear()
    }

    fun setAutoNewline(isAuto: Boolean) {
        if (isAutoNewLine != isAuto) {
            isAutoNewLine = isAuto
            postInvalidate()
        }
    }

    companion object {
        const val TEXT_SIZE_DEFAULT = 80f
        const val PADDING = 32

        //public static final int PADDING = 0;
        const val TEXT_TOP_PADDING = 10

        //控件的几种模式
        private const val IDLE_MODE = 2 //正常
        private const val MOVE_MODE = 3 //移动模式
        private const val SCALE_MODE = 4 //旋转模式
        private const val DELETE_MODE = 5 //删除模式
        private const val ROTATE_MODE = 6 //旋转模式
    }
} //end class
