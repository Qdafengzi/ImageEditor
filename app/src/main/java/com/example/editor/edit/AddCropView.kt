package com.example.editor.edit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.editor.R
import com.example.editor.XLogger
import com.example.editor.databinding.ImageCropBinding
import com.example.editor.edit.crop.main.CropRequest
import com.example.editor.edit.crop.ui.ImageCropFragment
import com.example.editor.edit.crop.util.file.FileCreator
import com.example.editor.edit.crop.util.file.FileOperationRequest


@Composable
fun AddCropView(viewModel: EditorViewModel,lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {

    val turnClickData = viewModel.turnClickData.collectAsState().value
    val cropFragmentState = remember { mutableStateOf<ImageCropFragment?>(null) }
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        // 创建一个观察者来触发我们所记得的回调来发送事件
        val observer = LifecycleEventObserver { _, event ->
            when (event.targetState) {
                Lifecycle.State.DESTROYED -> {
                    XLogger.d("=========onDestroy")

                    if (cropFragmentState.value != null) {

                        val activity = context.getActivity()
                        activity?.supportFragmentManager?.beginTransaction()?.remove(cropFragmentState.value!!)?.commitAllowingStateLoss()
                    }
                }
                else->{}
            }
        }
        // 将观察者添加到生命周期中
        lifecycleOwner.lifecycle.addObserver(observer)
        // 当组合函数离开时，会执行
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    XLogger.d("旋转-------》${turnClickData.turnLeft} ${turnClickData.turnRight}")

    CropView{
        cropFragmentState.value = it
    }

    LaunchedEffect(turnClickData) {
        cropFragmentState.value?.let { fragment ->
            if (turnClickData.turnLeft) {
                XLogger.d("左旋转")
                fragment.turnLeft()
                viewModel.turnLeftOrRight(left = false, right = false)
            }
            if (turnClickData.turnRight) {
                XLogger.d("右旋转")
                fragment.turnRight()
                viewModel.turnLeftOrRight(left = false, right = false)
            }
        }
    }
}

@Composable
fun CropView(callback: (imageCropFragment: ImageCropFragment) -> Unit) {
    val context = LocalContext.current
    val activity = context.getActivity()
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), factory = {
            val binding = DataBindingUtil.inflate<ImageCropBinding>(LayoutInflater.from(it), R.layout.image_crop, null, false)
            binding.root
        }, update = {
            XLogger.d("AndroidView update fragment bind")
            val bind = DataBindingUtil.bind<ImageCropBinding>(it)
            val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.resources.getResourcePackageName(R.mipmap.ic_editor))
                .appendPath(context.resources.getResourceTypeName(R.mipmap.ic_editor))
                .appendPath(context.resources.getResourceEntryName(R.mipmap.ic_editor))
                .build()

            // Save to given destination uri.
            val destinationUri = FileCreator
                .createFile(FileOperationRequest.createRandom(), context)
                .toUri()

            val cropRequest = CropRequest.Manual(
                sourceUri = uri,
                destinationUri = destinationUri,
            )
            activity?.let {
                val cropFragment = ImageCropFragment.newInstance(cropRequest)
                activity.supportFragmentManager.beginTransaction()
                    .add(R.id.containerCrop, cropFragment)
                    .commitAllowingStateLoss()
                callback.invoke(cropFragment)
            }
        })
}


fun Context.getActivity(): AppCompatActivity? {
    return if (this is AppCompatActivity) {
        this
    } else {
        this.contextWrapper?.getActivity()
    }
}

val Context.contextWrapper: Context?
    get() = if (this is android.content.ContextWrapper) this else null
