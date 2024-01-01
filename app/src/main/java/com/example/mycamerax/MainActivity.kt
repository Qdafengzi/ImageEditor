package com.example.mycamerax

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.RenderScript
import android.util.Log
import android.util.Range
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalZeroShutterLag
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
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mycamerax.databinding.ActivityMainBinding
import com.example.mycamerax.edit.ImageEditor
import com.example.mycamerax.utils.ResUtils
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


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

    lateinit var mImageAnalysis: ImageAnalysis
    private var filter1: GPUImageBrightnessFilter? = null
    private var filter2: GPUImageSharpenFilter? = null


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

    private lateinit var mCameraProvider: ProcessCameraProvider

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initView()
        setContent {
//            ImageEditorWithRectangularText()
            ImageEditor()
//            ScaleWithFixedButtonsExample()
//            ViewStyle()
//            ScalableImage5()
//            ScalableImage3333()


        }
    }

    @Composable
    fun ViewStyle() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {
                val intent = Intent(this@MainActivity, ViewActivity::class.java)
                startActivity(intent)
            }) {
                Text(text = "View")
            }
        }

    }


    @Composable
    fun ScalableImage444() {
        var scale by remember { mutableStateOf(1f) }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Layout(
                content = {
                    Image(
                        painter = painterResource(R.mipmap.ic_editor),
                        contentDescription = null,
                        modifier = Modifier
                            .graphicsLayer {
                                transformOrigin = TransformOrigin.Center
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_editor_scale),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val newScale =
                                        scale * (1 + change.positionChange().y / size.height)
                                    scale = if (newScale <= 0.1) 0.1f else newScale
                                }
                            }
                    )
                },
                measurePolicy = { measurables, constraints ->
                    XLogger.d("------>")
                    // 在这里进行测量和放置子元素的逻辑
                    val imagePlaceable =
                        measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val arrowPlaceable =
                        measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

                    val width = (imagePlaceable.width * scale).roundToInt()
                    val height = (imagePlaceable.height * scale).roundToInt()


//                    val imageOffsetX = (width - imagePlaceable.width) / 2
//                    val imageOffsetY = (height - imagePlaceable.height) / 2
//                    val arrowX = width - arrowPlaceable.width
//                    val arrowY = height - arrowPlaceable.height
                    layout(width, height) {
                        imagePlaceable.place(0, 0)
                        arrowPlaceable.place(
                            width,
                            height
                        )
                    }
                }
            )
        }
    }


    val padding = ResUtils.dp2px(20f)


    val rootImageSize = ResUtils.dp2px(40f)


    @Composable
    fun ScalableImage222() {
        var scale by remember { mutableStateOf(1f) }
        var size by remember { mutableStateOf(40.dp) } // 初始图片大小

        val sizeState = remember {
            mutableStateOf(
                Size(
                    ResUtils.dp2px(40f).toFloat(),
                    ResUtils.dp2px(40f).toFloat()
                )
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .wrapContentSize(),
            ) {
                val (box, scaleIcon) = createRefs()
                Image(
                    painter = painterResource(R.mipmap.ic_editor),
                    contentDescription = null,
                    modifier = Modifier
//                        .size(40.dp)
//                        .size(size) // 根据状态改变图片大小
                        .graphicsLayer(
                            scaleX = sizeState.value.width / 100f, // 根据状态改变图片的宽度
                            scaleY = sizeState.value.height / 100f, // 根据状态改变图片的高度
                            transformOrigin = TransformOrigin(0.5f, 0.5f) // 设置变换的中心点为图片的中心
                        )

                        .constrainAs(box) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                )

                Image(
                    painter = painterResource(R.drawable.ic_editor_scale),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .constrainAs(scaleIcon) {
                            end.linkTo(box.end)
                            bottom.linkTo(box.bottom)
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val newScale =
                                    scale * (1 + change.positionChange().y / this.size.height)
                                scale = if (newScale <= 0.1) {
                                    0.1f
                                } else if (newScale >= 5f) {
                                    2f
                                } else {
                                    newScale
                                }
                                size *= scale // 更新图片大小状态
                                XLogger.d("scale: ${scale}")
                            }
                        }
                )
            }
        }
    }


    @Composable
    fun ScalableImage() {
        var scale by remember { mutableStateOf(1f) }
        val arrowPosition = remember { mutableStateOf(Offset(0f, 0f)) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_editor),
                    contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            transformOrigin = TransformOrigin.Center
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            val arrowX = layoutCoordinates.size.width * scale
                            val arrowY = layoutCoordinates.size.height * scale
                            arrowPosition.value = Offset(arrowX, arrowY)
                        }
                )

                Image(
                    painter = painterResource(R.drawable.ic_editor_scale),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .offset {
                            IntOffset(
                                arrowPosition.value.x.toInt(),
                                arrowPosition.value.y.toInt()
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val newScale =
                                    scale * (1 + change.positionChange().y / this.size.height)
                                scale = if (newScale <= 0.1) 0.1f else newScale
                            }
                        }
                )
            }
        }
    }


    private val Offset.angle: Float
        get() = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()


    fun initView() {
        setContentView(mBinding.root)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                mCameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(this@MainActivity, this@MainActivity)
                queryCameraInfo(this@MainActivity, mCameraProvider)
            },
            mainThreadExecutor
        )

        mGPUImageFilterGroup.addFilter(mGPUImageMovieWriter)

//        mGPUImageFilterGroup.addFilter(mBinding.gupImage.filter)

        mBinding.viewFinder.controller = mCameraXControl
        mCameraXControl.isPinchToZoomEnabled = true
        mCameraXControl.isTapToFocusEnabled = true
        mCameraXControl.bindToLifecycle(this)

        clickListener()
    }


    private fun queryCameraInfo(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider
    ) {
        val cameraLensInfo = HashMap<Int, CameraInfo>()
        arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT).forEach { lens ->
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lens).build()
            if (cameraProvider.hasCamera(cameraSelector)) {
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
                if (lens == CameraSelector.LENS_FACING_BACK) {
                    cameraLensInfo[CameraSelector.LENS_FACING_BACK] = camera.cameraInfo
                } else if (lens == CameraSelector.LENS_FACING_FRONT) {
                    cameraLensInfo[CameraSelector.LENS_FACING_FRONT] = camera.cameraInfo
                }
            }
        }
        myCameraInfo.onInitialised(cameraLensInfo)
    }


    private val myCameraInfo: MCameraInfo = MCameraInfo()

    private class MCameraInfo : CameraInitListener {
        @OptIn(ExperimentalZeroShutterLag::class)
        @SuppressLint("RestrictedApi")
        override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {
            cameraLensInfo.forEach { t, u ->
                XLogger.d("t:${t} u:${u.zoomState}")
                XLogger.d("t:${t} u:${u.exposureState}")
                XLogger.d("t:${t} u:${u.cameraSelector}")
                XLogger.d("t:${t} u:${u.implementationType}")
                XLogger.d("t:${t} u:${u.intrinsicZoomRatio}")
                XLogger.d("t:${t} u:${u.cameraState}")
                XLogger.d("t:${t} u:${u.isPrivateReprocessingSupported}")
                XLogger.d("t:${t} u:${u.isZslSupported}")
                XLogger.d("t:${t} u:${u.sensorRotationDegrees}")
                XLogger.d("t:${t} u:${u.lensFacing}")
                XLogger.d("t:${t} u:${u.supportedFrameRateRanges}")
                XLogger.d("t:${t} u:${u.torchState}")
                XLogger.d("t:${t} u:${u.hasFlashUnit()}")

                XLogger.d("ssss")
//                XLogger.d("t:${t} u:${u.isFocusMeteringSupported(FocusMeteringAction.)}")
            }
        }
    }

    private fun getCameraPreview() = PreviewView(this).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        keepScreenOn = true
    }


    interface CameraInitListener {
        fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>)
    }


    fun getValue(max: Long, min: Long, sliderMax: Float, sliderMin: Float, progress: Float): Long {
        val slope = (max - min) / (sliderMax - sliderMin)
        val intercept = min - slope * sliderMin
        return (slope * progress + intercept).toLong()
    }

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    fun changeCompensation(progress: Float) {
        //如果不支持曝光补偿
        if (mCamera?.cameraInfo?.exposureState?.isExposureCompensationSupported != true) {
            return
        }

        val cameraInfo = mCamera?.cameraInfo ?: return
        val cameraControl = mCamera?.cameraControl ?: return

        val exposureRange = cameraInfo.exposureState.exposureCompensationRange
        val brightness =
            getValue(exposureRange.upper.toLong(), exposureRange.lower.toLong(), 100f, 1f, progress)
        val camera2CameraControl = Camera2CameraControl.from(cameraControl)
        XLogger.d("other device set camera exposure value progress:${progress} lower:${exposureRange.lower} upper:${exposureRange.upper}  brightness:$brightness")
        // 其他的设备开启自动曝光补偿 并且设置 曝光补偿指数
        camera2CameraControl.addCaptureRequestOptions(
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO,
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
                ).build()
        )
        XLogger.d("setBrightness----->other:$brightness")
        cameraControl.setExposureCompensationIndex(brightness.toInt())
    }


    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    fun exposureTime(progress: Float) {
        if (mCamera?.cameraInfo?.exposureState?.isExposureCompensationSupported != true) {
            return
        }

        val cameraInfo = mCamera?.cameraInfo ?: return
        val cameraControl = mCamera?.cameraControl ?: return

        val characteristics: CameraCharacteristics =
            Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)

        val manualRange: Range<Long>? = characteristics.get<Range<Long>>(
            CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
        )
        if (manualRange != null) {
            //setIso(1, characteristics)
            //曝光值的大小
            val min = manualRange.lower
            var max = manualRange.upper

            //0.01秒
            if (max > 10000000) {
                max = 10000000
            }

            var exposureTime = getValue(max, min, 100f, 1f, progress)
            if (exposureTime > max) {
                exposureTime = max
            }


            val camera2CameraControl = Camera2CameraControl.from(cameraControl)
            XLogger.d("box device set camera support EXPOSURE_TIME_RANGE min:$min max:${max} upper:${manualRange.upper} ae:$exposureTime")

            //关闭曝光 并且设置曝光值为
            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO,
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF
                )
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)
                .build()
            camera2CameraControl.addCaptureRequestOptions(captureRequestOptions)
        } else {
            XLogger.d("不支持 SENSOR_EXPOSURE_TIME")
        }
    }

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    fun setISO(progress: Float) {
        val cameraInfo = mCamera?.cameraInfo ?: return
        val characteristics: CameraCharacteristics =
            Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)
        mCamera?.let { camera ->
            val range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                ?: return

            val min = range.lower //100
            val max = range.upper //10000
            val iso = getValue(
                max = max.toLong(),
                min = min.toLong(),
                sliderMax = 100f,
                sliderMin = 0f,
                progress.toFloat()
            )

            XLogger.d("box device set camera support ISO min:$min max:${max}  iso:$iso")
            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)

            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO,
                )
                .setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY, iso.toInt()
                ).build()
            camera2CameraControl.addCaptureRequestOptions(captureRequestOptions)
        }
    }

    fun clickListener() {
        mBinding.compensationSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                changeCompensation(progress = progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        mBinding.exposureTimeSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                exposureTime(progress = progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        mBinding.isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setISO(progress = progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })


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

                XLogger.d("缩放：${camera.cameraInfo.zoomState.value?.zoomRatio}")
            }
        })

        mBinding.slider.addOnChangeListener { slider, value, fromUser ->
            filterAdjuster?.adjust(mBinding.slider.value.toInt())
            //TODO：如果是 自动对焦
            //setManualFocus(value.toInt())
        }
        mBinding.slider1.addOnChangeListener { slider, value, fromUser ->
            filter1?.setBrightness(range(value.toInt(), -1.0f, 1.0f))
            //filterAdjuster
        }



        mBinding.slider2.addOnChangeListener { slider, value, fromUser ->
            //setManualFocus(value.toInt())
            filter2?.setSharpness(range(value.toInt(), -1.0f, 1.0f))
        }

        mBinding.btnBrightness.setOnClickListener {
            val group = GPUImageFilterGroup()
            val filter = GPUImageBrightnessFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }
        mBinding.toneBrightness.setOnClickListener {
            val group = GPUImageFilterGroup()
            filter1 = GPUImageBrightnessFilter()
            filter2 = GPUImageSharpenFilter()
            group.addFilter(filter1)
            group.addFilter(filter2)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(group)
        }


        mBinding.btnTone.setOnClickListener {
            //TODO:白平衡的处理
            val group = GPUImageFilterGroup()
            val filter = GPUImageWhiteBalanceFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }

        mBinding.btnSharpen.setOnClickListener {
            val group = GPUImageFilterGroup()
            val filter = GPUImageSharpenFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }

        mBinding.btnExposure.setOnClickListener {
            val group = GPUImageFilterGroup()
            val filter = GPUImageExposureFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }

        mBinding.btnContrast.setOnClickListener {
            val group = GPUImageFilterGroup()
            val filter = GPUImageContrastFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }

        mBinding.btnGrayscale.setOnClickListener {
            val group = GPUImageFilterGroup()
            val filter = GPUImageGrayscaleFilter()
            group.addFilter(filter)
            group.addFilter(mGPUImageMovieWriter)
            mGPUImageFilterGroup = group

            switchFilterTo(filter)
        }

        mBinding.noFilter.setOnClickListener {
            val groupFilter = GPUImageFilterGroup()
            val filter = GPUImageFilter()
            groupFilter.addFilter(filter)
            mGPUImageFilterGroup = groupFilter

            switchFilterTo(filter)
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
                //暂停保存文件
                mIsRecording = false
                time = 0L
                currentRecording?.stop()
                mBinding.btnVideo.text = "VIDEO"

            } else {
                //开始录制
//                mBinding.btnVideo.text = "start"
                mSwitchVideo = true
                mIsRecording = true
                startRecording()
//                startRecordByGpuImageWrite()
                startRecordTimer()
            }
        }

        mBinding.jump.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }

//    fun saveVideoToMediaStore(context: Context, videoFile: File, displayName: String, description: String) {
//        val resolver = context.contentResolver
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
//            put(MediaStore.Video.Media.DESCRIPTION, description)
//            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
//            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
//            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
//        }
//
//        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
//
//        uri?.let { mediaUri ->
//            resolver.openOutputStream(mediaUri)?.use { outputStream ->
//                videoFile.inputStream().use { inputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            // 发送广播通知媒体库刷新
//            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mediaUri))
//        }
//    }

    fun refreshGalleryFile() {


    }

    fun range(percentage: Int, start: Float, end: Float): Float {
        return (end - start) * percentage / 100.0f + start
    }

    fun range(percentage: Int, start: Int, end: Int): Int {
        return (end - start) * percentage / 100 + start
    }

    private fun startRecordByGpuImageWrite() {
        XLogger.d("录制 startRecordByGpuImageWrite")
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"

        val downloadFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val filePath = File(downloadFolder.absolutePath, name)

        mGPUImageMovieWriter.prepareRecording(
            filePath.absolutePath,
            mBinding.gupImage.measuredWidth,
            mBinding.gupImage.measuredHeight
        )


        mGPUImageMovieWriter.startRecording(object : GPUImageMovieWriter.StartRecordListener {
            override fun onRecordStart() {
                XLogger.d("开始录制")
            }

            override fun onRecordError(e: java.lang.Exception?) {
                XLogger.d("录制出错${e?.message}")
            }
        })

        MediaScannerConnection.scanFile(this, arrayOf(filePath.absolutePath), null) { path, uri ->
            // 扫描完成后的操作，如果需要的话
            XLogger.d("录制 刷新成功：${filePath.absolutePath}")
        }
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


    @OptIn(ExperimentalGetImage::class)
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

        XLogger.d("文件地址：${mediaStoreOutput.collectionUri.toString()}")

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
    private fun bindCameraUseCases(context: Context, lifecycleOwner: LifecycleOwner) {
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

        if (!this::mCameraProvider.isInitialized) return

//        val build = Preview.Builder()
//        Camera2Interop.Extender(build).setCaptureRequestOption("")
//        val previewView = Preview.Builder().build()

        val width = mBinding.gupImage.width
        val screenAspectRatio = aspectRatio(width, width)
        val rotation = mBinding.gupImage.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val previewBuilder = Preview.Builder()
        previewBuilder.setTargetResolution(android.util.Size(width, width))
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
                //it.setSurfaceProvider(mBinding.viewFinder.surfaceProvider)
            }

        val selector = QualitySelector
            .from(
                Quality.UHD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )

        val recorder = Recorder.Builder()
//            .setAspectRatio()
            .setQualitySelector(selector)

            .setExecutor(mainThreadExecutor)
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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // 我们请求宽高比，但没有分辨率来匹配预览配置，但让
                // CameraX 针对最适合我们用例的特定分辨率进行优化
                // .setTargetAspectRatio(screenAspectRatio)
                // 设置初始目标旋转，如果旋转发生变化，我们将不得不再次调用它
                // 在此用例的生命周期内
//                .setTargetRotation(rotation)
                .setTargetResolution(android.util.Size(width, width))
//            .setCaptureOptionUnpacker { config, builder ->
//                config.config.
//            }
                .build()

        }

        // ImageCapture


        mImageAnalysis = ImageAnalysis.Builder()
            .setOutputImageRotationEnabled(true)

//            .setTargetResolution(Size(1400, 1400))
//            .setMaxResolution(Size(2160, 2160))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

//            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(android.util.Size(width, width))
            .build()

        val yuvUtils = YuvUtils()
        val converter = YuvToRgbConverter(this)

        val rs = RenderScript.create(context)

        mImageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            if (imageProxy.image == null) return@Analyzer
            imageProxy.image?.let { image ->
                if (image.width <= 1 || image.height <= 1) return@Analyzer

                val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
                converter.yuvToRgb(image, bitmap)
                // 顺时针旋转 Bitmap
                imageProxy.close()
                mBinding.gupImage.post {
                    mBinding.gupImage.setImage(bitmap)
                }
            }
        })

        val filter = CameraFilter { // 在这里实现你的滤镜效果
            mutableListOf()
        }

        // 重新绑定用例之前必须取消绑定它们
        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(mBinding.viewFinder.viewPort!!)
            .addUseCase(previewCase)
            .addUseCase(mImageCapture!!)
//            .addUseCase(mVideoCapture)
            .build()
        mCameraProvider.unbindAll()

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
            mCamera = mCameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
//                useCaseGroup,

//                previewCase,
                mImageAnalysis, mVideoCapture
            )

            val camera2Control = Camera2CameraControl.from(mCamera!!.cameraControl)

//            val captureRequestOption = CaptureRequestOptions
//                .Builder()
//                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_OFF)
//                .build()
//            camera2Control.captureRequestOptions = captureRequestOption

            camera2Control.captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                .build()


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

                    XLogger.d("缩放：${minZoom}  ${maxZoom}")
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
//            if (mBinding.root.display.displayId == displayId) {
//                val rotation = mBinding.root.display.rotation
////                imageAnalysis.targetRotation = rotation
////                imageCapture.targetRotation = rotation
//                XLogger.d("displayListener屏幕的方向：$rotation")
//            }
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