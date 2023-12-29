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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils
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




val rootImageSize = ResUtils.dp2px(40f)
@OptIn(ExperimentalComposeUiApi::class)
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
    val currentScale = currentImage.scale
    val currentHandleIndex = currentImageData.currentIndex
    var currentImagePosition by remember {
        mutableStateOf(currentImage.position)
    }

    val scaleArrowPosition = remember { mutableStateOf(Offset(0f, 0f)) }

    var scale  by   remember {
      mutableStateOf(currentScale)
    }

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
        dragEnd={
            viewModel.updateImagePosition(index, currentImagePosition)
        }
    )
    val view = LocalView.current
    Box(
        modifier = Modifier
//            .wrapContentSize()
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
            .drawBehind {
                // 外部矩形框
                if (currentHandleIndex == index) {
                    drawRect(
                        color = Color(0xFF0099A1),
                        style = Stroke(width = pxValue),
                        topLeft = Offset(paddingValue, paddingValue),
                        size = Size(
                            this.size.width - 2 * paddingValue, this.size.height - 2 * paddingValue
                        )
                    )
                }
            }
//            .scale(scale = scale)
        ) {
            val (logRef,deleteRef,rotateRef,scaleRef) = createRefs()
            Image(
                bitmap = imageData.image.asImageBitmap(),
                contentDescription = "logo",
                modifier = Modifier
                    //.background(color = Color.LightGray)
//                .pointerInteropFilter { motionEvent ->
//                    listener.onTouch(view, motionEvent)
//                }

                    .constrainAs(logRef) {

                    }
//                    .size(size = size)
                    .wrapContentSize()
                    .scale(scale)
                    .graphicsLayer {
                        this.transformOrigin = TransformOrigin.Center
                    }
                    .onGloballyPositioned {
                        // 获取图片的位置和大小信息
                        val scaleArrowX = it.size.width * scale - rootImageSize * (1 + scale)
                        val scaleArrowY = it.size.height * scale - rootImageSize * (1 + scale)
                        scaleArrowPosition.value = Offset(scaleArrowX, scaleArrowY)
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
            )


            if (currentHandleIndex == index) {
                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .constrainAs(deleteRef) {
                            end.linkTo(logRef.start, margin = (-10).dp)
                            bottom.linkTo(logRef.top, margin = (-10).dp)
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
                        .constrainAs(rotateRef) {
                            end.linkTo(logRef.start, margin = (-10).dp)
                            top.linkTo(logRef.bottom, margin = (-10).dp)
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
                                scaleArrowPosition.value.x.toInt(),
                                scaleArrowPosition.value.y.toInt()
                            )
                        }
                        .constrainAs(scaleRef) {
                            start.linkTo(logRef.end, margin = (-10).dp)
                            top.linkTo(logRef.bottom, margin = (-10).dp)
                        }
                        .pointerInput(imageData.image.hashCode()) {
                            detectDragGestures(onDragStart = {
                                XLogger.d("scale drag onDragStart")
                            }, onDragEnd = {
                                viewModel.updateScale(scale)
                                XLogger.d("scale drag onDragEnd scale:${scale}")
                            }, onDragCancel = {
                                XLogger.d("scale drag onDragCancel")
                            }, onDrag = { change, _ ->
//                                val newScale = scale * (1 + change.positionChange().y / size.height)
//                                scale = if (newScale <= 0.1) 0.1f else newScale

                                val newScale =
                                    scale * (1 + change.positionChange().y / this.size.height)
                                scale = newScale.coerceIn(0.1f, maxScale) // 限制scale的取值范围
                                XLogger.d("scale: $scale")


//                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
//                                scale = newScale.coerceIn(0.1f, maxScale) // 限制scale的取值范围
//                                size = (40.dp * scale).coerceIn(40.dp, screenWidthDp.dp) // 根据scale更新图片大小状态，并限制在40dp到200dp之间


                                XLogger.d("scale drag onDrag scale:$scale")
                            })
                        },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_scale),
                    contentDescription = "scale"
                )
            }
        }



//        Box(modifier = Modifier
//            .wrapContentSize()
//            .offset {
//                IntOffset(
//                    currentImagePosition.x.roundToInt(), currentImagePosition.y.roundToInt()
//                )
//            }
//            .drawBehind {
//                // 外部矩形框
//                if (currentHandleIndex==index){
//                    drawRect(
//                        color = Color(0xFF0099A1),
//                        style = Stroke(width = pxValue),
//                        topLeft = Offset(paddingValue, paddingValue),
//                        size = Size(
//                            this.size.width - 2 * paddingValue, this.size.height - 2 * paddingValue
//                        )
//                    )
//                }
//            }
//
//            .scale(scale = scale)
//        ) {
//            Image(
//                bitmap = imageData.image.asImageBitmap(),
//                contentDescription = "logo",
//                modifier = Modifier
//                    .padding(10.dp)
////                    .size(size)
////                    .scale(scale)
////                .pointerInteropFilter { motionEvent ->
////                    listener.onTouch(view, motionEvent)
////                }
//                    .pointerInput(imageData.image.hashCode()) {
//                        detectTapGestures(onTap = {
//                            viewModel.bringImageToFront(index)
//                        })
//                    }
//                    .pointerInput(imageData.image.hashCode()) {
//                        detectDragGestures(onDragStart = {
//                            XLogger.d("detectDragGestures onDragStart")
//                        }, onDragEnd = {
//                            XLogger.d("detectDragGestures onDragEnd")
//                            //移动结束了 更新 最后的位置
//                            viewModel.updateImagePosition(index, currentImagePosition)
//                            viewModel.bringImageToFront(index)
//                        }, onDragCancel = {
//                            //移动取消 更新 最后的位置
//                            viewModel.updateImagePosition(index, currentImagePosition)
//                            XLogger.d("detectDragGestures onDragCancel")
//                        }, onDrag = { _, dragAmount ->
//                            //乘以 缩放的倍数 这样无论缩放多少倍 都不会影响 移动的距离的偏移量
//                            currentImagePosition = Offset(
//                                currentImagePosition.x + dragAmount.x * scale,
//                                currentImagePosition.y + dragAmount.y * scale
//                            )
//                            XLogger.d("detectDragGestures onDrag")
//                        })
//                    }
//            )
//
//
//            if (currentHandleIndex == index) {
//                Image(
//                    modifier = Modifier
//                        .size(20.dp)
//                        .align(Alignment.TopStart)
//                        .scale(1f)
//                        .clickable(onClick = {
//                            viewModel.deleteImage(index)
//                        }),
//                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_delete),
//                    contentDescription = "delete"
//                )
//                Image(
//                    modifier = Modifier
//                        .size(20.dp)
//                        .scale(1f)
//                        .align(Alignment.BottomStart),
//                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_rotate),
//                    contentDescription = "rotate"
//                )
//
//
//                Image(
//                    modifier = Modifier
//                        .size(20.dp)
//                        .scale(1f)
//                        .align(Alignment.BottomEnd)
//                        .pointerInput(imageData.image.hashCode()) {
//                            detectDragGestures(onDragStart = {
//                                XLogger.d("scale drag onDragStart")
//                            }, onDragEnd = {
//                                viewModel.updateScale(scale)
//                                XLogger.d("scale drag onDragEnd scale:${scale}")
//                            }, onDragCancel = {
//                                XLogger.d("scale drag onDragCancel")
//                            }, onDrag = { change, _ ->
////                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
////                                scale = if (newScale <= 0.1) 0.1f else newScale
////                                XLogger.d("scale drag onDrag scale:$scale")
//                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
//                                scale = newScale.coerceIn(0.1f, maxScale) // 限制scale的取值范围
//                                size = (40.dp * scale).coerceIn(40.dp, screenWidthDp.dp) // 根据scale更新图片大小状态，并限制在40dp到200dp之间
//
//                            })
//                        },
//                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_scale),
//                    contentDescription = "scale"
//                )
//            }
//        }
    }
}