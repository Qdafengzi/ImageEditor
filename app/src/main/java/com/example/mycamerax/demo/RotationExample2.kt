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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import kotlin.math.PI
import kotlin.math.atan2


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RotationExample2() {
    var rotation by remember { mutableStateOf(0f) }
    // 图标的位置
    var iconPosition by remember { mutableStateOf(Offset.Zero) }

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