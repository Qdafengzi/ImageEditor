package com.example.mycamerax.edit.crop.main

import android.net.Uri
import android.os.Parcelable
import com.lyrebirdstudio.croppy.aspectratio.model.AspectRatio
import kotlinx.parcelize.Parcelize

@Parcelize
open class CropRequest(
    open val sourceUri: Uri,
    open val excludedAspectRatios: List<AspectRatio>,
) : Parcelable {
    @Parcelize
    class Manual(
        override val sourceUri: Uri,
        val destinationUri: Uri,
        override val excludedAspectRatios: List<AspectRatio> = arrayListOf(),
    ) : CropRequest(sourceUri, excludedAspectRatios)

    @Parcelize
    class Auto(
        override val sourceUri: Uri,
        val storageType: StorageType = StorageType.EXTERNAL,
        override val excludedAspectRatios: List<AspectRatio> = arrayListOf(),
    ) : CropRequest(sourceUri, excludedAspectRatios)

    companion object {
        fun empty(): CropRequest =
            CropRequest(Uri.EMPTY,  arrayListOf())
    }
}


