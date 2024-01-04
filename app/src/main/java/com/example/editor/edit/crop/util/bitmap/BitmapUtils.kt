package com.example.editor.edit.crop.util.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.example.editor.edit.crop.ui.CroppedBitmapData
import com.example.editor.edit.crop.util.extensions.rotateBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object BitmapUtils {

    private const val MAX_SIZE = 1024


    fun saveBitmap(croppedBitmapData: CroppedBitmapData, file: File) {
        FileOutputStream(file).use { out ->
            croppedBitmapData.croppedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun resize(uri: Uri, context: Context): ResizedBitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

        var widthTemp = options.outWidth
        var heightTemp = options.outHeight
        var scale = 1

        while (true) {
            if (widthTemp / 2 < MAX_SIZE || heightTemp / 2 < MAX_SIZE)
                break
            widthTemp /= 2
            heightTemp /= 2
            scale *= 2
        }

        val resultOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        var resizedBitmap = BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(uri),
            null,
            resultOptions
        )

        resizedBitmap = resizedBitmap?.rotateBitmap(getOrientation(context.contentResolver.openInputStream(uri)))

        return ResizedBitmap(resizedBitmap)
    }

    private fun getOrientation(inputStream: InputStream?): Int {
        val exifInterface: ExifInterface
        var orientation = 0
        try {
            exifInterface = ExifInterface(inputStream!!)
            orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return orientation
    }
}