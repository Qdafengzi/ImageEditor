package com.example.editor.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.editor.R


@Composable
fun ScaleDemo() {
    var scale by remember { mutableStateOf(1f) }
    var size by remember {
        mutableStateOf(100f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier
            .size(size.dp)
            .offset { IntOffset(100,100) }
           , contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_editor),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
//                    .graphicsLayer {
//                        scaleY = scale
//                        scaleX = scale
//                        transformOrigin = TransformOrigin.Center
//                    }
            )
        }


        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 160.dp)
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newScale = scale * (1 + change.positionChange().y / this.size.height)
                        scale = newScale.coerceIn(0.2f, 10f)
                        size *= scale
                    }
                }
        )
    }
}
