package com.example.mycamerax.edit

import android.graphics.PointF
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val padding = ResUtils.dp2px(10f)

class ViewOnTouchListener(
    private val viewModel: EditorViewModel,
    private val index: Int,
    private val bitmapWidth: Int,
    private val bitmapHeight: Int,
    private val moveCallBack: (deltaX: Float, deltaY: Float) -> Unit,
    private val dragEnd: () -> Unit,
) : View.OnTouchListener {
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var startX = 0f
        var startY = 0f
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewModel.bringImageToFront(index)
                startX = event.x - bitmapWidth / 2f
                startY = event.y - bitmapHeight / 2f
                XLogger.d("action Down")
            }

            MotionEvent.ACTION_MOVE -> {
                XLogger.d("action Move")
                val deltaX: Float = event.x - startX - bitmapWidth / 2f
                val deltaY: Float = event.y - startY - bitmapHeight / 2f
                moveCallBack.invoke(deltaX, deltaY)
            }

            MotionEvent.ACTION_UP -> {
                dragEnd.invoke()
            }
        }
        return true
    }
}


val scaleSize = ResUtils.dp2px(20f)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddImage(index: Int, imageData: ImageData, viewModel: EditorViewModel) {
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current

//    val screenWidthDp = LocalConfiguration.current.screenWidthDp
//    val maxScale = screenWidthDp / 40f
//    var size by remember { mutableStateOf(40.dp) } // 初始图片大小

//    val paddingValue = LocalDensity.current.run { 10.dp.toPx() }
    val pxValue = LocalDensity.current.run { 1.dp.toPx() }

    val currentImageData = viewModel.currentImageList.collectAsState().value
    val currentImage = currentImageData.imageList[index]
//    val eventType = currentImage.eventType


    val currentHandleIndex = currentImageData.currentIndex
    var currentImagePosition by remember {
        mutableStateOf(currentImage.position)
    }

    var currentRotate by remember { mutableStateOf(currentImage.rotate) }
//    val sensitivity = 50f

//    var imageCenter by remember { mutableStateOf(Offset.Zero) }
//    var imagePosition  by remember {
//        mutableStateOf(Offset.Zero)
//    }

    //三个按钮的偏移位置
    var scaleIconOffset by remember { mutableStateOf(currentImage.scaleIconOffset) }
    var deleteIconOffset by remember { mutableStateOf(currentImage.deleteIconOffset) }
    var rotateIconOffset by remember { mutableStateOf(currentImage.rotateIconOffset) }

    var scale by remember {
        mutableStateOf(currentImage.scale)
    }

//    var imageSize by remember { mutableStateOf(Size(0f, 0f)) }


    //双指 缩放 和旋转的处理
//    val transformState = rememberTransformableState { zoomChange, _, rotationChange ->
//        // 处理缩放、偏移和旋转的变化
//        scale *= zoomChange
//        angle += rotationChange
//    }


//    val listener = ViewOnTouchListener(
//        viewModel = viewModel,
//        index = index,
//        bitmapWidth = currentImage.image.width,
//        bitmapHeight = currentImage.image.height,
//        moveCallBack = { deltaX, deltaY ->
//            XLogger.d("deltaX:${deltaX}  deltaY:${deltaY}")
//            currentImagePosition = Offset(
//                currentImagePosition.x + deltaX,
//                currentImagePosition.y + deltaY
//            )
//        },
//        dragEnd = {
//            viewModel.updateImagePosition(index, currentImagePosition)
//        }
//    )
//    val view = LocalView.current

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

            .graphicsLayer {
//                rotationZ = currentRotate
                transformOrigin = TransformOrigin.Center
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
                    //.transformable(state = transformState)
//                    .scale(scale)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ =  currentRotate
                        transformOrigin = TransformOrigin.Center
                    }
                    .onGloballyPositioned { layoutCoordinates ->
//                        if (eventType==TouchType.ROTATE) return@onGloballyPositioned

                        XLogger.d("onGloballyPositioned-------------->")
                        val angleRadians = Math.toRadians(currentRotate.toDouble()).toFloat()


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
//                        val rotateButtonX = size.width * scaleValue * if (scale < 1) 1 else -1
//                        val rotateButtonY = size.height * scaleValue * if (scale < 1) -1 else 1
                        //rotateIconOffset = Offset(rotateButtonX, rotateButtonY)


                        //旋转后的x y 坐标 肯定变化了

                        // 计算旋转后的偏移量
                        // 获取图像的中心点坐标
                        val centerX = size.width / 2
                        val centerY = size.height / 2

                        // 计算旋转后的偏移量
                        val rotatedOffsetX = centerX * cos(Math.toRadians(currentRotate.toDouble())) - centerY * sin(Math.toRadians(currentRotate.toDouble()))
                        val rotatedOffsetY = centerX * sin(Math.toRadians(currentRotate.toDouble())) + centerY * cos(Math.toRadians(currentRotate.toDouble()))

                        val rotateButtonX = (size.width) * scaleValue * if (scale < 1) 1 else -1
                        val rotateButtonY = (size.height) * scaleValue * if (scale < 1) -1 else 1
//
//                        // 更新图像的偏移量
                        val imageOffset =  Offset(rotatedOffsetX.toFloat()-size.width* 0.5f, rotatedOffsetY.toFloat()-size.height* 0.5f)
                        //imageOffset x y 调换位置了

                        // 初始化的就是 宽度的一半
                        XLogger.d( "Rotated Offset: $imageOffset   size:${size} rotateButtonX:${rotateButtonX}  rotateButtonY:${rotateButtonY}")
//
////                        Offset(rotatedOffsetX.toFloat(), -rotatedOffsetY.toFloat())
                        rotateIconOffset = Offset(rotateButtonX.toFloat() , rotateButtonY.toFloat())


//                        val centerX = size.width / 2
//                        val centerY = size.height / 2
//
//                        val relativeCenterX = centerX
//                        val relativeCenterY = centerY
//                        val alphaRadians = Math.toRadians(currentRotate.toDouble())
//                        val rotatedRelativeCenterX = relativeCenterX * Math.cos(alphaRadians) - relativeCenterY * Math.sin(alphaRadians)
//                        val rotatedRelativeCenterY = relativeCenterX * Math.sin(alphaRadians) + relativeCenterY * Math.cos(alphaRadians)
//                        val rotatedCenterX = rotatedRelativeCenterX-size.width/2
//                        val rotatedCenterY = rotatedRelativeCenterY-size.height/2
//                        XLogger.d( "Rotated Offset: rotatedX:$rotatedCenterX  rotatedY:${rotatedCenterY} size:${size} rotateX:${rotateButtonX}  rotateY:${rotateButtonY}")

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
                            // 根据旋转的角度 计算 x y 的偏移量
                            val angleInRadians = Math.toRadians(currentRotate.toDouble()) // 将旋转角度转换为弧度
                            val cosAngle = cos(angleInRadians).toFloat() // 计算角度的余弦值
                            val sinAngle = sin(angleInRadians).toFloat() // 计算角度的正弦值

                            // 根据旋转角度调整水平和垂直移动量
                            val rotatedX = dragAmount.x * cosAngle - dragAmount.y * sinAngle
                            val rotatedY = dragAmount.x * sinAngle + dragAmount.y * cosAngle

                            currentImagePosition = Offset(
                                currentImagePosition.x + rotatedX * scale,
                                currentImagePosition.y + rotatedY * scale
                            )
                            XLogger.d("detectDragGestures onDrag")
                        })
                    }

//                    .pointerInput(imageData.image.hashCode()) {
//                        coroutineScope {
//                            awaitPointerEventScope {
//                                while (true) {
//                                    XLogger.d("fingers event start---------->")
//                                    var pointSize = 1
//                                    val downPointerInputChange = awaitFirstDown()
//                                    viewModel.bringImageToFront(index)
//                                    drag(
//                                        downPointerInputChange.id,
//                                        onDrag = {
//                                            pointSize = currentEvent.changes.size
//                                            if (pointSize == 1) {
//                                                viewModel.updateTouchType(TouchType.MOVE)
//                                                XLogger.d("one fingers drag---------->")
//                                                //乘以 缩放的倍数 这样无论缩放多少倍 都不会影响 移动的距离的偏移量
//                                                // 根据旋转的角度 计算 x y 的偏移量
//
//                                                // 将旋转角度转换为弧度
//                                                val angleInRadians = Math.toRadians(currentRotate.toDouble())
//                                                // 计算角度的余弦值
//                                                val cosAngle = cos(angleInRadians).toFloat()
//                                                // 计算角度的正弦值
//                                                val sinAngle = sin(angleInRadians).toFloat()
//
//                                                // 根据旋转角度调整水平和垂直移动量
//                                                val rotatedX =
//                                                    it.positionChange().x * cosAngle - it.positionChange().y * sinAngle
//                                                val rotatedY =
//                                                    it.positionChange().x * sinAngle + it.positionChange().y * cosAngle
//
//                                                // 更新位置
//                                                currentImagePosition = Offset(
//                                                    currentImagePosition.x + rotatedX * scale,
//                                                    currentImagePosition.y + rotatedY * scale
//                                                )
//                                            } else {
//                                                viewModel.updateTouchType(TouchType.SCALE_ROTATE)
//                                                XLogger.d("two fingers drag---------->")
//                                                val zoomChange = currentEvent.calculateZoom()
//                                                scale *= zoomChange
//                                                currentRotate += currentEvent.calculateRotation()
//                                            }
//                                        },
//                                    )
//
//                                    val dragUpOrCancelPointerInputChange =
//                                        awaitDragOrCancellation(downPointerInputChange.id)
//
//                                    if (dragUpOrCancelPointerInputChange == null) {
//                                        if (pointSize == 1) {
//                                            XLogger.d("one fingers drag up---------->")
//                                            viewModel.updateImagePosition(index, currentImagePosition)
//                                            viewModel.bringImageToFront(index)
//                                        } else {
//                                            XLogger.d("two fingers drag up---------->")
//                                            viewModel.updateScale(scale)
//                                            viewModel.updateIconsOffset(deleteIconOffset, rotateIconOffset, scaleIconOffset)
//                                            viewModel.updateRotate(currentRotate)
//                                        }
//                                        viewModel.updateTouchType(TouchType.NONE)
//                                    }
//                                }
//                            }
//                        }
//                    }

                    .drawBehind {
                        // 外部矩形框
                        if (currentHandleIndex == index) {
                            drawRect(
                                color = Color(0xFF0099A1),
                                style = Stroke(width = pxValue),
                            )
                        }
                    }
//                    .onSizeChanged {
//                        imageSize = it.toSize()
//                    }
            )

            if (currentHandleIndex == index) {
                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .offset {
                            IntOffset(deleteIconOffset.x.toInt(), deleteIconOffset.y.toInt())
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
                        .constrainAs(rotateRef) {
                            end.linkTo(logoRef.start, margin = (-10).dp)
                            top.linkTo(logoRef.bottom, margin = (-10).dp)
                        }
                        .size(20.dp)
                        .pointerInput(imageData.image.hashCode()) {
                            val sensitivity = 50f
                            detectDragGestures(
                                onDragStart = {
//                                    viewModel.updateTouchType(type = TouchType.ROTATE)
                                },
                                onDragCancel = {
//                                    viewModel.updateTouchType(type = TouchType.NONE)
                                },
                                onDrag = { change, _ ->
                                    val dx = change.position.x - size.width / 2
                                    val dy = change.position.y - size.height / 2
                                    currentRotate = (atan2(dy / sensitivity, dx / sensitivity) * 180 / PI).toFloat()
                                    if (currentRotate < 0) currentRotate += 360f

                                    XLogger.d("rotate drag onDrag currentRotate:$currentRotate  currentRotate:${currentRotate}  dx:${dx} dy:${dy}")
                                },
                                onDragEnd = {
                                    XLogger.d("rotate drag onDragEnd:$currentRotate")
                                    viewModel.updateRotate(currentRotate)
//                                    viewModel.updateTouchType(type = TouchType.NONE)
                                }
                            )
                        }
//                        .offset {
//                            IntOffset(rotateIconOffset.x.toInt(), rotateIconOffset.y.toInt())
//                        }
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
//                                viewModel.updateTouchType(TouchType.SCALE)
                                XLogger.d("scale drag onDragStart")
                            }, onDragEnd = {
//                                viewModel.updateTouchType(TouchType.NONE)
                                viewModel.updateScale(scale)
                                viewModel.updateIconsOffset(
                                    deleteIconOffset,
                                    rotateIconOffset,
                                    scaleIconOffset
                                )
                                XLogger.d("scale drag onDragEnd scale:${scale}")
                            }, onDragCancel = {
//                                viewModel.updateTouchType(TouchType.NONE)
                                XLogger.d("scale drag onDragCancel")
                            }, onDrag = { change, _ ->
                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
                                scale = newScale.coerceIn(0.2f, 10f) // 限制scale的取值范围
                                //imageSize = (scale * 100).dp.coerceIn(40.dp, 1000.dp)
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

fun calculateRotatedPosition(cx: Float, cy: Float, r: Float, angleDegrees: Float): PointF {
    val angleRadians = Math.toRadians(angleDegrees.toDouble()).toFloat()
    val newX = cx + r * cos(angleRadians)
    val newY = cy + r * sin(angleRadians)
    return PointF(newX, newY)
}
