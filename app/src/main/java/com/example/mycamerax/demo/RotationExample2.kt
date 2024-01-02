package com.example.mycamerax.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import kotlin.math.PI
import kotlin.math.atan2


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RotationExample2() {
    // 旋转角度

    var rotation by remember { mutableStateOf(0f) }
    // 图像的中心点
    var imageCenter by remember { mutableStateOf(Offset.Zero) }
//    var imagePosition by remember { mutableStateOf(Offset.Zero) }
    // 图标的位置
    var iconPosition by remember { mutableStateOf(Offset.Zero) }
//    val maxAngle = 360f

    var imageSize by remember {
        mutableStateOf(Size(0f, 0f))
    }

    var oldRotate by remember {
        mutableStateOf(0f)
    }

    var newPosition  by remember {
        mutableStateOf(Offset.Zero)
    }

    var imagePosition  by remember {
        mutableStateOf(Offset.Zero)
    }

    var imagePositionLast  by remember {
        mutableStateOf(Offset.Zero)
    }

    // 旋转速度的调整因子，数值越大旋转速度越慢
    val speedFactor = 100

    val sensitivity = 1000f
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        ConstraintLayout {
            val (imageRef,iconRes) = createRefs()
            Image(
                painter = painterResource(id = R.mipmap.ic_editor),
                contentDescription = "Rotating Image",
                modifier = Modifier
                    .size(200.dp)
                    .constrainAs(imageRef) {

                    }
                    .graphicsLayer {
                        rotationZ = rotation
                    }
                    .onGloballyPositioned { coordinates ->
                        newPosition = coordinates.windowToLocal(Offset.Zero)
                        imageCenter = coordinates.boundsInParent().center
                        imagePosition = coordinates.localToWindow(Offset.Zero)

                        //XLogger.d("x:${imagePosition.x}  y:${imagePosition.y}")

                    }

                    .onSizeChanged {
                        imageSize = it.toSize()
                    }
            )

            Icon(
                Icons.Default.Delete,
                contentDescription = "Rotation Icon",
                modifier = Modifier
                    .padding(16.dp)
                    .constrainAs(iconRes) {
                        start.linkTo(imageRef.end)
                        top.linkTo(imageRef.bottom)
                    }
                .onGloballyPositioned { coordinates ->
                    iconPosition = coordinates.boundsInParent().center
                }
                    .pointerInput(Unit) {

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        rotation += (((angleB - angleA) * (180f / PI.toFloat())) / speedFactor).also {
//                            iconPosition = change.position
//                        }
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        // 计算向量的叉积，判断旋转方向
//                        val crossProduct = vectorA.x * vectorB.y - vectorA.y * vectorB.x
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        val angleDiff = (angleB - angleA) * (180f / PI.toFloat())
//                        rotation += if (crossProduct >= 0) {
//                            // 顺时针旋转
//                            abs(angleDiff) / speedFactor
//                        } else {
//                            // 逆时针旋转
//                            -abs(angleDiff) / speedFactor
//                        }
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        val angleDiff = (angleB - angleA) * (180f / PI.toFloat())
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x).toDegrees()
//                        val angleB = atan2(vectorB.y, vectorB.x).toDegrees()
//                        var angleDiff = (angleB - angleA) % 360
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        var angleDiff = (angleB - angleA) * (180f / PI.toFloat())
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        val angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//                        var angleDiff = angleB - angleA
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }
//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//
//                        var angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        var angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//
//                        // 将角度转换到 0 到 360 度的范围
//                        if (angleA < 0) angleA += 360
//                        if (angleB < 0) angleB += 360
//
//                        var angleDiff = angleB - angleA
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//
//                        var angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        var angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//
//                        // 将角度转换到 0 到 360 度的范围
//                        if (angleA < 0) angleA += 360
//                        if (angleB < 0) angleB += 360
//
//                        var angleDiff = angleB - angleA
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        if (angleDiff > 180) angleDiff -= 360
//                        else if (angleDiff < -180) angleDiff += 360
//                        rotation -= angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//
//                        var angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        var angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//
//                        // 将角度转换到 0 到 360 度的范围
//                        if (angleA < 0) angleA += 360
//                        if (angleB < 0) angleB += 360
//
//                        var angleDiff = angleB - angleA
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        if (angleDiff > 180) angleDiff -= 360
//                        else if (angleDiff < -180) angleDiff += 360
//
//                        // 根据旋转的方向动态更改旋转角度的更新方式
//                        if (angleDiff > 0) {
//                            rotation += angleDiff / speedFactor
//                        } else {
//                            rotation -= angleDiff / speedFactor
//                        }
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//
//                        change.previousPosition
//                        val vectorB = change.position - imageCenter
//
//                        val angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        val angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//
//                        var angleDiff = angleB - angleA
//
//                        // Normalizing the angle difference to the range [-180, 180]
//                        angleDiff %= 360
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = change.previousPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//
//                        val angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        val angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//
//                        var angleDiff = angleB - angleA
//
//                        // Normalizing the angle difference to the range [-180, 180]
//                        angleDiff %= 360
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//
//                        XLogger.d("角度：${angleDiff}")
//
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//
//                        val vectorB = change.position - imageCenter
//                        val angleB = atan2(vectorB.y, vectorB.x)
//
//                        var angleDiff = angleB - angleA
//                        angleDiff = (angleDiff * 180 / PI).toFloat()
//
//                        // 根据角度差的正负来确定旋转方向
//                        if (angleDiff > 180) {
//                            angleDiff -= 360
//                        } else if (angleDiff < -180) {
//                            angleDiff += 360
//                        }
//
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//
//                        val vectorB = change.position - imageCenter
//                        val angleB = atan2(vectorB.y, vectorB.x)
//
//                        var angleDiff = angleB - angleA
//                        angleDiff = (angleDiff * 180 / PI).toFloat()
//
//                        // 根据角度差的正负来确定旋转方向
//                        if (angleDiff > 180) {
//                            angleDiff -= 360
//                        } else if (angleDiff < -180) {
//                            angleDiff += 360
//                        }
//
//                        rotation += angleDiff / speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x) * (180f / PI.toFloat())
//                        val angleB = atan2(vectorB.y, vectorB.x) * (180f / PI.toFloat())
//                        var angleDiff = angleB - angleA
//
//                        // Normalizing the angle difference to the range [-180, 180]
//                        angleDiff %= 360
//                        if (angleDiff < -180) angleDiff += 360
//                        else if (angleDiff > 180) angleDiff -= 360
//
//                        rotation += angleDiff * speedFactor
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA = iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        // 计算向量的叉积，判断旋转方向
//                        val crossProduct = vectorA.x * vectorB.y - vectorA.y * vectorB.x
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        val angleDiff = (angleB - angleA) * (180f / PI.toFloat())
//                        rotation += if (crossProduct >= 0) {
//                            // 顺时针旋转
//                            abs(angleDiff) / speedFactor
//                        } else {
//                            // 逆时针旋转
//                            -abs(angleDiff) / speedFactor
//                        }
//                        iconPosition = change.position
//                    }

//                    detectDragGestures { change, _ ->
//                        val vectorA =   iconPosition - imageCenter
//                        val vectorB = change.position - imageCenter
//                        val angleA = atan2(vectorA.y, vectorA.x)
//                        val angleB = atan2(vectorB.y, vectorB.x)
//                        val angleDiff = (angleB - angleA) * (180f / PI.toFloat())
//
//                        // 判断旋转方向
//                        val rotationAngle = if (angleDiff >= 0) {
//                            // 顺时针旋转
//                            abs(angleDiff) / speedFactor
//                        } else {
//                            // 逆时针旋转
//                            -abs(angleDiff) / speedFactor
//                        }
//
//                        // 应用旋转
//                        rotation += rotationAngle
//                        iconPosition = change.position
//                    }
//



//                        detectDragGestures(
//                            onDrag = { change, _ ->
//                                XLogger.d("${change.position.y}  ${change.position.x}")
//                                val rotationSpeed = 0.03f // 降低旋转速度的因子
//                                val angle = atan2(imagePosition.y - imageCenter.x, imagePosition.x - imageCenter.y) * (180 / PI) * rotationSpeed
//
//                                val xDistance = abs(change.positionChange().x)
//                                val yDistance = abs(change.positionChange().y)
//
//                                val isClockwise = if (xDistance > yDistance) {
//                                    if (change.position.y > 0) {
//                                        change.positionChange().x > 0
//                                    } else {
//                                        change.positionChange().x < 0
//                                    }
//                                } else {
//                                    if (change.position.x < 0) {
//                                        change.positionChange().y > 0
//                                    } else {
//                                        change.positionChange().y < 0
//                                    }
//                                }
//                                XLogger.d("positionChange ${change.positionChange().x}  ${change.positionChange().y}")
//                                if (isClockwise) {
//                                    rotation -= angle.toFloat()
//                                } else {
//                                    rotation += angle.toFloat()
//                                }
//                            })
//                    detectDragGestures(
//                        onDragStart = {
//
//                        },
//                        onDrag = { change, dragAmount ->
//                            val xc = imageSize.width / 2
//                            val yc = -imageSize.height / 2
//                            val x = change.position.x
//                            val y = change.position.y
//                            val mCurrAngle = Math.toDegrees(atan2((x - xc).toDouble(), (yc - y).toDouble()))
//                            if (oldRotate == 0f) {
//                                oldRotate = mCurrAngle.toFloat()
//                                return@detectDragGestures
//                            }
//                            rotation = rotation + mCurrAngle.toFloat() - oldRotate
//                            oldRotate = mCurrAngle.toFloat()
//                        },
//                        onDragEnd = {
//                            oldRotate = 0f
//                        }
//                    )

//                    detectDragGestures(
//                        onDragStart = {
//                        },
//                        onDrag = { change, _ ->
//                        val x = change.position.x
//                        val y = change.position.y
//
//
//                        val diff = Math.sqrt(
//                            (change.positionChange().x * change.positionChange().x +
//                                    change.positionChange().y * change.positionChange().y).toDouble()
//                        )
//                        val rot = Math.toDegrees(
//                            Math.atan2(
//                                change.positionChange().x.toDouble(),
//                                change.positionChange().y.toDouble()
//                            )
//                        )*0.1
//
//                            rotation   = rot.toFloat()
//
//
//
//                        XLogger.d("角度：${rot}") }
//                    )

                        detectDragGestures { change, _ ->
                            val dx = change.position.x - size.width / 2
                            val dy = change.position.y - size.height / 2
                            rotation = (atan2(dy / sensitivity, dx / sensitivity) * 180 / PI).toFloat()
                            if (rotation < 0) rotation += 360f
                        }
                    }
            )
        }


    }
}

fun Float.toDegrees() = this * (180f / PI.toFloat())