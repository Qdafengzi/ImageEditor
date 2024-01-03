package com.example.mycamerax.edit.crop.main

import android.os.Parcelable
import androidx.annotation.ColorRes
import com.example.mycamerax.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class CropTheme(@ColorRes val accentColor: Int) : Parcelable {

    companion object {
        fun default() = CropTheme(R.color.blue)
    }
}