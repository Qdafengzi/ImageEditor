package com.example.editor.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.editor.R

@Composable
fun ScaledImageViewWithFixedSizeIconAndSlider() {
    val imageView: Painter = painterResource(id = R.mipmap.ic_editor)
    val icon = painterResource(id = R.drawable.ic_editor_rotate)

    val scale = remember { mutableStateOf(1f) }

    Column {
        Slider(
            value = scale.value,
            onValueChange = { newValue -> scale.value = newValue },
            valueRange = 1f..3f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Box(Modifier.fillMaxWidth().aspectRatio(2f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                // Draw the base image
                with(imageView){
                    draw(size = size*scale.value)
                }


                val posX =(size.width/2f)*scale.value +  (size.width/2f) - 40.dp.toPx() * scale.value
                val posY = (size.height/2f)*scale.value +  (size.height/2f) - 40.dp.toPx() * scale.value

                // Translate and scale the icon
                translate(left = posX, top = posY) {
                    // Set the icon scale to 1 to keep its size constant during scaling
                    val iconScale = 1f
                    scale(iconScale, Offset(40. dp.toPx() / 2, 40.dp.toPx() / 2)) {
                        with(icon){
                            draw(size = Size(40.dp.toPx(),40.dp.toPx()))
                        }
                    }
                }
            }
        }

    }
}