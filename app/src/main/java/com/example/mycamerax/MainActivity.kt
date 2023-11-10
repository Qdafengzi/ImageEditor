package com.example.mycamerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExposureState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.mycamerax.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider.OnChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val mBinding by lazy {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
    }

    private val mCameraXControl by lazy {
        LifecycleCameraController(this)
    }

    private var maxZoom = 10f
    private var minZoom = 0.6f

    private var camera: Camera? = null

    private var exposureState: ExposureState? = null
    private var supportExposureState: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mBinding.viewFinder.controller = mCameraXControl
        mCameraXControl.isPinchToZoomEnabled = true
        mCameraXControl.isTapToFocusEnabled = true
        mCameraXControl.bindToLifecycle(this)

        lifecycleScope.launch {
            bindCameraUseCases(this@MainActivity, mBinding.viewFinder, this@MainActivity)
        }
        mBinding.btnPhoto.setOnClickListener {
            takePhoto(context = this@MainActivity, mCameraXControl) {
                mBinding.img.setImageBitmap(it)
            }
        }




        mBinding.sdScale.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            camera?.cameraControl?.setLinearZoom(
                value / 100f
            )
        })

        mBinding.sdBright.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            if (supportExposureState) {
                val range = exposureState?.exposureCompensationRange!!
                val size = abs(range.lower) + abs(range.upper)
                val offset = 200 / size
                val brightness = range.lower + value / offset
                XLogger.d("-------------->${brightness}")
                camera?.cameraControl?.setExposureCompensationIndex(brightness.toInt())
            }
        })


    }

    internal fun Float.clamped(scaleFactor: Float) = this * if (scaleFactor > 1f) {
        1.0f + (scaleFactor - 1.0f) * 2
    } else {
        1.0f - (1.0f - scaleFactor) * 2
    }


    fun takePhoto(
        context: Context, controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit
    ) {
        //权限
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(), 0, 0, image.width, image.height, matrix, true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    XLogger.d("Couldn't take photo: ${exception.message}")
                }
            })
    }


    private suspend fun bindCameraUseCases(
        context: Context,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        } ?: return


        val width = previewView.width
        val screenAspectRatio = aspectRatio(width, width)
        val rotation = previewView.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val previewCase = Preview.Builder()
            .setTargetRotation(rotation)
            .setTargetResolution(Size(width, width))
            .build()
            .also {
                // 附加取景器的表面提供程序以预览用例
                it.setSurfaceProvider(previewView.surfaceProvider)
            }


        // ImageCapture
        val imageCaptureCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            // 我们请求宽高比，但没有分辨率来匹配预览配置，但让
            // CameraX 针对最适合我们用例的特定分辨率进行优化
            // .setTargetAspectRatio(screenAspectRatio)
            // 设置初始目标旋转，如果旋转发生变化，我们将不得不再次调用它
            // 在此用例的生命周期内
            .setTargetRotation(rotation)
            .setTargetResolution(Size(width, width))
            .build()

        // ImageAnalysis
        val imageAnalyzerCase = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
//                .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setTargetResolution(Size(width, width))
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here - you should do something useful
                    // instead!

                    XLogger.d("Average luminosity: $luma")
                })
            }

        val viewPort: ViewPort = previewView.viewPort!!
        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(previewCase)
            .addUseCase(imageCaptureCase)
            .addUseCase(imageAnalyzerCase)
            .build()

        // 重新绑定用例之前必须取消绑定它们
        cameraProvider.unbindAll()
        if (camera != null) {
            // 必须从前一个摄像机实例中删除观察者
            camera!!.cameraInfo.cameraState.removeObservers(lifecycleOwner)
        }

        try {
            // 可以在此处传递可变数量的用例 -
            // 相机提供对 CameraControl 和 CameraInfo 的访问
//            camera = cameraProvider.bindToLifecycle(
//                this, cameraSelector, preview, imageCapture, imageAnalyzer
//            )
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, useCaseGroup
            )

            val zoomState = camera!!.cameraInfo.zoomState
            minZoom = zoomState.value!!.minZoomRatio
            maxZoom = zoomState.value!!.maxZoomRatio
            exposureState = camera!!.cameraInfo.exposureState
            supportExposureState = exposureState?.isExposureCompensationSupported ?: true


            // 设置缩放比例
//        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//            val zoomState = camera!!.cameraInfo.zoomState
//            val maxZoomRatio = zoomState.value!!.maxZoomRatio
//            val minZoomRatio = zoomState.value!!.minZoomRatio
//
//            override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
//                //1 -- 10
//                val zoomRatio = zoomState.value!!.zoomRatio
//                Log.d("CameraX,"," maxZoomRatio:"+maxZoomRatio +" minZoomRatio:"+minZoomRatio +" zoomRatio:"+zoomRatio);
//                if (zoomRatio < maxZoomRatio) {
//                    camera!!.cameraControl.setZoomRatio((zoomRatio + 0.1).toFloat())
//                }
//
//                if (zoomRatio > minZoomRatio) {
//                    camera!!.cameraControl.setZoomRatio((zoomRatio - 0.1).toFloat())
//                }
////                    val currentZoomRatio = camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F
////                    val delta = scaleGestureDetector.scaleFactor
////                    camera!!.cameraControl.setZoomRatio(currentZoomRatio * delta)
//                return true
//            }
//        }

//        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)
//        previewView.setOnTouchListener { _, motionEvent ->
//            scaleGestureDetector.onTouchEvent(motionEvent)
//            true
//        }
            observeCameraState(camera?.cameraInfo!!, lifecycleOwner = lifecycleOwner)
        } catch (exc: Exception) {
            XLogger.d("Use case binding failed:${exc.message}")
        }
    }

    private fun observeCameraState(cameraInfo: CameraInfo, lifecycleOwner: LifecycleOwner) {
        cameraInfo.cameraState.observe(lifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        XLogger.d("CameraX:=====>PENDING_OPEN")
                    }

                    CameraState.Type.OPENING -> {
                        XLogger.d("CameraX:=====>OPENING")
                    }

                    CameraState.Type.OPEN -> {
                        XLogger.d("CameraX:=====>OPEN")
                    }

                    CameraState.Type.CLOSING -> {
                        XLogger.d("CameraX:=====>CLOSING")
                    }

                    CameraState.Type.CLOSED -> {
                        XLogger.d("CameraX:=====>CLOSED")
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        XLogger.d("CameraX error:=====>ERROR_STREAM_CONFIG")
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        XLogger.d("CameraX error:=====>ERROR_CAMERA_IN_USE")
                    }

                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        XLogger.d("CameraX error:=====>ERROR_MAX_CAMERAS_IN_USE")
                    }

                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        XLogger.d("CameraX error:=====>ERROR_OTHER_RECOVERABLE_ERROR")
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        XLogger.d("CameraX error:=====>ERROR_CAMERA_DISABLED")
                    }

                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        XLogger.d("CameraX error:=====>ERROR_CAMERA_FATAL_ERROR")
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        XLogger.d("CameraX error:=====>ERROR_DO_NOT_DISTURB_MODE_ENABLED")
                    }
                }
            }
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }


    /**
     * 我们的定制图像分析课程。
     *
     * <p>我们需要做的就是用我们想要的操作覆盖函数“analyze”。这里，
     * 我们通过查看 YUV 帧的 Y 平面来计算图像的平均亮度。
     */
    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         *
         * 用于从图像平面缓冲区提取字节数组的辅助扩展函数
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         *
         * 分析图像以产生结果。
         * 调用者负责确保该分析方法能够足够快地执行，以防止图像采集管道中的停顿。否则，将不会采集和分析新的可用图像。
         *
         * 分析图像以产生结果。 调用者负责确保该分析方法能够足够快地执行，以防止图像采集管道中的停顿。否则，将不会采集和分析新的可用图像。
         * 参数：
         * image - 正在分析的图像非常重要：分析器方法实现必须在使用完接收到的图像后调用 image.close() 。
         * 否则，根据背压设置，可能无法接收新图像或相机可能停止运行。
         */
        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first
            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer
            // Extract image data from callback object
            val data = buffer.toByteArray()
            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }
            // Compute average luminance for the image
            val luma = pixels.average()
            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }

}

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit