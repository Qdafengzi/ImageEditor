package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R

@Composable
fun ScaleWithFixedButtonsExample() {
    var scale by remember { mutableStateOf(1f) }
    val imageSize = 200.dp * scale

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Slider(modifier = Modifier.align(Alignment.TopStart),value = scale, onValueChange = { scale = it }, valueRange = 0.1f..2f)

        ConstraintLayout {
            val (image, button1, button2, button3, button4) = createRefs()

            Image(
                painter = painterResource(id = R.mipmap.ic_editor),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp) // original size
                    .offset((-100.dp * (1 - scale)), (-100.dp * (1 - scale))) // offset to maintain center
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ) // scale rendering
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            )

            // Buttons
            Button(onClick = {}, Modifier.constrainAs(button1) {
                top.linkTo(image.top)
                start.linkTo(image.start)
            }) { Text("1") }

            Button(onClick = {}, Modifier.constrainAs(button2) {
                top.linkTo(image.top)
                end.linkTo(image.end)
            }) { Text("2") }

            Button(onClick = {}, Modifier.constrainAs(button3) {
                bottom.linkTo(image.bottom)
                start.linkTo(image.start)
            }) { Text("3") }

            Button(onClick = {}, Modifier.constrainAs(button4) {
                bottom.linkTo(image.bottom)
                end.linkTo(image.end)
            }) { Text("4") }
        }
    }
}
