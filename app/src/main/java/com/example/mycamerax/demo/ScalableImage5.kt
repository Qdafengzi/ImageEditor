package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger

@Composable
fun ScalableImage5() {
    var imageSize by remember { mutableStateOf(100.dp) }
    var scale by remember { mutableStateOf(1f) }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ConstraintLayout(modifier = Modifier.waterfallPadding()) {
            val (imgRef, iconRef) = createRefs()
            Image(
                painter = painterResource(id = R.mipmap.ic_editor),
                contentDescription = "example image",
                modifier = Modifier
                    .size(imageSize)
                    .constrainAs(imgRef) {
                    }
                    .background(color = Color.Red)
                    .drawWithContent {
                        drawContent()
                        drawCircle(color= Color.Blue, radius = 20f)
                    }
                    .onSizeChanged {
                        XLogger.d("------------>${it.width} ${it.height}")
                    }
                    .onGloballyPositioned {
                        XLogger.d("onGloballyPositioned:${it.positionInRoot().x}  ${it.positionInRoot().y}")
                    }

            )

            Image(
                painter = painterResource(id = R.drawable.ic_editor_scale),
                contentDescription = "example image",
                modifier = Modifier
                    .constrainAs(iconRef) {
                        start.linkTo(imgRef.end)
                        top.linkTo(imgRef.bottom)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newScale = scale * (1 + change.positionChange().y / this.size.height)
                            scale = newScale.coerceIn(0.2f, 10f) // 限制scale的取值范围
                            imageSize = (scale * 100).dp.coerceIn(40.dp, 1000.dp)
                            XLogger.d("scale: $scale")
                        }
                    }
            )
        }
    }
}
