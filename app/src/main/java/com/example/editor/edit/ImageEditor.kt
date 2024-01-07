package com.example.editor.edit

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.editor.R
import com.example.editor.XLogger
import kotlinx.coroutines.launch


@Composable
fun ImageEditor(viewModel: EditorViewModel = viewModel()) {
    XLogger.d("ImageEditor")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = Unit) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_editor)
        viewModel.editorInit(bitmap)
    }
    val rootImageData = viewModel.rootImageData.collectAsState().value
    val bitmap = rootImageData.rootBitmap?.asImageBitmap()
    if (bitmap == null) {
        XLogger.d("image is empty")
        return
    }

    val editeType = rootImageData.editType

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            //裁剪模式
            if (editeType == EditeType.CROP) {
                //AddCrop(viewModel = viewModel)
                AddCropView(viewModel = viewModel)
            } else {
                //其他模式
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color = Color.White.copy(alpha = 0.5f))
                        .onSizeChanged {
                            //中心点的位置
                            viewModel.updateCenterOffset(Offset(it.width / 2f, it.height / 2f))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "root image",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(),
                        contentScale = if (bitmap.width > bitmap.height) ContentScale.FillWidth else ContentScale.FillHeight
                    )
                    //图片、文字
                    AddImageOrText(viewModel = viewModel)
                    //遮罩
                    LogoTextTopMask(viewModel)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//            Button(onClick = {
//
//            }) {
//                Text(text = "取消")
//            }
//            Button(onClick = {
//
//            }) {
//                Text(text = "撤销")
//            }
//            Button(onClick = {
//
//            }) {
//                Text(text = "保存")
//            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                scope.launch {
                    val imageBitmap = when ((1..3).random()) {
                        1 -> BitmapFactory.decodeResource(context.resources, R.mipmap.icon11)
                        2 -> BitmapFactory.decodeResource(context.resources, R.mipmap.icon222)
                        3 -> BitmapFactory.decodeResource(context.resources, R.mipmap.icon33)
                        else -> BitmapFactory.decodeResource(context.resources, R.mipmap.icon_44)
                    }
                    viewModel.addImage(imageBitmap)
                }
            }) {
                Text(text = "addImage")
            }
//            Button(onClick = {
//
//            }) {
//                Text(text = "addTxt")
//            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                viewModel.addCrop()
            }) {
                Text(text = "Cut")
            }
            Button(onClick = {
                viewModel.cancelCrop()
            }) {
                Text(text = "取消")
            }

            Button(onClick = {
                viewModel.turnLeftOrRight(left = true, right = false)
            }) {
                Text(text = "Rotate Left")
            }

            Button(onClick = {
                viewModel.turnLeftOrRight(left = false, right = true)
            }) {
                Text(text = "Rotate Right")
            }
        }
    }
}


@Composable
fun AddImageOrText(viewModel: EditorViewModel) {
    val rootImageData = viewModel.rootImageData.collectAsState().value
    val imageList = viewModel.currentImageList.collectAsState().value.imageList
    when (rootImageData.editType) {
        EditeType.PIC -> {
            imageList.forEachIndexed { index, imageData ->
                XLogger.d("imageList draw：$imageData")
                key("${index}_${imageData.hashCode()}") {
                    AddImage(index, imageData, viewModel)
                }
            }
        }

        EditeType.TEXT -> {

        }

        EditeType.NONE -> {

        }

        else -> {}
    }
}

/**
 * 添加文字 和图的时候上面的遮罩
 */
@Composable
fun LogoTextTopMask(viewModel: EditorViewModel) {
    val rootImageData = viewModel.rootImageData.collectAsState().value
    val editeType = rootImageData.editType
    val cropRect = rootImageData.destRect

    if (editeType == EditeType.PIC || editeType == EditeType.TEXT) {
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            val cropSize = Size(cropRect.width(), cropRect.height())
            val cropOffset = Offset(cropRect.left, cropRect.top)
            val cropRectangle = Rect(
                offset = cropOffset,
                size = cropSize,
            )
            //裁剪区域
            val cropPath = Path().apply {
                addRect(cropRectangle)
            }

            //整个区域
            val screenPath = Path().apply {
                addRect(Rect(Offset.Zero, size))
            }
            //路径合成
            val combinedPath = Path().apply {
                op(screenPath, cropPath, PathOperation.Difference)
            }
            //绘制路径
            drawPath(path = combinedPath, color = Color.White)
        })
    }
}

