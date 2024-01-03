package com.example.mycamerax.edit

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycamerax.R
import com.example.mycamerax.XLogger
import kotlinx.coroutines.launch


@Composable
fun ImageEditor( viewModel: EditorViewModel = viewModel()) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        XLogger.d("设置")
        val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_editor)
        viewModel.editorInit(bitmap)
    }
    val rootImageData = viewModel.rootImageData.collectAsState().value
    val bitmap = rootImageData.rootBitmap?.asImageBitmap()

    if (bitmap == null) {
        XLogger.d("图像为空")
        return
    }


    val editeType = rootImageData.editType
    val imageList = viewModel.currentImageList.collectAsState().value.imageList

    Column(modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ){
            //裁剪模式
            if (editeType == EditeType.CROP) {
                AddCrop(viewModel = viewModel)
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
                            .fillMaxSize()
                            .onSizeChanged {
                            }
                            .onGloballyPositioned {
                                val localToWindow = it.localToWindow(Offset.Zero)
                                //
                                val boundsInParent = it.boundsInParent()
                                XLogger.d(" localToWindow:${localToWindow} boundsInParent:${boundsInParent}")

                            }
                        ,
                        contentScale = if (bitmap.width > bitmap.height) ContentScale.FillWidth else ContentScale.FillHeight
                    )
                    //剪切一个
                    //图片、文字 多个
                    when (editeType) {
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
                        else->{}
                    }
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

//            Button(onClick = {
//            }) {
//                Text(text = "Rotate Left")
//            }
//
//            Button(onClick = {
//
//            }) {
//                Text(text = "Rotate Right")
//            }
        }
    }
}

