package com.example.editor.edit.crop.ui

import android.app.Application
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.editor.edit.aspectratio.model.AspectRatio
import com.example.editor.edit.crop.main.CropRequest
import com.example.editor.edit.crop.state.CropFragmentViewState
import com.example.editor.edit.crop.util.bitmap.BitmapUtils
import com.example.editor.edit.crop.util.bitmap.ResizedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageCropViewModel(val app: Application) : AndroidViewModel(app) {

    private var cropRequest: CropRequest? = null

    private val cropViewStateLiveData = MutableLiveData<CropFragmentViewState>()
        .apply {
            value = CropFragmentViewState(aspectRatio = AspectRatio.ASPECT_FREE)
        }

    private val resizedBitmapLiveData = MutableLiveData<ResizedBitmap>()

    fun setCropRequest(cropRequest: CropRequest) {
        this.cropRequest = cropRequest
        viewModelScope.launch(Dispatchers.IO) {
            val resizedBitmap = BitmapUtils.resize(cropRequest.sourceUri, app.applicationContext)
            this.launch(Dispatchers.Main) {
                resizedBitmapLiveData.value = resizedBitmap
            }
        }
    }

    fun getCropRequest(): CropRequest? = cropRequest

    fun getCropViewStateLiveData(): LiveData<CropFragmentViewState> = cropViewStateLiveData

    fun getResizedBitmapLiveData(): LiveData<ResizedBitmap> = resizedBitmapLiveData

    fun updateCropSize(cropRect: RectF) {
        cropViewStateLiveData.value =
            cropViewStateLiveData.value?.onCropSizeChanged(cropRect = cropRect)
    }

    fun onAspectRatioChanged(aspectRatio: AspectRatio) {
        cropViewStateLiveData.value =
            cropViewStateLiveData.value?.onAspectRatioChanged(aspectRatio = aspectRatio)
    }

    override fun onCleared() {
        super.onCleared()
    }
}