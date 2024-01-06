package com.example.editor.edit

import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.editor.R
import com.example.editor.XLogger
import com.example.editor.edit.data.ImageData
import com.example.editor.edit.data.TouchType
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@Composable
fun AddImage2(index: Int, imageData: ImageData, viewModel: EditorViewModel) {
    XLogger.d("addImage2")
    // 当前图的数据
    val currentImageData = viewModel.currentImageList.collectAsState().value
    val currentImage = currentImageData.imageList[index]

    val deletePainter: Painter = painterResource(id = R.drawable.ic_editor_delete)
    val rotatePainter: Painter = painterResource(id = R.drawable.ic_editor_rotate)
    val scalePainter: Painter = painterResource(id = R.drawable.ic_editor_scale)
    val currentHandleIndex = currentImageData.currentIndex
    var currentImagePosition by remember { mutableStateOf(currentImage.position) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // 当前的旋转角度
    var currentRotate by remember { mutableStateOf(currentImage.rotate) }

    var deleteRectF  by remember {
        mutableStateOf(currentImage.deleteRectF)
    }
    var rotateRectF  by remember {
        mutableStateOf(currentImage.rotateRectF)
    }
    var scaleRectF  by remember {
        mutableStateOf(currentImage.scaleRectF)
    }

    // 当前的缩放
    var scale by remember { mutableStateOf(currentImage.scale) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Image(
            bitmap = imageData.image.asImageBitmap(),
            contentDescription = "logo",
            modifier = Modifier
                .wrapContentSize()
                .offset {
                    IntOffset(currentImagePosition.x.roundToInt(), currentImagePosition.y.roundToInt())
                }
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                }
                .rotate(currentRotate)
                .scale(scale)
                .pointerInput(imageData.image.hashCode()) {
                    coroutineScope {
                        awaitPointerEventScope {
                            var pointSize = 1
                            val maxScaleWith = screenWidthDp.dp.toPx() / currentImage.defaultSize.width
                            val maxScaleHeight = screenWidthDp.dp.toPx() / currentImage.defaultSize.height
                            val maxScale = minOf(maxScaleWith, maxScaleHeight)
                            var touchType: TouchType = TouchType.NONE

                            while (true) {
                                val down = awaitFirstDown()
                                XLogger.d("fingers event start-${deleteRectF}--------->${down.position.x} ${down.position.y}")
                                //如果是删除区域
                                XLogger.d("按钮位置测试 点击:${down.position}")
                                if (currentHandleIndex == index && deleteRectF.contains(down.position.x, down.position.y)) {
                                    XLogger.d("区域检测 删除按钮区域")
                                    viewModel.deleteImage(index)
                                } else if (currentHandleIndex == index && rotateRectF.contains(down.position.x, down.position.y)) {
                                    XLogger.d("按钮位置测试 旋转:${down.position}")
                                    touchType = TouchType.ROTATE
                                    XLogger.d("区域检测 旋转按钮区域")
                                } else if (currentHandleIndex == index && scaleRectF.contains(down.position.x, down.position.y)) {
                                    XLogger.d("按钮位置测试 缩放:${down.position}")
                                    touchType = TouchType.SCALE
                                    XLogger.d("区域检测 缩放按钮区域")
                                } else {
                                    XLogger.d("区域检测 其他区域")
                                    touchType = TouchType.NONE
                                }

                                drag(
                                    down.id,
                                    onDrag = {
                                        pointSize = currentEvent.changes.size
                                        if (pointSize == 1) {
                                            XLogger.d("one fingers drag---------->")
                                            when (touchType) {
                                                TouchType.NONE -> {
                                                    XLogger.d("区域检测 触摸移动区域handle-------------------->")
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


                                                    val positionY = currentImagePosition.y + rotatedY * scale

                                                    XLogger.d("new position:${positionY}  width:${screenWidthDp.dp.toPx()} 20:${20.dp.toPx()} positionY:${positionY}")

                                                    // 更新位置
                                                    currentImagePosition = Offset(currentImagePosition.x + rotatedX * scale, positionY)
                                                }

                                                TouchType.SCALE -> {
                                                    XLogger.d("区域检测 缩放区域handle-------------------->")
                                                    val newScale = scale * (1 + it.positionChange().y / size.height)
                                                    XLogger.d("maxScale:${maxScale}")
                                                    scale = newScale.coerceIn(0.2f, maxScale) // 限制scale的取值范围
                                                }

                                                TouchType.ROTATE -> {
                                                    XLogger.d("区域检测 旋转区域handle-------------------->")
                                                    val dx = it.position.x - size.width / 2
                                                    val dy = it.position.y - size.height / 2
                                                    currentRotate = (atan2(dy, dx) * 180 / PI).toFloat()
                                                    if (currentRotate < 0) currentRotate += 360f
                                                }
                                            }
                                        } else {
                                            XLogger.d("two fingers drag---------->")
                                            val zoomChange = currentEvent.calculateZoom()
                                            val newScale = scale * zoomChange
                                            scale = newScale.coerceIn(0.2f, maxScale)
                                            currentRotate += currentEvent.calculateRotation()
                                        }
                                    },
                                )

                                val dragUpOrCancelPointerInputChange =
                                    awaitDragOrCancellation(down.id)

                                if (dragUpOrCancelPointerInputChange == null) {
                                    if (pointSize == 1) {
                                        XLogger.d("one fingers drag up---------->$touchType")
                                        when (touchType) {
                                            TouchType.SCALE -> {
                                                viewModel.updateScale(scale)
                                            }

                                            TouchType.ROTATE -> {
                                                viewModel.updateRotate(currentRotate)
                                            }

                                            else -> {
                                                viewModel.updateImagePosition(index, currentImagePosition)
                                            }
                                        }
                                    } else {
                                        XLogger.d("two fingers drag up---------->")
                                        viewModel.updateScale(scale)
                                        viewModel.updateRotate(currentRotate)
                                    }
                                    viewModel.bringImageToFront(index)
                                }
                            }
                        }
                    }
                }
                .padding(10.dp)
                .drawWithContent {
                    drawContent()
                    XLogger.d("=========>drawContent")
                    //绘制四个角
                    if (currentHandleIndex == index) {
                        val frameOffsetTop = -1.dp.toPx()
                        val frameOffsetInterval = 2.dp.toPx()
                        val strokeWidth = 1.dp.toPx() / scale
                        drawRect(
                            color = Color(0xFF0099A1),
                            topLeft = Offset(frameOffsetTop, frameOffsetTop),
                            size = Size(size.width + frameOffsetInterval, size.height + frameOffsetInterval),
                            style = Stroke(width = strokeWidth),
                        )

                        val iconWidth = 20.dp.toPx()
                        //绘制三个控件
                        //10dp 放大   10dp (10dp*scale + iconWidth/2f)
                        //左侧padding的缩放长度 -
                        //删除按钮的区域
                        val iconHalf = 10.dp.toPx()
                        // 最初10dp 不做缩放  10dp 缩放后 要减去不做缩放的icon
                        //val deletePadding = -iconHalf * scale + (iconHalf * scale - iconHalf / scale)
                        val deletePadding = -iconHalf / scale
                        translate(left = deletePadding, top = deletePadding) {
                            scale(scale = 1f) {
                                with(deletePainter) {
                                    draw(size = Size(iconWidth / scale, iconWidth / scale))
                                }
                            }
                        }

                        val rotateTop = size.height - iconHalf / scale
                        translate(left = deletePadding, top = rotateTop) {
                            scale(scale = 1f) {
                                with(rotatePainter) {
                                    draw(size = Size(iconWidth / scale, iconWidth / scale))
                                }
                            }
                        }
                        //drawCircle(color = Color.Red.copy(alpha = 0.4f), radius = 10.dp.toPx(), center = Offset(0f, rotateTop))


                        val scaleLeft = size.width - iconHalf / scale
                        XLogger.d("触摸缩放：$scale height:${size.height} deletePadding:${deletePadding} rotateTop:${rotateTop}  width:${size.width} center:${this.center} ")
                        translate(left = scaleLeft, top = rotateTop) {
                            scale(scale = 1f) {
                                with(scalePainter) {
                                    draw(size = Size(iconWidth / scale, iconWidth / scale))
                                }
                            }
                        }

                        //更新 删除  旋转 和缩放的触摸区域
                        deleteRectF = RectF(-iconHalf, -iconHalf, iconHalf * 2, iconHalf * 2)
                        //drawRect(color = Color.Green, topLeft = Offset(-iconHalf,-iconHalf),Size(abs(iconHalf * 2+iconHalf),abs(iconHalf*2+iconHalf)))
                        rotateRectF = RectF(-iconHalf, size.height - iconHalf * 2, iconHalf * 2, size.height + iconHalf * 2)
                        //drawRect(color = Color.Green, topLeft = Offset(-iconHalf,size.height - iconHalf * 2),Size(abs(iconHalf * 2+iconHalf),abs(size.height + iconHalf*2 - (size.height - iconHalf * 2))))
                        scaleRectF = RectF(size.width - iconHalf * 2, size.height - iconHalf * 2, size.width + iconHalf, size.height + iconHalf * 2)
                        //XLogger.d("按钮位置测试 绘制:${scaleRectF}")
                        //drawRect(color = Color.Green, topLeft = Offset(size.width - iconHalf*2,size.height - iconHalf * 2),Size(abs(size.width + iconHalf -(size.width - iconHalf*2)),abs(size.height + iconHalf*2 - (size.height - iconHalf * 2))))
                    }
                },
            contentScale = if (imageData.image.width > imageData.image.height) ContentScale.FillWidth else ContentScale.FillHeight
        )
    }
}