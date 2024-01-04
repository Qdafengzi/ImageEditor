package com.example.editor.edit.crop.main

import android.os.Parcelable
import androidx.annotation.ColorRes
import com.example.editor.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class CropTheme(@ColorRes val accentColor: Int) : Parcelable {

    companion object {
        fun default() = CropTheme(R.color.blue)
    }
}