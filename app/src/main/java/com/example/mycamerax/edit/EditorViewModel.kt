package com.example.mycamerax.edit

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class TouchType {
    NONE,
    MOVE,
    SCALE,
    ROTATE,
    SCALE_ROTATE,
}



data class ImageData(
    val image:Bitmap,
    val position:Offset,
    val imageSize: Dp= 40.dp,
    val rotate: Float = 0f,
    val scale:Float=1f,
    val scaleIconOffset: Offset = Offset.Zero,
    val deleteIconOffset: Offset = Offset.Zero,
    val rotateIconOffset: Offset = Offset.Zero,
//    val eventType:TouchType= TouchType.NONE
)

data class RootImage(
    val rootBitmap: Bitmap? = null,
    val center: Offset = Offset.Zero,
    val editType:EditeType= EditeType.NONE
)

data class CurrentImageList(
    val imageList :List<ImageData> = listOf(),
    val currentIndex:Int = 0,//当前移动的图
)

class EditorViewModel:ViewModel() {

    private val  _rootImage = MutableStateFlow(RootImage())
    val rootImage = _rootImage.asStateFlow()

    val deque = ArrayList<Bitmap>()


    //当前添加的图
    private val _currentImageList = MutableStateFlow(CurrentImageList())
    val currentImageList = _currentImageList.asStateFlow()



    fun updateBitmap(bitmap: Bitmap){
        viewModelScope.launch {
            _rootImage.update {
                it.copy(rootBitmap = bitmap)
            }
            deque.add(bitmap)

            XLogger.d("图像：${_rootImage.value.rootBitmap?.height}")
        }

    }

    /**
     * 中心点的位置
     */
    fun updateCenterOffset(centerOffset: Offset){
        viewModelScope.launch {
            _rootImage.update {
                it.copy(center = centerOffset)
            }
        }
    }

    fun addImage(bitmap:Bitmap){
        val list = _currentImageList.value.imageList.toMutableList()

        //val paddingValue = LocalDensity.current.run { 10.dp.toPx() }
        val padding = ResUtils.dp2px(10f)
        //新添加的图位置要放在中心位置
        val rootPosition = Offset(
            _rootImage.value.center.x - bitmap.width / 2f-padding,
            _rootImage.value.center.y - bitmap.height / 2f-padding
        )

        list.add(ImageData(image = bitmap, position = rootPosition))

        _currentImageList.update {
            it.copy(imageList = list.toList(), currentIndex = list.size - 1)
        }

        _rootImage.update {
            it.copy(editType = EditeType.PIC)
        }

        _currentImageList.value.imageList.forEachIndexed { index, imageData ->
            XLogger.d("增加的hash:${index}  ${imageData.hashCode()}")
        }
    }

    fun deleteImage(index:Int) {
       XLogger.d("删除：${index}")
        val list = _currentImageList.value.imageList.toMutableList()
        list.removeAt(index)
        val newSize = (list.size-1)
        _currentImageList.update {
            it.copy(imageList = list.toList(), currentIndex = if (newSize < 0) 0 else newSize)
        }
    }

    /**
     * 触摸等事件要前置
     * 把图片前置
     */
    fun bringImageToFront(index:Int){
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
    private fun updateHandleImageIndex(index: Int){
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

    fun updateScale(scale:Float){
        viewModelScope.launch {
            val list = _currentImageList.value.imageList.toMutableList()
            list[_currentImageList.value.currentIndex]  = list[_currentImageList.value.currentIndex].copy(scale = scale)
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
        list[_currentImageList.value.currentIndex]  = list[_currentImageList.value.currentIndex].copy(
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
        list[_currentImageList.value.currentIndex]  = list[_currentImageList.value.currentIndex].copy(
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
}