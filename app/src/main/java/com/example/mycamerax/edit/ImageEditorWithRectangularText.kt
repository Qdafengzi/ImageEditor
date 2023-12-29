package com.example.mycamerax.edit

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.zoomBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ImageEditorWithRectangularText() {
    val context = LocalContext.current
    val pxValue = LocalDensity.current.run { 1.dp.toPx() }
    val paddingValue = LocalDensity.current.run { 10.dp.toPx() }

    val textContent = remember() {
        mutableStateOf("哈哈哈哇哈哈哈")
    }
    val rotationAngle = remember() {
        mutableStateOf(0f)
    }

    val minScale = 0.5f
    val maxScale = 2f
    var scale by remember { mutableStateOf(1f) }

    val hide = remember {
        mutableStateOf(false)
    }

    val imageBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.mipmap.icon11
    ).asImageBitmap()


    val scope = rememberCoroutineScope()

    // 0 缩放 1 旋转
    var type = remember {
        mutableStateOf(0)
    }

    //字体大小
    val fontSize = remember {
        derivedStateOf {
            scale * 20
        }
    }

    val canScale = remember {
        derivedStateOf {
            fontSize.value > 8f && fontSize.value < 30f
        }
    }


    var center by remember {
        mutableStateOf(IntOffset(0, 0))
    }

    var isRotating by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        XLogger.d("zoomChange:$zoomChange  rotationChange:${rotationChange}  panChange:${panChange.y}")
        if (type.value == 0) {
            scale += (zoomChange / 10) * 0.1f
        } else {
            rotationAngle.value = rotationAngle.value + (rotationChange) * 10f
        }
    }


    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }


    var initialTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var initialAngle by remember { mutableStateOf(0f) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged {
                offsetX= it.width/2f
                offsetY= it.height/2f
            }
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (!hide.value) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .offset {
                    IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                }
                .graphicsLayer {
                    rotationZ = rotationAngle.value
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                }
                .transformable(state = transformState)
                .drawBehind {
                    drawRect(
                        color = Color(0xFF0099A1),
                        style = Stroke(width = pxValue),
                        topLeft = Offset(paddingValue, paddingValue),
                        size = Size(
                            this.size.width - 2 * paddingValue,
                            this.size.height - 2 * paddingValue
                        )
                    )
                }
                .onSizeChanged {
                    XLogger.d("拖动--------->size change ${it.center}")
                    if (type.value == 0) {
                        return@onSizeChanged
                    }
                    center = it.center
                }
        ) {
            Text(
                text = textContent.value,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(10.dp)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        XLogger.d("拖动 offset---------->")
                        detectDragGestures { change, dragAmount ->
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                color = Color.Black,
                fontSize = (fontSize.value).sp,

                )
            Image(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable(onClick = {
                        hide.value = true
                    }),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_delete),
                contentDescription = "delete"
            )
            Image(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                initialAngle = calculateAngle(
                                    offset.x,
                                    offset.y,
                                    center.x.toFloat(),
                                    center.y.toFloat()
                                )

                            },
                            onDrag = { change, _ ->
                                val rotationCurrentAngle = calculateAngle(
                                    change.position.x,
                                    change.position.y,
                                    center.x.toFloat(),
                                    center.y.toFloat()
                                )
                                val rotationDelta = rotationCurrentAngle - initialAngle
                                rotationAngle.value += rotationDelta
                            },
                            onDragEnd = {

                            }
                        )
                    },
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_rotate),
                contentDescription = "rotate"
            )

            Image(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { centroid: Offset, pan: Offset, zoom: Float, rotation: Float ->
                                scope.launch {
                                    type.value = 0
                                    XLogger.d("zoom======${zoom}=====>y:${pan.y}  x:${pan.x}")
                                    transformState.zoomBy(pan.x)
                                }
                            }
                        )
                    },
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_editor_scale),
                contentDescription = "scale"
            )
        }
    }
}


private fun calculateAngle(x: Float, y: Float, width: Float, height: Float): Float {
    val centerX = (width) / 2f
    val centerY = (height) / 2f

    val dx = x - centerX
    val dy = y - centerY

    return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
}