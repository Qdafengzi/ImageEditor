package com.example.editor.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.editor.R
import com.example.editor.edit.data.ImageData
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@Composable
fun AddImage(index: Int, imageData: ImageData, viewModel: EditorViewModel) {
    // 当前图的数据
    val currentImageData = viewModel.currentImageList.collectAsState().value
    val currentImage = currentImageData.imageList[index]

    val currentHandleIndex = currentImageData.currentIndex
    var currentImagePosition by remember { mutableStateOf(currentImage.position) }

    // 当前的旋转角度
    var currentRotate by remember { mutableStateOf(currentImage.rotate) }

    //三个按钮的偏移位置
    var scaleIconOffset by remember { mutableStateOf(currentImage.scaleIconOffset) }
    var deleteIconOffset by remember { mutableStateOf(currentImage.deleteIconOffset) }
    var rotateIconOffset by remember { mutableStateOf(currentImage.rotateIconOffset) }

    // 当前的缩放
    var scale by remember { mutableStateOf(currentImage.scale) }
    //var imageSize by remember { mutableStateOf(currentImage.imageSize) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier
            .wrapContentSize()
            .offset {
                IntOffset(currentImagePosition.x.roundToInt(), currentImagePosition.y.roundToInt())
            }
            .graphicsLayer {
                rotationZ = currentRotate
                transformOrigin = TransformOrigin.Center
            }
        ) {
            Image(
                bitmap = imageData.image.asImageBitmap(),
                contentDescription = "logo",
                modifier = Modifier
                    .padding(10.dp)
                    .wrapContentSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
//                        rotationZ =  currentRotate
                        transformOrigin = TransformOrigin.Center
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        com.example.editor.XLogger.d("onGloballyPositioned-------------->")
                        //val angleRadians = Math.toRadians(currentRotate.toDouble()).toFloat()
                        val size = layoutCoordinates.size.toSize()
                        //因为是按照中心点进行缩放 所以其缩放距离是一般的缩放比
                        val scaleValue = abs(1 - scale) / 2f

                        val widthScale = size.width * scaleValue
                        val heightScale = size.height * scaleValue

                        // 计算缩放按钮的位置
                        val scaleButtonX = widthScale * if (scale < 1) -1 else 1
                        val scaleButtonY = heightScale * if (scale < 1) -1 else 1
                        scaleIconOffset = Offset(scaleButtonX, scaleButtonY)

                        // 计算删除按钮的位置
                        val deleteButtonX = widthScale * if (scale < 1) 1 else -1
                        val deleteButtonY = heightScale * if (scale < 1) 1 else -1
                        deleteIconOffset = Offset(deleteButtonX, deleteButtonY)

                        val rotateButtonX = widthScale * if (scale < 1) 1 else -1
                        val rotateButtonY = heightScale * if (scale < 1) -1 else 1
                        rotateIconOffset = Offset(rotateButtonX, rotateButtonY)
                    }
                    //两个手指 处理缩放 和 旋转逻辑
                    .pointerInput(imageData.image.hashCode()) {
                        coroutineScope {
                            awaitPointerEventScope {
                                var pointSize = 1
                                while (true) {
                                    com.example.editor.XLogger.d("fingers event start---------->")
                                    val downPointerInputChange = awaitFirstDown()
                                    drag(
                                        downPointerInputChange.id,
                                        onDrag = {
                                            pointSize = currentEvent.changes.size
                                            if (pointSize == 1) {
                                                com.example.editor.XLogger.d("one fingers drag---------->")
                                                //乘以 缩放的倍数 这样无论缩放多少倍 都不会影响 移动的距离的偏移量
                                                // 根据旋转的角度 计算 x y 的偏移量

                                                // 将旋转角度转换为弧度
                                                val angleInRadians = Math.toRadians(currentRotate.toDouble())
                                                // 计算角度的余弦值
                                                val cosAngle = cos(angleInRadians).toFloat()
                                                // 计算角度的正弦值
                                                val sinAngle = sin(angleInRadians).toFloat()

                                                // 根据旋转角度调整水平和垂直移动量
                                                val rotatedX = it.positionChange().x * cosAngle - it.positionChange().y * sinAngle
                                                val rotatedY = it.positionChange().x * sinAngle + it.positionChange().y * cosAngle

                                                // 更新位置
                                                currentImagePosition = Offset(currentImagePosition.x + rotatedX * scale, currentImagePosition.y + rotatedY * scale)
                                            } else {
                                                com.example.editor.XLogger.d("two fingers drag---------->")
                                                val zoomChange = currentEvent.calculateZoom()
                                                scale *= zoomChange
                                                currentRotate += currentEvent.calculateRotation()
                                            }
                                        },
                                    )

                                    val dragUpOrCancelPointerInputChange =
                                        awaitDragOrCancellation(downPointerInputChange.id)

                                    if (dragUpOrCancelPointerInputChange == null) {
                                        if (pointSize == 1) {
                                            com.example.editor.XLogger.d("one fingers drag up---------->")
                                            viewModel.updateImagePosition(index, currentImagePosition)
                                        } else {
                                            com.example.editor.XLogger.d("two fingers drag up---------->")
                                            viewModel.updateScale(scale)
                                            viewModel.updateIconsOffset(deleteIconOffset, rotateIconOffset, scaleIconOffset)
                                            viewModel.updateRotate(currentRotate)
                                        }
                                        viewModel.bringImageToFront(index)
                                    }
                                }
                            }
                        }
                    }
                    .drawBehind {
                        // 外部矩形框
                        if (currentHandleIndex == index) {
                            drawRect(
                                color = Color(0xFF0099A1),
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                    }
            )

            if (currentHandleIndex == index) {
                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(deleteIconOffset.x.toInt(), deleteIconOffset.y.toInt())
                        }
                        .clickable(onClick = {
                            viewModel.deleteImage(index)
                        }),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_delete),
                    contentDescription = "delete"
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(20.dp)
                        .offset {
                            IntOffset(rotateIconOffset.x.toInt(), rotateIconOffset.y.toInt())
                        }
                        .onSizeChanged {
                            com.example.editor.XLogger.d("rotate====onSizeChanged:${it.center.x}")

                        }
                        .onPlaced {
                            val localToWindow = it.localToWindow(Offset.Zero)
                            com.example.editor.XLogger.d("rotate====onPlaced-----positionInWindow:${it.positionInWindow()}  positionInParent:${it.positionInParent()} positionInRoot:${it.positionInRoot()}")
                        }
                        .pointerInput(imageData.image.hashCode()) {
                            com.example.editor.XLogger.d("------------------>开始旋转")
                            val sensitivity = 1f
                            var startX = 0f
                            var startY = 0f
                            var dragAmountTotalX = 0f
                            var dragAmountTotalY = 0f

                            detectDragGestures(
                                onDragStart = {
                                    //记录开始旋转的位置
                                    startX = it.x
                                    startY = it.y
//                                    startX = rotateIconOffset.x
//                                    startY = rotateIconOffset.y
                                },
                                onDragCancel = {

                                },
                                onDrag = { _, dragAmount ->
//                                    val dx = change.position.x - size.width / 2
//                                    val dy = change.position.y - size.height / 2
                                    // 旋转过程中记录X的总偏移量
                                    dragAmountTotalX += dragAmount.x
                                    dragAmountTotalY += dragAmount.y
                                    val dx = startX + dragAmountTotalX - size.width / 2
                                    val dy = startY + dragAmountTotalY - size.height / 2
                                    com.example.editor.XLogger.d("开始旋转------------>dragAmount:${dragAmount}   dx:$dx  dy$dy")
                                    currentRotate = (atan2(dy / sensitivity, dx / sensitivity) * 180 / PI).toFloat()
                                    if (currentRotate < 0) {
                                        currentRotate += 360f
                                    }
                                    com.example.editor.XLogger.d("rotate drag onDrag currentRotate:$currentRotate  currentRotate:${currentRotate}  dx:${dx} dy:${dy}")
                                },
                                onDragEnd = {
                                    com.example.editor.XLogger.d("rotate drag onDragEnd:$currentRotate")
                                    viewModel.updateRotate(currentRotate)
                                    viewModel.updateIconsOffset(deleteIconOffset, rotateIconOffset, scaleIconOffset)
                                }
                            )
                        },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_rotate),
                    contentDescription = "rotate"
                )

                Image(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .offset {
                            IntOffset(scaleIconOffset.x.toInt(), scaleIconOffset.y.toInt())
                        }
                        .pointerInput(imageData.image.hashCode()) {
                            detectDragGestures(onDragStart = {
                                com.example.editor.XLogger.d("scale drag onDragStart")
                            }, onDragEnd = {
                                viewModel.updateScale(scale)
                                viewModel.updateIconsOffset(deleteIconOffset, rotateIconOffset, scaleIconOffset)
                                com.example.editor.XLogger.d("scale drag onDragEnd scale:${scale}")
                            }, onDragCancel = {
                                com.example.editor.XLogger.d("scale drag onDragCancel")
                            }, onDrag = { change, _ ->
                                val newScale = scale * (1 + change.positionChange().y / this.size.height)
                                scale = newScale.coerceIn(0.2f, 10f) // 限制scale的取值范围
                                //imageSize = (scale * 40).dp.coerceIn(40.dp, 1000.dp)
                                com.example.editor.XLogger.d("scale drag onDrag scale:$scale")
                            })
                        },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_scale),
                    contentDescription = "scale"
                )
            }
        }
    }
}

