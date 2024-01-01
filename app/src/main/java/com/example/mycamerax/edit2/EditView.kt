package com.example.mycamerax.edit2

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import com.example.mycamerax.R
import com.example.mycamerax.databinding.ItemAddViewBinding
import kotlin.math.abs
import kotlin.math.sqrt


class EditView : FrameLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // 如果你的应用的 min sdk >= 21 (Android 5.0)，你可以添加这个构造函数
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    val deque = ArrayList<Bitmap>()

    val mBitmap: Bitmap? = null

    lateinit var  rootView :ImageView


    fun initView(bitmap: Bitmap) {

        rootView = ImageView(context)
        rootView.setImageBitmap(bitmap)
        rootView.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(rootView)
    }

    fun updateBitmap(bitmap: Bitmap) {

    }

    @SuppressLint("ClickableViewAccessibility")
    fun addImage(bitmap: Bitmap) {
        val binding = DataBindingUtil.inflate<ItemAddViewBinding>(LayoutInflater.from(context), R.layout.item_add_view,this,true)
        binding.imageAdd.setImageBitmap(bitmap)
        val layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        binding.root.layoutParams = layoutParams

        val parentView = binding.root.parent
        if (parentView!=null){
            (parentView as ViewGroup).removeView(binding.root)
        }



      var scaleFactor = 1.0f
      var initialDistance = 0f
      var initialScaleFactor = 1.0f

        var startDistance = 0f

        var dX: Float = 0f
        var dY: Float = 0f
        val mScaleFactor = 1.0f
        var mLastTouchX: Float = 0f
        var mLastTouchY: Float = 0f

        binding.imageScale.setOnTouchListener { view, event ->
            when (event.getAction()) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.getRawX()
                    dY = view.y - event.getRawY()
                    mLastTouchX = event.getRawX()
                    mLastTouchY = event.getRawY()
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx: Float = event.getRawX() - mLastTouchX
                    val dy: Float = event.getRawY() - mLastTouchY

                    val distance = abs(dx)

                    binding.imageAdd.pivotX = binding.imageAdd.width/2f
                    binding.imageAdd.pivotY= binding.imageAdd.height/2f
                    binding.imageAdd.scaleX = distance/10f
                    binding.imageAdd.scaleY = distance/10f




//                    binding.imageAdd.animate()
//                        .x(event.getRawX() + dX)
//                        .y(event.getRawY() + dY)
//                        .setDuration(0)
//                        .start()
                    mLastTouchX = event.getRawX()
                    mLastTouchY = event.getRawY()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {}
                MotionEvent.ACTION_POINTER_UP -> {}
                MotionEvent.ACTION_UP -> {}
            }
            true
        }


        addView(binding.root)




    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }
}
