package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mycamerax.R
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun RotationExample3() {
    var angle by remember { mutableStateOf(0f) }
    val maxAngle = 360f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()


    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_editor),
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin.Center
                }
        )

        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val dx = change.position.x - size.width/2
                        val dy = change.position.y - size.height/2
                        angle = (atan2(dy, dx) * 180 / PI).toFloat()
                        if (angle < 0) angle += maxAngle
                    }
                }
        )
    }
}
