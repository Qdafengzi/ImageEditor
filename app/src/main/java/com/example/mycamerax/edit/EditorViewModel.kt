package com.example.mycamerax.edit

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycamerax.XLogger
import com.example.mycamerax.edit.data.CurrentImageList
import com.example.mycamerax.edit.data.ImageData
import com.example.mycamerax.edit.data.RootImage
import com.example.mycamerax.utils.ResUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class EditorViewModel : ViewModel() {

    private val _rootImageData = MutableStateFlow(RootImage())
    val rootImageData = _rootImageData.asStateFlow()

    //撤销的核心点
    private val bitmapQueue = ArrayList<Bitmap>()


    //当前添加的图
    private val _currentImageList = MutableStateFlow(CurrentImageList())
    val currentImageList = _currentImageList.asStateFlow()


    fun editorInit(bitmap: Bitmap) {
        viewModelScope.launch {
            val originalBitmapRect = if (bitmap.height >= bitmap.width) {
                //以高度为准进行缩放
                //屏幕高度
                val screenWidth = ResUtils.screenWidth.toFloat()
                val zoomRatio = screenWidth / bitmap.height.toFloat()
                val widthOfZoom = bitmap.width * zoomRatio
                val left = (screenWidth - widthOfZoom) * 0.5f
                XLogger.d("zoomRatio----screenWidth:${ResUtils.screenWidth}")
                XLogger.d("zoomRatio----width:${bitmap.width}  height:${bitmap.height}-- width:${widthOfZoom}---->zoomRatio:${zoomRatio} left:$left")
                //1080 - 854.1818  /2
                //zoomRatio----width:435  height:550-- width:854.1818---->zoomRatio:1.9636364 x:112.90909
                Rect(left, 0f, left + widthOfZoom, screenWidth)

            } else {
                val screenWidth = ResUtils.screenWidth.toFloat()
                val zoomRatio = screenWidth / bitmap.width.toFloat()
                val heightOfZoom = bitmap.height * zoomRatio
                val top = (screenWidth - heightOfZoom) * 0.5f
                Rect(0f, top, screenWidth, top + heightOfZoom)
            }

            _rootImageData.update {
                it.copy(
                    rootBitmap = bitmap,
                    originalSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
                    cropRect = originalBitmapRect
                )
            }
            bitmapQueue.add(bitmap)
        }
    }

    fun updateBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _rootImageData.update {
                it.copy(rootBitmap = bitmap)
            }
            bitmapQueue.add(bitmap)

            XLogger.d("图像：${_rootImageData.value.rootBitmap?.height}")
        }

    }

    /**
     * 中心点的位置
     */
    fun updateCenterOffset(centerOffset: Offset) {
        viewModelScope.launch {
            _rootImageData.update {
                it.copy(center = centerOffset)
            }
        }
    }

    fun addImage(bitmap: Bitmap) {
        val list = _currentImageList.value.imageList.toMutableList()

        //val paddingValue = LocalDensity.current.run { 10.dp.toPx() }
        val padding = ResUtils.dp2px(10f)
        //新添加的图位置要放在中心位置
        val rootPosition = Offset(
            _rootImageData.value.center.x - bitmap.width / 2f - padding,
            _rootImageData.value.center.y - bitmap.height / 2f - padding
        )

        list.add(ImageData(image = bitmap, position = rootPosition))

        _currentImageList.update {
            it.copy(imageList = list.toList(), currentIndex = list.size - 1)
        }

        _rootImageData.update {
            it.copy(editType = EditeType.PIC)
        }

        _currentImageList.value.imageList.forEachIndexed { index, imageData ->
            XLogger.d("增加的hash:${index}  ${imageData.hashCode()}")
        }
    }

    fun deleteImage(index: Int) {
        XLogger.d("删除：${index}")
        val list = _currentImageList.value.imageList.toMutableList()
        list.removeAt(index)
        val newSize = (list.size - 1)
        _currentImageList.update {
            it.copy(imageList = list.toList(), currentIndex = if (newSize < 0) 0 else newSize)
        }
    }

    /**
     * 触摸等事件要前置
     * 把图片前置
     */
    fun bringImageToFront(index: Int) {
        //XLogger.d("前置START最后一个是：${_currentImageList.value.imageList.last()}")
        //如果就一个不做处理
        if (_currentImageList.value.imageList.size <= 1) return

        val list = _currentImageList.value.imageList.toMutableList()
        val data = list.removeAt(index)

        //XLogger.d("当前的：${data}")
        list.add(data)

        //更新当前的图
        updateHandleImageIndex(list.size - 1)

        _currentImageList.update {
            it.copy(imageList = list.toList())
        }
        XLogger.d("前置成功最后一个是：${_currentImageList.value.imageList.last()}")
    }

    /**
     * 更新当前处理的图片
     */
    private fun updateHandleImageIndex(index: Int) {
        XLogger.d("current index $index")
        _currentImageList.update {
            it.copy(currentIndex = index)
        }
    }

    fun updateImagePosition(index: Int, position: Offset) {
        val list = _currentImageList.value.imageList.toMutableList()
        val newData = list[index].copy(position = position)
        list[index] = newData
        _currentImageList.update {
            it.copy(imageList = list.toList())
        }
    }

    fun updateScale(scale: Float) {
        viewModelScope.launch {
            val list = _currentImageList.value.imageList.toMutableList()
            list[_currentImageList.value.currentIndex] =
                list[_currentImageList.value.currentIndex].copy(scale = scale)
            _currentImageList.update {
                it.copy(imageList = list.toList())
            }
        }
    }

    fun updateIconsOffset(
        deleteIconOffset: Offset,
        rotateIconOffset: Offset,
        scaleIconOffset: Offset
    ) {
        val list = _currentImageList.value.imageList.toMutableList()
        list[_currentImageList.value.currentIndex] =
            list[_currentImageList.value.currentIndex].copy(
                deleteIconOffset = deleteIconOffset,
                rotateIconOffset = rotateIconOffset,
                scaleIconOffset = scaleIconOffset
            )
        _currentImageList.update {
            it.copy(imageList = list.toList())
        }
    }

    fun updateRotate(angle: Float) {
        val list = _currentImageList.value.imageList.toMutableList()
        list[_currentImageList.value.currentIndex] =
            list[_currentImageList.value.currentIndex].copy(
                rotate = angle
            )
        _currentImageList.update {
            it.copy(imageList = list.toList())
        }
    }


//    fun updateTouchType(type:TouchType){
//        val list = _currentImageList.value.imageList.toMutableList()
//        list[_currentImageList.value.currentIndex]  = list[_currentImageList.value.currentIndex].copy(
//            eventType = type
//        )
//        _currentImageList.update {
//            it.copy(imageList = list.toList())
//        }
//    }


    fun addCrop() {
        _rootImageData.update {
            it.copy(editType = EditeType.CROP)
        }
    }

    fun cancelCrop() {
        _rootImageData.update {
            it.copy(editType = EditeType.NONE)
        }
    }
}