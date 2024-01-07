package com.example.editor

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.example.editor.databinding.ActivityImageEditorBinding
import com.example.editor.edit.crop.main.CropRequest
import com.example.editor.edit.crop.ui.ImageCropFragment
import com.example.editor.edit.crop.util.file.FileCreator
import com.example.editor.edit.crop.util.file.FileOperationRequest
import com.example.editor.editimage.EditImageFragment

class ImageEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityImageEditorBinding>(this, R.layout.activity_image_editor)

        val editImageFragment = EditImageFragment()

        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.mipmap.ic_editor))
            .appendPath(resources.getResourceTypeName(R.mipmap.ic_editor))
            .appendPath(resources.getResourceEntryName(R.mipmap.ic_editor))
            .build()

        // Save to given destination uri.
        val destinationUri = FileCreator
            .createFile(FileOperationRequest.createRandom(), this)
            .toUri()

        val cropRequest = CropRequest.Manual(
            sourceUri = uri,
            destinationUri = destinationUri,
        )
        val cropFragment = ImageCropFragment.newInstance(cropRequest)

        supportFragmentManager.beginTransaction()
            .add(R.id.frameLayout, editImageFragment)
            .commitAllowingStateLoss()

        binding.btnImage.setOnClickListener {
            try {
                supportFragmentManager.beginTransaction().replace(R.id.frameLayout, editImageFragment).commit()
                editImageFragment.addImage()
            } catch (e: Exception) {
                XLogger.d("error")
            }
        }
        binding.btnText.setOnClickListener {
            try {
                supportFragmentManager.beginTransaction().replace(R.id.frameLayout, editImageFragment).commit()
                editImageFragment.addText()
            } catch (e: Exception) {
                XLogger.d("error")
            }
        }

        binding.btnCrop.setOnClickListener {
            if (supportFragmentManager.fragments.contains(cropFragment)) {
                supportFragmentManager.beginTransaction().show(cropFragment).commitAllowingStateLoss()
            } else {
                supportFragmentManager.beginTransaction().replace(R.id.frameLayout, cropFragment).commitAllowingStateLoss()
            }
        }
        binding.btnLeft.setOnClickListener {
            cropFragment.rotate(-90f)
        }
        binding.btnRight.setOnClickListener {
            cropFragment.rotate(90f)
        }
    }
}