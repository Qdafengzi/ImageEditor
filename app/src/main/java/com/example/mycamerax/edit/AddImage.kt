package com.example.mycamerax.edit

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils
import kotlin.math.abs
import kotlin.math.roundToInt

val padding = ResUtils.dp2px(10f)

class ViewOnTouchListener(private val viewModel: EditorViewModel,
                          private val index: Int,
                          private val bitmapWidth:Int,
                          private val bitmapHeight:Int,
                          private val moveCallBack:(deltaX:Float,deltaY:Float)->Unit,
                          private val dragEnd:()->Unit,
) : View.OnTouchListener {
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var startX = 0f
        var startY = 0f
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                viewModel.bringImageToFront(index)
                startX = event.x - bitmapWidth / 2f
                startY = event.y - bitmapHeight / 2f
                XLogger.d("action Down")
            }
            MotionEvent.ACTION_MOVE->{
                XLogger.d("action Move")
                val deltaX: Float = event.x - startX - bitmapWidth / 2f
                val deltaY: Float = event.y - startY - bitmapHeight / 2f
                moveCallBack.invoke(deltaX,deltaY)
            }
            MotionEvent.ACTION_UP->{
                dragEnd.invoke()
            }
        }
        return true
    }
}





val scaleSize = ResUtils.dp2px(20f)
@Composable
fun AddImage(index: Int, imageData: ImageData, viewModel: EditorViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxScale = screenWidthDp / 40f
//    var size by remember { mutableStateOf(40.dp) } // 初始图片大小

    val paddingValue = LocalDensity.current.run { 10.dp.toPx() }
    val pxValue = LocalDensity.current.run { 1.dp.toPx() }

    val currentImageData = viewModel.currentImageList.collectAsState().value
    val currentImage = currentImageData.imageList[index]


    val currentHandleIndex = currentImageData.currentIndex
    var currentImagePosition by remember {
        mutableStateOf(currentImage.position)
    }


    //三个按钮的偏移位置
    var scaleIconOffset  by  remember { mutableStateOf(currentImage.scaleIconOffset) }
    var deleteIconOffset  by  remember { mutableStateOf(currentImage.deleteIconOffset) }
    var rotateIconOffset by  remember { mutableStateOf(currentImage.rotateIconOffset) }

    var scale  by   remember {
      mutableStateOf(currentImage.scale)
    }

    var imageSize by remember { mutableStateOf(100.dp) }

    val listener = ViewOnTouchListener(
        viewModel = viewModel,
        index = index,
        bitmapWidth = currentImage.image.width,
        bitmapHeight =currentImage.image.height ,
        moveCallBack = {deltaX, deltaY ->
            XLogger.d("deltaX:${deltaX}  deltaY:${deltaY}")
            currentImagePosition = Offset(
                currentImagePosition.x + deltaX,
                currentImagePosition.y + deltaY
            )
        },
        dragEnd = {
            viewModel.updateImagePosition(index, currentImagePosition)
        }
    )
    val view = LocalView.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
//            .drawWithContent {
//                drawContent()
//                // 中心辅助线
//                drawLine(color = Color.Black,start= Offset(size.width/2f,0f),end = Offset(size.width/2f,size.height),strokeWidth = 1f)
//                drawLine(color = Color.Black,start= Offset(0f,size.height/2f),end = Offset(size.width,size.height/2f),strokeWidth = 1f)
//            }

    ) {
        ConstraintLayout(modifier = Modifier
            .wrapContentSize()
            .offset {
                IntOffset(
                    currentImagePosition.x.roundToInt(), currentImagePosition.y.roundToInt()
                )
            }
        ) {
            val (logoRef, deleteRef, rotateRef, scaleRef) = createRefs()
            Image(
                bitmap = imageData.image.asImageBitmap(),
                contentDescription = "logo",
                modifier = Modifier
                    //.background(color = Color.LightGray)
//                .pointerInteropFilter { motionEvent ->
//                    listener.onTouch(view, motionEvent)
//                }
                    .wrapContentSize()
                    .constrainAs(logoRef) {
                    }
                    .scale(scale)
                    .graphicsLayer {
                        //this.translationX =
                        this.transformOrigin = TransformOrigin.Center
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        val size = layoutCoordinates.size.toSize()
                        //因为是按照中心点进行缩放 所以其缩放距离是一般的缩放比
                        val scaleValue = abs(1 - scale) / 2f

                        // 计算缩放按钮的位置
                        val scaleButtonX = size.width * scaleValue * if (scale < 1) -1 else 1
                        val scaleButtonY = size.height * scaleValue * if (scale < 1) -1 else 1
                        scaleIconOffset = Offset(scaleButtonX, scaleButtonY)

                        // 计算删除按钮的位置
                        val deleteButtonX = size.width * scaleValue * if (scale < 1) 1 else -1
                        val deleteButtonY = size.height * scaleValue * if (scale < 1) 1 else -1
                        deleteIconOffset = Offset(deleteButtonX, deleteButtonY)


                        // 计算旋转按钮的位置
                        val rotateButtonX = size.width * scaleValue * if (scale < 1) 1 else -1
                        val rotateButtonY = size.height * scaleValue * if (scale < 1) -1 else 1
                        rotateIconOffset = Offset(rotateButtonX, rotateButtonY)
                    }


                    .pointerInput(imageData.image.hashCode()) {
                        detectTapGestures(onTap = {
                            viewModel.bringImageToFront(index)
                        })
                    }
                    .pointerInput(imageData.image.hashCode()) {
                        detectDragGestures(onDragStart = {
                            XLogger.d("detectDragGestures onDragStart")
                        }, onDragEnd = {
                            XLogger.d("detectDragGestures onDragEnd")
                            //移动结束了 更新 最后的位置
                            viewModel.updateImagePosition(index, currentImagePosition)
                            viewModel.bringImageToFront(index)
                        }, onDragCancel = {
                            //移动取消 更新 最后的位置
                            viewModel.updateImagePosition(index, currentImagePosition)
                            XLogger.d("detectDragGestures onDragCancel")
                        }, onDrag = { _, dragAmount ->
                            //乘以 缩放的倍数 这样无论缩放多少倍 都不会影响 移动的距离的偏移量
                            currentImagePosition = Offset(
                                currentImagePosition.x + dragAmount.x * scale,
                                currentImagePosition.y + dragAmount.y * scale
                            )
                            XLogger.d("detectDragGestures onDrag")
                        })
                    }
                    .drawBehind {
                        // 外部矩形框
                        if (currentHandleIndex == index) {
                            drawRect(
                                color = Color(0xFF0099A1),
                                style = Stroke(width = pxValue),
                            )
                        }
                    }
            )

            if (currentHandleIndex == index) {
                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .offset {
                            IntOffset(
                                deleteIconOffset.x.toInt(),
                                deleteIconOffset.y.toInt()
                            )
                        }
                        .constrainAs(deleteRef) {
                            end.linkTo(logoRef.start, margin = (-10).dp)
                            bottom.linkTo(logoRef.top, margin = (-10).dp)
                        }
                        .clickable(onClick = {
                            viewModel.deleteImage(index)
                        }),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_delete),
                    contentDescription = "delete"
                )
                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .offset {
                            IntOffset(
                                rotateIconOffset.x.toInt(),
                                rotateIconOffset.y.toInt()
                            )
                        }
                        .constrainAs(rotateRef) {
                            end.linkTo(logoRef.start, margin = (-10).dp)
                            top.linkTo(logoRef.bottom, margin = (-10).dp)
                        }
                    ,
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_rotate),
                    contentDescription = "rotate"
                )

                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .offset {
                            IntOffset(
                                scaleIconOffset.x.toInt(),
                                scaleIconOffset.y.toInt()
                            )
                        }
                        .constrainAs(scaleRef) {
                            start.linkTo(logoRef.end, (-10).dp)
                            top.linkTo(logoRef.bottom, (-10).dp)
                        }
                        .pointerInput(imageData.image.hashCode()) {
                            detectDragGestures(onDragStart = {
                                XLogger.d("scale drag onDragStart")
                            }, onDragEnd = {
                                viewModel.updateScale(scale)
                                viewModel.updateIconsOffset(deleteIconOffset,rotateIconOffset,scaleIconOffset)
                                XLogger.d("scale drag onDragEnd scale:${scale}")
                            }, onDragCancel = {
                                XLogger.d("scale drag onDragCancel")
                            }, onDrag = { change, _ ->
                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
                                scale = newScale.coerceIn(0.2f, 10f) // 限制scale的取值范围
                                imageSize = (scale * 100).dp.coerceIn(40.dp, 1000.dp)
                                XLogger.d("scale: $scale")
                                XLogger.d("scale drag onDrag scale:$scale")
                            })
                        },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_scale),
                    contentDescription = "scale"
                )
            }
        }
    }
}

//旋转  图片、三个ICON
//缩放  图片
