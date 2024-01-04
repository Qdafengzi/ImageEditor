package com.example.editor.edit.crop.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.editor.R
import com.example.editor.databinding.FragmentImageCropBinding
import com.example.editor.edit.crop.main.CropRequest
import com.example.editor.edit.crop.main.CropTheme
import com.example.editor.edit.crop.state.CropFragmentViewState
import com.example.editor.edit.crop.util.bitmap.BitmapUtils
import com.example.editor.edit.crop.util.file.FileCreator
import com.example.editor.edit.crop.util.file.FileExtension
import com.example.editor.edit.crop.util.file.FileOperationRequest

class ImageCropFragment : Fragment() {

    private lateinit var binding: FragmentImageCropBinding

    private val viewModel: ImageCropViewModel by lazy {
        ViewModelProvider(this)[ImageCropViewModel::class.java]
    }

    var onCancelClicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        binding = FragmentImageCropBinding.inflate(inflater, container, false)
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
        binding.btnLeft.setOnClickListener {

//            binding.cropView.turnLeft()

            //TODO:注意合成的时候的影响
            val currentRotation = binding.cropView.rotation
            val targetRotation = currentRotation - 90f
            val animator = ObjectAnimator.ofFloat(binding.cropView, "rotation", currentRotation, targetRotation)
            animator.duration = 100 // 动画持续时间，单位为毫秒
            animator.addListener(object :Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {
                    binding.btnLeft.isClickable = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.btnLeft.isClickable = true
                }

                override fun onAnimationCancel(animation: Animator) {
                    binding.btnLeft.isClickable = true
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })
            animator.start()
//            binding.cropView.rotation -=90f
        }


        binding.btnRight.setOnClickListener {
            //TODO：注意合成的时候的影响
            val currentRotation = binding.cropView.rotation
            val targetRotation = currentRotation + 90f
            val animator = ObjectAnimator.ofFloat(binding.cropView, "rotation", currentRotation, targetRotation)
            animator.duration = 100 // 动画持续时间，单位为毫秒
            animator.addListener(object :Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {
                    binding.btnRight.isClickable = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.btnRight.isClickable = true
                }

                override fun onAnimationCancel(animation: Animator) {
                    binding.btnRight.isClickable = true
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })
            animator.start()
           // binding.cropView.rotation +=90f
        }

        binding.imageViewApply.setOnClickListener {
            viewModel.getCropRequest()?.let { cropRequest->
                when (cropRequest) {
                    is CropRequest.Manual -> {
                        BitmapUtils.saveBitmap(binding.cropView.getCroppedData(), cropRequest.destinationUri.toFile())
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
                        BitmapUtils.saveBitmap(binding.cropView.getCroppedData(), destinationUri.toFile())
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
            .observe(viewLifecycleOwner, Observer(this@ImageCropFragment::renderViewState))

        viewModel
            .getResizedBitmapLiveData()
            .observe(viewLifecycleOwner, Observer { binding.cropView.setBitmap(it.bitmap) })

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