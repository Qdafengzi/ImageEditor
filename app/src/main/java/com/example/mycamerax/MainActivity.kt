package com.example.mycamerax

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mycamerax.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider.OnChangeListener
import com.theeasiestway.yuv.YuvUtils
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {

    fun interface RecordListener {
        fun onStop()
    }


    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"

        const val DEFAULT_QUALITY_IDX = 0
        val TAG: String = MainActivity::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val cameraParamsExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val mBinding by lazy {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
    }
    private val videoGpuGroup by lazy {

    }

    private val mCameraXControl by lazy {
        LifecycleCameraController(this)
    }

    private var mGPUImageFilterGroup = GPUImageFilterGroup()

    private val mGPUImageMovieWriter by lazy {
        GPUImageMovieWriter()
    }


    private var mIsRecording = false

    //正在录制视频
    private var mSwitchVideo = false

    private var maxZoom = 10f
    private var minZoom = 0.6f

    private var mCamera: Camera? = null

    private var exposureState: ExposureState? = null
    private var supportExposureState: Boolean = true

    private var mImageCapture: ImageCapture? = null
    private var mVideoCapture: VideoCapture<Recorder>? = null

    private var filterAdjuster: GPUImageFilterTools.FilterAdjuster? = null

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(this) }


    private val mScaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                mCamera?.also { camera ->
                    val zoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio
                    zoomRatio?.also { ratio ->
                        XLogger.d("zoomRatio--------->${ratio}  ${ratio * detector.scaleFactor}  ${detector.scaleFactor}")
                        camera.cameraControl.setZoomRatio(ratio * detector.scaleFactor)
                        //控制滑动条的数据
                        val progress = ratio * 10
                        //TODO:滑动条的数据 会相互干扰
//                    mBinding.sdScale.post {
//                        mBinding.sdScale.value = progress
//                    }

                    }
                }
                return true
            }
        }

    private val mScaleGestureDetector by lazy {
        ScaleGestureDetector(this, mScaleGestureListener)
    }


    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mGPUImageFilterGroup.addFilter(mGPUImageMovieWriter)

//        mGPUImageFilterGroup.addFilter(mBinding.gupImage.filter)

        mBinding.viewFinder.controller = mCameraXControl
        mCameraXControl.isPinchToZoomEnabled = true
        mCameraXControl.isTapToFocusEnabled = true

        mCameraXControl.bindToLifecycle(this)

        lifecycleScope.launch {
            bindCameraUseCases(this@MainActivity, this@MainActivity)
        }
        clickListener()
    }


    fun clickListener() {
        mBinding.btnPhoto.setOnClickListener {
            mSwitchVideo = true
            ///storage/emulated/0/Pictures/GPUImage/1699711140054.jpg
            val folderName = "GPUImage"
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            mBinding.gupImage.capture()
            mBinding.gupImage.saveToPictures(folderName, fileName) {
                XLogger.d("folderName================>$folderName")
                XLogger.d("fileName================>$fileName")
                runOnUiThread {
                    //uri 转 path
                    Glide.with(this@MainActivity)
                        .load(it)
//                                    .apply(RequestOptions.circleCropTransform())
                        .into(mBinding.img)
                }
            }

//            takePhoto(context = this@MainActivity, mCameraXControl) {
//                mBinding.img.setImageBitmap(it)
//            }

//            val name = SimpleDateFormat(FILENAME, Locale.US)
//                .format(System.currentTimeMillis())
//            val contentValues = ContentValues().apply {
//                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//                put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                    val appName = resources.getString(com.example.mycamerax.R.string.app_name)
//                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
//                }
//            }
//
//            // Create output options object which contains file + metadata
//            val outputOptions = ImageCapture.OutputFileOptions
//                .Builder(
//                    contentResolver,
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    contentValues
//                )
//                .build()
//            mImageCapture?.takePicture(
//
//                outputOptions,
//                cameraExecutor,
//                object : ImageCapture.OnImageCapturedCallback(),
//                    ImageCapture.OnImageSavedCallback {
//                    override fun onError(exc: ImageCaptureException) {
//                        XLogger.d("Photo capture failed: ${exc.message}")
//                    }
//
//                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                        val savedUri = output.savedUri
//                        XLogger.d("Photo capture succeeded: $savedUri")
//
//                        // We can only change the foreground Drawable using API level 23+ API
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            // Update the gallery thumbnail with latest picture taken
//
//                            //setGalleryThumbnail(savedUri.toString())
//                            //子线程 要切换线程
//                            runOnUiThread {
//                                Glide.with(this@MainActivity)
//                                    .load(savedUri.toString())
////                                    .apply(RequestOptions.circleCropTransform())
//                                    .into(mBinding.img)
//                            }
//                        }
//
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                            // Suppress deprecated Camera usage needed for API level 23 and below
//                            @Suppress("DEPRECATION")
//                            sendBroadcast(
//                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
//                            )
//                        }
//                    }
//                })
        }
        mBinding.sdScale.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            mCamera?.let { camera ->
                val linearZoomFuture = camera.cameraControl.setLinearZoom(
                    value / 100f
                )

                linearZoomFuture.addListener({
                    XLogger.d("------>开始")
                }, cameraParamsExecutor)
            }
        })

        mBinding.slider.addOnChangeListener { slider, value, fromUser ->
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
            //TODO：如果是 自动对焦
            setManualFocus(value.toInt())
        }

        mBinding.btnBrightness.setOnClickListener {

            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageBrightnessFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.btnTone.setOnClickListener {
            //TODO:白平衡的处理
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageWhiteBalanceFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.btnSharpen.setOnClickListener {
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageSharpenFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.btnExposure.setOnClickListener {
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageExposureFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.btnContrast.setOnClickListener {
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageContrastFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.btnGrayscale.setOnClickListener {
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageGrayscaleFilter())
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group

            switchFilterTo(group)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }

        mBinding.noFilter.setOnClickListener {
            val group = GPUImageFilterGroup()
            mGPUImageFilterGroup = group
            mBinding.gupImage.filter = group
        }

//        mBinding.slider.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
//            if (supportExposureState) {
//                val range = exposureState?.exposureCompensationRange!!
//                val size = abs(range.lower) + abs(range.upper)
//                val offset = 200 / size
//                val brightness = range.lower + value / offset
//                XLogger.d("-------------->${brightness}")
//                mCamera?.cameraControl?.setExposureCompensationIndex(brightness.toInt())
//            }
//        })


        mBinding.gupImage.setOnTouchListener { v, event ->
            mScaleGestureDetector.onTouchEvent(event)
            if (mCamera == null) return@setOnTouchListener false

            //一个手指的时候 触发 聚焦功能
            if (event.pointerCount == 1) {
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        // 单指触摸，执行对焦操作
                        handleTapToFocus(event)
                    }

                    else -> {}
                }
            }

            return@setOnTouchListener true
        }

        mBinding.btnVideo.setOnClickListener {
            if (mIsRecording) {
                mBinding.btnVideo.text = "stop"
                //暂停保存文件
                mIsRecording = false
                currentRecording?.stop()
            } else {
                //开始录制
                mBinding.btnVideo.text = "start"
                mSwitchVideo = true
                mIsRecording = true
//                 startRecording()
                startRecordByGpuImageWrite()
                startRecordTimer()
            }
        }
    }

    private fun startRecordByGpuImageWrite() {

        mGPUImageMovieWriter.startRecording(object : GPUImageMovieWriter.StartRecordListener {
            override fun onRecordStart() {
                XLogger.d("开始录制")
            }

            override fun onRecordError(e: java.lang.Exception?) {
                XLogger.d("录制出错")
            }

        })
    }


    private fun switchFilterTo(filter: GPUImageFilter) {
        if (mBinding.gupImage.filter == null || mBinding.gupImage.filter!!.javaClass != filter.javaClass) {
            mBinding.gupImage.filter = filter
            filterAdjuster = GPUImageFilterTools.FilterAdjuster(filter)
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
        }
    }

    var time = 0L
    fun startRecordTimer() {
        lifecycleScope.launch {
            while (mIsRecording) {
                delay(1000)
                time += 1
                mBinding.btnVideo.post {
                    mBinding.btnVideo.text = "${time}s"
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording =
            mVideoCapture?.output?.prepareRecording(this, mediaStoreOutput)
                .apply {
                    //是否支持录音
//                    if (audioEnabled)
//                        withAudioEnabled()
                }
                ?.start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")
    }


    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status) {
            recordingState = event
        }

        //updateUI(event)


        if (event is VideoRecordEvent.Finalize) {
            // display the captured video

            XLogger.d("录制视频结束：")
        }
    }


    @ExperimentalCamera2Interop
    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("RestrictedApi")
    private suspend fun bindCameraUseCases(context: Context, lifecycleOwner: LifecycleOwner) {
        // 获取设备方向
        val display: Display = windowManager.defaultDisplay
        val displayRotation = display.rotation

        // 根据设备方向旋转预览图片
        val rotationDegrees = when (displayRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        XLogger.d("=============>${rotationDegrees}")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        } ?: return

//        val build = Preview.Builder()
//        Camera2Interop.Extender(build).setCaptureRequestOption("")


//        val previewView = Preview.Builder().build()

        val width = mBinding.gupImage.width
        val screenAspectRatio = aspectRatio(width, width)
        val rotation = mBinding.gupImage.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val previewBuilder = Preview.Builder()
        previewBuilder.setTargetResolution(Size(width, width))
//        previewBuilder.setTargetRotation(Surface.ROTATION_0)
//        previewBuilder.setTargetAspectRatio(screenAspectRatio)
        Camera2Interop.Extender(previewBuilder)
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            )

//            .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, CaptureRequest.COLOR_CORRECTION_GAINS)

//            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, ...);
        val previewCase = previewBuilder.build()
            .also {
                // 附加取景器的表面提供程序以预览用例
//                mBinding.viewFinder.
                it.setSurfaceProvider(mBinding.viewFinder.surfaceProvider)
            }

        val selector = QualitySelector
            .from(
                Quality.UHD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )
        val recorder = Recorder.Builder()
            .setQualitySelector(selector)
            .build()

//        val cameraSelector = CameraSelector.Builder()
//            .addCameraFilter {
//                it.filter { camInfo ->
//                    val level = Camera2CameraInfo.from(camInfo)
//                        .getCameraCharacteristic(
//                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
//                        )
//                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
//                }
//            }.build()


        mVideoCapture = VideoCapture.withOutput(recorder)





        if (mSwitchVideo) {
            //视频
            val recorder = Recorder.Builder()
//                .setQualitySelector(qualitySelector)
                .build()
            mVideoCapture = VideoCapture.withOutput(recorder)
        } else {
            //照片
            mImageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // 我们请求宽高比，但没有分辨率来匹配预览配置，但让
                // CameraX 针对最适合我们用例的特定分辨率进行优化
                // .setTargetAspectRatio(screenAspectRatio)
                // 设置初始目标旋转，如果旋转发生变化，我们将不得不再次调用它
                // 在此用例的生命周期内
//                .setTargetRotation(rotation)
                .setTargetResolution(Size(width, width))
//            .setCaptureOptionUnpacker { config, builder ->
//                config.config.
//            }
                .build()

        }

        // ImageCapture


        // ImageAnalysis


//        val imageAnalyzerCase = ImageAnalysis.Builder()
//            // We request aspect ratio but no resolution
//            // Set initial target rotation, we will have to call this again if rotation changes
//            // during the lifecycle of this use case
////                .setTargetAspectRatio(screenAspectRatio)
//            .setTargetRotation(rotation)
//            .setTargetResolution(Size(width, width))
//            .build()
//            // The analyzer can then be assigned to the instance
//            .also {
////                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
////                    // Values returned from our analyzer are passed to the attached listener
////                    // We log image analysis results here - you should do something useful
////                    // instead!
////
////                    XLogger.d("Average luminosity: $luma")
////                })
//                val myImageAnalyzer = MyImageAnalyzer(this)
//                it.setAnalyzer(cameraExecutor, myImageAnalyzer)
//            }
//
////        val viewPort: ViewPort = previewView.viewPort!!


        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageRotationEnabled(true)

//            .setTargetResolution(Size(1400, 1400))
//            .setMaxResolution(Size(2160, 2160))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(width, width))
            .build()

        val yuvUtils = YuvUtils()

        val converter = YuvToRgbConverter(this)

//        imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
//            // 进行 YUV 转 RGB 转换并旋转
//            val rgbBitmap = yuvToRgb(imageProxy)
//            // 应用滤镜
//            // 在 GPUImage 视图上渲染处理后的图像
//            mBinding.gupImage.post {
//                mBinding.gupImage.setImage(rgbBitmap)
//            }
//
//            imageProxy.close()
//        })
        imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            if (imageProxy.image == null) return@Analyzer

//            val data = ImageUtils.generateNV21Data(imageProxy.image)
//            val width: Int = imageProxy.width
//            val height: Int = imageProxy.height
//            imageProxy.close()
//            mBinding.gupImage.post {
//                mBinding.gupImage.updatePreviewFrame(data,width,height)
//            }
//            return@Analyzer


            imageProxy.image?.let { image ->
                val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
                converter.yuvToRgb(image, bitmap)
                // 顺时针旋转 Bitmap
//                val matrix = Matrix()
//                matrix.postRotate(90f)
//                val rotatedBitmap =
//                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                imageProxy.close()
                mBinding.gupImage.post {
                    mBinding.gupImage.setImage(bitmap)
                }


//                if (!mSwitchVideo){
//                    //图片
//                    val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
//                    converter.yuvToRgb(image, bitmap)
//
//
//                    // 顺时针旋转 Bitmap
//                    val matrix = Matrix()
//                    matrix.postRotate(90f)
//                    val rotatedBitmap =
//                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//
//                    imageProxy.close()
//                    mBinding.gupImage.post {
//                        mBinding.gupImage.setImage(rotatedBitmap)
//                    }
//                }else{
//                    val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
//                    converter.yuvToRgb(image, bitmap)
//
//                    // 顺时针旋转 Bitmap
//                    val matrix = Matrix()
//                    matrix.postRotate(90f)
//                    val rotatedBitmap =
//                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//
//                    // 应用 GPUImage 滤镜
//                    val gpuImage = GPUImage(context)
//                    gpuImage.setImage(rotatedBitmap)
//                    gpuImage.setFilter(GPUImageFilter())
//
//                    //处理后的滤镜
//                    val filteredBitmap =   gpuImage.bitmapWithFilterApplied
//                    // 将 Bitmap 转换为 YUV 数据，以便写入视频文件
//                    val yuvImage = rgbToYuv(filteredBitmap)
//
//                    // 将处理后的 YUV 数据写入视频文件
//
//
//                }


//                val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
//                    converter.yuvToRgb(image, bitmap)
//
//                    // 顺时针旋转 Bitmap
//                    val matrix = Matrix()
//                    matrix.postRotate(90f)
//                    val rotatedBitmap =
//                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//
//                    // 应用 GPUImage 滤镜
//                    val gpuImage = GPUImage(context)
//                    gpuImage.setImage(rotatedBitmap)
//                    gpuImage.setFilter(GPUImageFilter())
//
//                    //处理后的滤镜
//                    val filteredBitmap =   gpuImage.bitmapWithFilterApplied
//                    // 将 Bitmap 转换为 YUV 数据，以便写入视频文件
//                    val yuvImage = rgbToYuv(filteredBitmap)
//
//
//                    imageProxy.close()
//                    mBinding.gupImage.post {
//                        mBinding.gupImage.setImage(rotatedBitmap)
//                    }
            }
        })

//        imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer {imageProxy->
//            imageProxy.image?.also {image->
//                if (imageProxy.format==PixelFormat.RGBA_8888){
//                    val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
//                    converter.yuvToRgb(image, bitmap)
//                    image.close()
//                    mBinding.viewFinder.post {
//                        mBinding.viewFinder.setImage(bitmap)
//                    }
//                }else{
//                    XLogger.d("================>${imageProxy.format}")
//                }
//            }
//        })
        // 重新绑定用例之前必须取消绑定它们
        val useCaseGroup = UseCaseGroup.Builder()
//            .setViewPort(viewPort)
            .addUseCase(previewCase)
//            .addUseCase(mImageCapture!!)
            .addUseCase(imageAnalysis)
//            .addEffect()
            .build()
        cameraProvider.unbindAll()

        mCamera?.let { camera ->
            // 必须从前一个摄像机实例中删除观察者
            camera.cameraInfo.cameraState.removeObservers(lifecycleOwner)
        }

        try {
            // 可以在此处传递可变数量的用例 -
//            mCamera = cameraProvider.bindToLifecycle(
//                lifecycleOwner, cameraSelector, useCaseGroup
//            )

            //imageAnalysis, videoCapture
            mCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, imageAnalysis, mVideoCapture
            )

            val camera2Control = Camera2CameraControl.from(mCamera!!.cameraControl)

//            val captureRequestOption = CaptureRequestOptions
//                .Builder()
//                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_OFF)
//                .build()
//            camera2Control.captureRequestOptions = captureRequestOption

            val camera2CameraInfo = Camera2CameraInfo.from(mCamera!!.cameraInfo)
            val cameraCharacteristic =
                camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)


            //15  15
            //14  20   SENSOR_REFERENCE_ILLUMINANT1_COOL_WHITE_FLUORESCENT
            //20  20
            //25  25
            //14  30
            //30  30
            cameraCharacteristic?.forEach {
                XLogger.d("cameraCharacteristic------------${it.lower}  ${it.upper}")
            }

            mCamera?.let { camera ->
                val zoomState = camera.cameraInfo.zoomState
                zoomState.value?.let { zoomState ->
                    minZoom = zoomState.minZoomRatio
                    maxZoom = zoomState.maxZoomRatio
                }
                exposureState = camera.cameraInfo.exposureState
                supportExposureState = exposureState?.isExposureCompensationSupported ?: true
                observeCameraState(camera.cameraInfo, lifecycleOwner = lifecycleOwner)
            }

        } catch (exc: Exception) {
            XLogger.d("Use case binding failed:${exc.message}")
        }
    }

    @SuppressLint("RestrictedApi")
    fun setManualFocus(progress: Int) {
        //TODO:这个progress 传值是否对
        if (mCamera?.cameraControl == null || mCamera?.cameraInfo == null) return

        mCamera?.let { camera ->
            val camChars: CameraCharacteristics =
                Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)
            val discoveredMinFocusDistance: Float = camChars
                .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

            val num: Float = progress.toFloat() * discoveredMinFocusDistance / 100

            Camera2CameraControl.from(camera.cameraControl)
                .addCaptureRequestOptions(
                    CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF
                        ) // set AF to manual
                        .setCaptureRequestOption(
                            CaptureRequest.LENS_FOCUS_DISTANCE, num
                        ).build()
                )
        }
    }

    // 处理单击对焦操作
    private fun handleTapToFocus(event: MotionEvent) {
        val width = mBinding.viewFinder.width.toFloat()
        val height = mBinding.viewFinder.height.toFloat()

        XLogger.d("触摸的位置：$width $height")

        // 将单击的位置转换为相机坐标系中的焦点位置
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            width, height
        )
        val autoFocusPoint = factory.createPoint(event.x, event.y)

        // 应用焦点位置到相机的对焦功能
//
//        val scaleWidth = width /focusWidth
//        val action = FocusMeteringAction.Builder(
//            SurfaceOrientedMeteringPointFactory(
//                event.x, event.y
//            ).createPoint(event.x/scaleWidth, event.y/scaleWidth)
//        ).apply { disableAutoCancel() }.build()

        val action = FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
            .addPoint(autoFocusPoint)
//            .setAutoCancelDuration(5, TimeUnit.SECONDS)//5秒后自动取消
            .build()
        mCamera?.cameraControl?.startFocusAndMetering(action)


        canvasFocus(event)
    }

    /**
     * 绘制焦点
     */
    fun canvasFocus(event: MotionEvent) {
        val focusWidth = 200f // 焦点框的宽度
        val focusHeight = 200f // 焦点框的高度
        // 计算焦点框的位置
        val left = event.x - focusWidth / 2
        val top = event.y - focusHeight / 2
        val right = event.x + focusWidth / 2
        val bottom = event.y + focusHeight / 2

        // 更新 FocusView 的焦点框位置
        mBinding.focusView.setFocusRect(left, top, right, bottom)
        //聚焦框的消失
        mBinding.focusView.dimFocusRect()
    }


    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
//    @Throws(IOException::class)
//    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap? {
//        val ei = ExifInterface(selectedImage.path)
//        val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
//        return when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_90 -> rotateMyBitmap(img, 90)
//            ExifInterface.ORIENTATION_ROTATE_180 -> rotateMyBitmap(img, 180)
//            ExifInterface.ORIENTATION_ROTATE_270 -> rotateMyBitmap(img, 270)
//            else -> img
//        }
//    }


    /**
     * 旋转图片
     * @return 旋转后图片（只是修改了Bitmap对象，没有修改图片文件)
     */
    fun rotateMyBitmap(bmp: Bitmap, gree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(-90f)
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    private var bitmap: Bitmap? = null
    private fun allocateBitmapIfNecessary(width: Int, height: Int): Bitmap {
        if (bitmap == null || bitmap!!.width != width || bitmap!!.height != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return bitmap!!
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


//    fun takePhoto(
//        context: Context, controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit
//    ) {
//        //权限
//        controller.takePicture(
//            ContextCompat.getMainExecutor(context),
//            object : ImageCapture.OnImageCapturedCallback() {
//                override fun onCaptureSuccess(image: ImageProxy) {
//                    super.onCaptureSuccess(image)
//
//                    val matrix = Matrix().apply {
//                        postRotate(image.imageInfo.rotationDegrees.toFloat())
//                    }
//                    val rotatedBitmap = Bitmap.createBitmap(
//                        image.toBitmap(), 0, 0, image.width, image.height, matrix, true
//                    )
//
//                    onPhotoTaken(rotatedBitmap)
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    super.onError(exception)
//                    XLogger.d("Couldn't take photo: ${exception.message}")
//                }
//            })
//    }

    inner class MyImageAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
        lateinit var yuvUtils: YuvUtils

        private var bitmap: Bitmap? = null
        private var gpuImage: GPUImage? = null
        //创建GPUImage对象并设置滤镜类型，这里我使用的是素描滤镜

        init {
            initFilter()
        }

        private fun initFilter() {
            yuvUtils = YuvUtils()
            gpuImage = GPUImage(context)
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageSketchFilter())
            gpuImage!!.setFilter(group)
        }

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(image: ImageProxy) {
            //将Android的YUV数据转为libYuv的数据
            var yuvFrame = yuvUtils.convertToI420(image.image!!)
            //对图像进行旋转（由于回调的相机数据是横着的因此需要旋转90度）
            yuvFrame = yuvUtils.rotate(yuvFrame, 90)
            //根据图像大小创建Bitmap
            bitmap = Bitmap.createBitmap(yuvFrame.width, yuvFrame.height, Bitmap.Config.ARGB_8888)
            //将图像转为Argb格式的并填充到Bitmap上
            yuvUtils.yuv420ToArgb(yuvFrame)

            //利用GpuImage给图像添加滤镜
            bitmap = gpuImage!!.getBitmapWithFilterApplied(bitmap)
            //由于这不是UI线程因此需要在UI线程更新UI

            mBinding.image.post {
                mBinding.image.setImageBitmap(bitmap)
                //关闭ImageProxy，才会回调下一次的数据
                image.close()
            }
        }

    }


    /**
     * 我们的定制图像分析课程。
     *
     * <p>我们需要做的就是用我们想要的操作覆盖函数“analyze”。这里，
     * 我们通过查看 YUV 帧的 Y 平面来计算图像的平均亮度。
     */
    class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {


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

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (mBinding.root.display.displayId == displayId) {
                val rotation = mBinding.root.display.rotation
//                imageAnalysis.targetRotation = rotation
//                imageCapture.targetRotation = rotation
                XLogger.d("displayListener屏幕的方向：$rotation")
            }
        }

        override fun onDisplayAdded(displayId: Int) {
        }

        override fun onDisplayRemoved(displayId: Int) {
        }
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

//                XLogger.d("orientationEventListener屏幕的方向：$rotation")

//                imageAnalysis.targetRotation = rotation
//                imageCapture.targetRotation = rotation
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.unregisterDisplayListener(displayListener)
        orientationEventListener.disable()
    }

    internal fun Float.clamped(scaleFactor: Float) = this * if (scaleFactor > 1f) {
        1.0f + (scaleFactor - 1.0f) * 2
    } else {
        1.0f - (1.0f - scaleFactor) * 2
    }

}

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit