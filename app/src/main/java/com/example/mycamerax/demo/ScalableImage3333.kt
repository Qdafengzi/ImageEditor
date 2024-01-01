package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils

val rootImageSize = ResUtils.dp2px(40f)

@Composable
fun ScalableImage3333() {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxScale = screenWidthDp / 40f
    var scale by remember { mutableStateOf(1f) }
    val scaleArrowPosition = remember { mutableStateOf(Offset(0f, 0f)) }
    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ConstraintLayout(modifier = Modifier.wrapContentSize()) {
            val (box, scaleIcon) = createRefs()
            Image(
                painter = painterResource(R.mipmap.icon11),
                contentDescription = null,
                modifier = Modifier
                    .wrapContentSize()
                    .scale(scale = scale)
                    .onGloballyPositioned {
                        // 获取图片的位置和大小信息
//                        val scaleArrowX = it.size.width * scale - rootImageSize * (1 + scale)
//                        val scaleArrowY = it.size.height * scale - rootImageSize * (1 + scale)

                        XLogger.d("缩放-------->width:${it.size.width}  scale:${scale}")
//                        val scaleArrowX = it.size.width * scale
//                        val scaleArrowY = it.size.height * scale


                        //scale <1的时候  减去缩小的部分
                        val scaleArrowX = if (scale < 1) {
                            it.size.width * (1 - scale)
                        } else if (scale > 1) {
                            it.size.width * (1 - scale)
                        } else {
                            1f
                        }

                        val scaleArrowY = if (scale < 1) {
                            it.size.height * (1 - scale)
                        } else if (scale > 1) {
                            it.size.height * (1 - scale)
                        } else {
                            1f
                        }



                        scaleArrowPosition.value = Offset(scaleArrowX, scaleArrowY)
                    }
                    .constrainAs(box) {
                    }
            )

            Image(
                painter = painterResource(R.drawable.ic_editor_scale),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(scaleIcon) {
                        end.linkTo(box.end)
                        bottom.linkTo(box.bottom)
                    }
                    .offset {
                        IntOffset(
                            scaleArrowPosition.value.x.toInt(),
                            scaleArrowPosition.value.y.toInt()
                        )
                    }

                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newScale =
                                scale * (1 + change.positionChange().y / this.size.height)
                            scale = newScale.coerceIn(0.2f, maxScale) // 限制scale的取值范围
                            XLogger.d("scale: $scale")
                        }
                    }
            )
        }
    }
}