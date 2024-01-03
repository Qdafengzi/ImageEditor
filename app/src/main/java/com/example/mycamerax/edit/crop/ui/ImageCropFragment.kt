package com.example.mycamerax.edit.crop.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.mycamerax.R
import com.example.mycamerax.databinding.FragmentImageCropBinding
import com.example.mycamerax.edit.crop.main.CropRequest
import com.example.mycamerax.edit.crop.main.CropTheme
import com.example.mycamerax.edit.crop.util.delegate.inflate
import com.example.mycamerax.edit.crop.state.CropFragmentViewState
import com.example.mycamerax.edit.crop.util.bitmap.BitmapUtils
import com.example.mycamerax.edit.crop.util.file.FileCreator
import com.example.mycamerax.edit.crop.util.file.FileExtension
import com.example.mycamerax.edit.crop.util.file.FileOperationRequest

class ImageCropFragment : Fragment() {

    private val binding: FragmentImageCropBinding by inflate(R.layout.fragment_image_crop)

    private lateinit var viewModel: ImageCropViewModel


    var onCancelClicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)[ImageCropViewModel::class.java]
        val cropRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(KEY_BUNDLE_CROP_REQUEST, CropRequest::class.java)
                ?: CropRequest.empty()
        } else {
            arguments?.getParcelable(KEY_BUNDLE_CROP_REQUEST) ?: CropRequest.empty()
        }
        viewModel.setCropRequest(cropRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.getCropRequest()?.let {
            binding.cropView.setTheme(CropTheme(R.color.white))
            binding.recyclerViewAspectRatios.setActiveColor(R.color.blue)
            binding.recyclerViewAspectRatios.excludeAspectRatio(*it.excludedAspectRatios.toTypedArray())
        }

        binding.recyclerViewAspectRatios.setItemSelectedListener {
            binding.cropView.setAspectRatio(it.aspectRatioItem.aspectRatio)
            viewModel.onAspectRatioChanged(it.aspectRatioItem.aspectRatio)
        }

        binding.imageViewCancel.setOnClickListener {
            //取消
            onCancelClicked?.invoke()
        }

        binding.imageViewApply.setOnClickListener {
            viewModel.getCropRequest()?.let { cropRequest->
                when (cropRequest) {
                    is CropRequest.Manual -> {
                        BitmapUtils
                            .saveBitmap(binding.cropView.getCroppedData(), cropRequest.destinationUri.toFile())
                    }
                    is CropRequest.Auto -> {
                        val destinationUri = FileCreator.createFile(
                            FileOperationRequest(
                                cropRequest.storageType,
                                System.currentTimeMillis().toString(),
                                FileExtension.PNG
                            ),
                            requireContext()
                        ).toUri()
                        BitmapUtils
                            .saveBitmap(binding.cropView.getCroppedData(), destinationUri.toFile())
                    }
                }
            }
        }

        with(binding.cropView) {
            onInitialized = {
                viewModel.updateCropSize(binding.cropView.getCropSizeOriginal())
            }
            observeCropRectOnOriginalBitmapChanged = {
                viewModel.updateCropSize(binding.cropView.getCropSizeOriginal())
            }
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel
            .getCropViewStateLiveData()
            .observe(this, Observer(this@ImageCropFragment::renderViewState))

        viewModel
            .getResizedBitmapLiveData()
            .observe(this, Observer { binding.cropView.setBitmap(it.bitmap) })

    }

    override fun onResume() {
        super.onResume()
        binding.recyclerViewAspectRatios.reset()
    }

    private fun renderViewState(cropFragmentViewState: CropFragmentViewState) {
        binding.viewState = cropFragmentViewState
        binding.executePendingBindings()
    }

    companion object {
        private const val KEY_BUNDLE_CROP_REQUEST = "KEY_BUNDLE_CROP_REQUEST"

        @JvmStatic
        fun newInstance(cropRequest: CropRequest): ImageCropFragment {
            return ImageCropFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_BUNDLE_CROP_REQUEST, cropRequest)
                }
            }
        }
    }
}