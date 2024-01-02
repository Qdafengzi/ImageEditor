package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycamerax.R
import kotlin.math.atan2
@Composable
fun RotationExample() {
    val rotation = remember { mutableStateOf(0f) }
    val prevRot = remember { mutableStateOf(0f) }
    val startRot = remember { mutableStateOf(0f) }
    val centerPoint = remember { mutableStateOf(Offset.Zero) }
    val centerPointAA = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                centerPoint.value = it.boundsInParent().center
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(id = R.mipmap.ic_editor),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(rotation.value)
                .onSizeChanged {
                    centerPointAA.value = Offset(it.center.x.toFloat(),it.center.y.toFloat())
                }
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color = Color.Red)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {

                            val delX = centerPointAA.value.x - centerPoint.value.x
                            val delY = centerPointAA.value.y - centerPoint.value.y
                            startRot.value = rotation.value
                            prevRot.value = Math.toDegrees(atan2(delY, delX).toDouble()).toFloat()
                        },
                        onDragEnd = {
                            prevRot.value = rotation.value
                        },
                        onDrag = { change, _ ->
                            val delX = change.position.x - centerPoint.value.x
                            val delY = change.position.y - centerPoint.value.y
                            val rot = Math.toDegrees(atan2(delY, delX).toDouble()).toFloat()
                            val rotDiff = prevRot.value - rot
                            val newRot = (startRot.value + rotDiff) / 1f
                            rotation.value = newRot % 360
                        }
                    )
                },
        ) {
            Text(
                text = "Rotate",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
