package com.example.mycamerax.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.mycamerax.XLogger
import kotlinx.coroutines.coroutineScope


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AddCrop(viewModel: EditorViewModel) {
    val context = LocalContext.current
    val rootImageData = viewModel.rootImageData.collectAsState().value
    val cropRect = rootImageData.cropRect
    val rootBitmap = rootImageData.rootBitmap?:return


    //线的宽度
    val rectLineWidth = LocalDensity.current.run { 2.dp.toPx() }
    //四个角的直角 高度
    val rectHeight = LocalDensity.current.run { 27.dp.toPx() }
    //中间横线的宽度
    val midRectLength = LocalDensity.current.run { 28.dp.toPx() }
    //四个角的线宽度
    val outRectStrokeWidth = LocalDensity.current.run { 3.5.dp.toPx() }

    var touchFlag by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 绘制裁剪框
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            var pointSize = 1
                            val down = awaitFirstDown()
                            touchFlag = true
                            drag(down.id, onDrag = {
                                pointSize = currentEvent.changes.size
                                if (pointSize == 1) {
                                    //判断触摸的位置 和 偏移量
                                    val touchX = it.position.x
                                    val touchY = it.position.y
                                    val xChange = it.positionChange().x
                                    val yChange = it.positionChange().y
                                    XLogger.d("触摸的位置 touchX:${touchX} touchY:${touchY} xChange:$xChange yChange:$yChange")
                                    //判断如果是四边的位置 可以进行x 或y方向上的缩放


                                    //判断 如果是四个角的位置 同时进行 x 和 y 方向的缩放
                                }
                            })

                            val dragOrCancel = awaitDragOrCancellation(down.id)
                            if (dragOrCancel == null) {
                                touchFlag = false
                                //释放了
                                if (pointSize == 1) {

                                } else {

                                }

                            }
                        }
                    }
                }
            }
        ) {


            //drawImage()
            drawImage(rootBitmap.asImageBitmap(),

                )


            if (touchFlag) {
                //每个格子的宽度
                val cellWidth = (cropRect.width - rectLineWidth * 2) / 3
                //每个格子的高度
                val cellHeight = (cropRect.height - rectLineWidth * 2) / 3
                // 绘制水平线
                (0..3).forEachIndexed { index, _ ->
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            cropRect.left + rectLineWidth,
                            cropRect.top + index * cellHeight + rectLineWidth
                        ),
                        end = Offset(
                            cropRect.left + cropRect.width - rectLineWidth,
                            cropRect.top + index * cellHeight + rectLineWidth
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                //绘制竖线
                (0..3).forEachIndexed { index, _ ->
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            cropRect.left + rectLineWidth + index * cellWidth,
                            cropRect.top + rectLineWidth
                        ),
                        end = Offset(
                            cropRect.left + rectLineWidth + index * cellWidth,
                            cropRect.top + cropRect.height - rectLineWidth
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }



            //绘制 裁剪区域
            run {
                //----------------------------裁剪区域亮 非裁剪区域黑色遮罩 开始-----------------------------------------//
                val cropSize = Size(cropRect.width, cropRect.height)
                val cropOffset = Offset(cropRect.left, cropRect.top)
                val cropRectangle = Rect(
                    offset = cropOffset,
                    size = cropSize,
                )

                //裁剪区域
                val cropPath = Path().apply {
                    addRect(cropRectangle)
                }

                //整个区域
                val screenPath = Path().apply {
                    addRect(Rect(Offset.Zero, size))
                }

                //路径合成
                val combinedPath = Path().apply {
                    op(screenPath, cropPath, PathOperation.Difference)
                }

                //绘制路径
                drawPath(path = combinedPath, color = Color(0x4D000000))
                //----------------------------裁剪区域亮 非裁剪区域黑色遮罩 结束-----------------------------------------//
            }

            //绘制 四个直角
            run {
                //左上角
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left, cropRect.top),
                    end = Offset(cropRect.left + rectHeight, cropRect.top),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left, cropRect.top),
                    end = Offset(cropRect.left, cropRect.top + rectHeight),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )

                //左下角
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left, cropRect.top + cropRect.height - rectHeight),
                    end = Offset(cropRect.left, cropRect.top + cropRect.height),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left, cropRect.top + cropRect.height),
                    end = Offset(cropRect.left + rectHeight, cropRect.top + cropRect.height),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )

                //右上角
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.width + cropRect.left - rectHeight, cropRect.top),
                    end = Offset(cropRect.width + cropRect.left, cropRect.top),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.width + cropRect.left, cropRect.top),
                    end = Offset(cropRect.width + cropRect.left, cropRect.top + rectHeight),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )

                //右下角
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.width + cropRect.left, cropRect.top + cropRect.height - rectHeight),
                    end = Offset(cropRect.width + cropRect.left, cropRect.top + cropRect.height),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.width + cropRect.left, cropRect.top + cropRect.height),
                    end = Offset(cropRect.width + cropRect.left - rectHeight, cropRect.top + cropRect.height),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            //画四个边的中线
            run {
                //上
                val lineLeft = (cropRect.width - midRectLength) / 2f
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left + lineLeft, cropRect.top),
                    end = Offset(cropRect.left + lineLeft + midRectLength, cropRect.top),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )

                //下
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left + lineLeft, cropRect.top + cropRect.height),
                    end = Offset(cropRect.left + lineLeft + midRectLength, cropRect.top + cropRect.height),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )


                val lineTop = (cropRect.width - midRectLength) / 2f
                //左
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left, cropRect.top + lineTop),
                    end = Offset(cropRect.left, cropRect.top + lineTop + midRectLength),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )

                //右
                drawLine(
                    color = Color.White,
                    start = Offset(cropRect.left + cropRect.width, cropRect.top + lineTop),
                    end = Offset(cropRect.left + cropRect.width, cropRect.top + lineTop + midRectLength),
                    strokeWidth = outRectStrokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        //偏移量 和宽高 就可以定位了
//        GlideImage(
//            model = ContextCompat.getDrawable(context, R.mipmap.ic_editor),
//            contentDescription = "white crop frame",
//            modifier = Modifier.wrapContentSize(),
//        )
    }
}