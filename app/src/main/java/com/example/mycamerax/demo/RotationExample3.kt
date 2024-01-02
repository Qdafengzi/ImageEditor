package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import com.example.mycamerax.R
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun RotationExample3() {
    var angle by remember { mutableStateOf(0f) }
    val maxAngle = 360f
    val centerPoint = remember { mutableStateOf(Offset.Zero) }
    val sensitivity = 50f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                centerPoint.value = it.boundsInParent().center
            }

    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_editor),
            contentDescription = null,
            modifier = Modifier
                .pointerInput(Unit){
                    detectDragGestures { change, dragAmount ->
                        if (change != null) {
                            val dx = change.position.x - size.width / 2
                            val dy = change.position.y - size.height / 2
                            angle = (atan2(dy / sensitivity, dx / sensitivity) * 180 / PI).toFloat()
                            if (angle < 0) angle += maxAngle
                        }
                    }
                }

                .graphicsLayer {
                    rotationZ = angle
                }

        )

//        Icon(
//            imageVector = Icons.Default.Delete,
//            contentDescription = null,
//            modifier = Modifier
//                .align(Alignment.Center)
//                .padding(16.dp)
//                .size(48.dp)
//                .pointerInput(Unit) {
//                    detectDragGestures { change, _ ->
//                        XLogger.d("=======>${change.position.x}    ${centerPoint.value.x}")
//                        val dx = change.position.x - centerPoint.value.x
//                        val dy = change.position.y - centerPoint.value.y
//                        angle = (atan2(dy, dx) * 180 / PI).toFloat()
//                        if (angle < 0) angle += maxAngle
//                    }
//                }
//        )
    }
}
