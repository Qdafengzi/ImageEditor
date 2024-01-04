package com.example.editor.edit.crop.state

import android.content.Context
import android.graphics.RectF
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.editor.R
import com.example.editor.edit.aspectratio.model.AspectRatio
import com.example.editor.edit.crop.inputview.SizeInputData
import com.example.editor.edit.crop.inputview.SizeInputViewType
import com.example.editor.edit.crop.main.CropTheme
import kotlin.math.roundToInt

data class CropFragmentViewState(
    val cropTheme: CropTheme = CropTheme.default(),
    val aspectRatio: AspectRatio,
    val sizeInputData: SizeInputData? = null
) {

    fun getWidthButtonText(context: Context): Spannable {
        Log.v("TEST", "text:${cropTheme.accentColor}")
        if (sizeInputData?.widthValue?.isNaN() == true) {
            return SpannableString("")
        }
        val width = sizeInputData?.widthValue?.roundToInt().toString()
        val wordtoSpan = SpannableString("W $width")
        wordtoSpan.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, cropTheme.accentColor)),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        wordtoSpan.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)),
            1,
            wordtoSpan.length - 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return wordtoSpan
    }

    fun getHeightButtonText(context: Context): Spannable {
        if (sizeInputData?.heightValue?.isNaN() == true) {
            return SpannableString("")
        }
        val height = sizeInputData?.heightValue?.roundToInt().toString()
        val wordtoSpan = SpannableString("H $height")
        wordtoSpan.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, cropTheme.accentColor)),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        wordtoSpan.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)),
            1,
            wordtoSpan.length - 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return wordtoSpan
    }

    fun onAspectRatioChanged(aspectRatio: AspectRatio): CropFragmentViewState {
        return CropFragmentViewState(
            sizeInputData = sizeInputData,
            aspectRatio = aspectRatio,
            cropTheme = cropTheme
        )
    }

    fun onCropSizeChanged(cropRect: RectF): CropFragmentViewState {
        return CropFragmentViewState(
            sizeInputData = SizeInputData(
                type = SizeInputViewType.WIDTH,
                widthValue = cropRect.width(),
                heightValue = cropRect.height()
            ),
            aspectRatio = aspectRatio,
            cropTheme = cropTheme
        )
    }

    fun onThemeChanged(cropTheme: CropTheme): CropFragmentViewState {
        return CropFragmentViewState(
            sizeInputData = sizeInputData,
            aspectRatio = aspectRatio,
            cropTheme = cropTheme
        )
    }
}