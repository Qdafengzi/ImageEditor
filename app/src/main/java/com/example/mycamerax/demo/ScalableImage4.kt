package com.example.mycamerax.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import com.example.mycamerax.utils.ResUtils

val Scalesize = ResUtils.dp2px(20f)

@Composable
fun ScalableImage4() {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxScale = screenWidthDp / 40f
    var scale by remember { mutableStateOf(1f) }

    var size by remember {
        mutableStateOf(40.dp)
    }


    val scaleArrowPosition = remember { mutableStateOf(Offset(0f, 0f)) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ConstraintLayout(modifier = Modifier.wrapContentSize()) {
            val (box, scaleIcon) = createRefs()
            Image(
                painter = painterResource(R.mipmap.icon11),
                contentDescription = null,
                modifier = Modifier
                    .wrapContentSize()
//                    .scale(scale = scale)
//                    .fillMaxHeight(Constraints.Infinity.toFloat())

                    .onSizeChanged {
                        XLogger.d("size:${it.width} ${it.height}")
                    }

                    .onGloballyPositioned {
                        // 获取图片的位置和大小信息
                        val scaleArrowX = it.size.width * scale - rootImageSize * (1 + scale)
                        val scaleArrowY = it.size.height * scale - rootImageSize * (1 + scale)

                        XLogger.d("缩放-------->width:${it.size.width}  scale:${scale}")
//                        val scaleArrowX = it.size.width * scale
//                        val scaleArrowY = it.size.height * scale
                        scaleArrowPosition.value = Offset(scaleArrowX, scaleArrowY)
                    }
                    .constrainAs(box) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)

//                        height = Dimension.fillToConstraints
                    }
                    .size(size)
            )

            Image(
                painter = painterResource(R.drawable.ic_editor_scale),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
//                    .offset {
//                        IntOffset(
//                            scaleArrowPosition.value.x.toInt(),
//                            scaleArrowPosition.value.y.toInt()
//                        )
//                    }
                    .constrainAs(scaleIcon) {
                        end.linkTo(box.end)
                        bottom.linkTo(box.bottom)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newScale =
                                scale * (1 + change.positionChange().y / this.size.height)
                            scale = newScale.coerceIn(0.2f, maxScale) // 限制scale的取值范围

                            size *= scale
                            XLogger.d("scale: $scale")
                        }
                    }
            )
        }
    }
}
