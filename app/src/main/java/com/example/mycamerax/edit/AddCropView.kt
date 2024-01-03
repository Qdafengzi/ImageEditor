package com.example.mycamerax.edit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.example.mycamerax.R
import com.example.mycamerax.databinding.ImageCropBinding
import com.example.mycamerax.edit.crop.main.CropRequest
import com.example.mycamerax.edit.crop.ui.ImageCropFragment
import com.example.mycamerax.edit.crop.util.file.FileCreator
import com.example.mycamerax.edit.crop.util.file.FileOperationRequest


@Composable
fun AddCropView() {
    AndroidView(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f), factory = {
        val binding = DataBindingUtil.inflate<ImageCropBinding>(LayoutInflater.from(it), R.layout.image_crop, null, false)
        binding.root
    }, update = {
        val bind = DataBindingUtil.bind<ImageCropBinding>(it)


        val context = it.context

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

        val activity = context.getActivity()
        activity?.let {
            val cropFragment = ImageCropFragment.newInstance(cropRequest)
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.containerCrop, cropFragment)
                .commitAllowingStateLoss()
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
