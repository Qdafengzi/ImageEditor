package com.example.mycamerax

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
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
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mycamerax.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider.OnChangeListener
import com.theeasiestway.yuv.YuvUtils
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
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


    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"
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

    private val mCameraXControl by lazy {
        LifecycleCameraController(this)
    }

    private var maxZoom = 10f
    private var minZoom = 0.6f

    private var mCamera: Camera? = null

    private var exposureState: ExposureState? = null
    private var supportExposureState: Boolean = true

    private var mImageCapture: ImageCapture? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mBinding.viewFinder.controller = mCameraXControl
        mCameraXControl.isPinchToZoomEnabled = true
        mCameraXControl.isTapToFocusEnabled = true
        mCameraXControl.bindToLifecycle(this)

        lifecycleScope.launch {
            bindCameraUseCases(this@MainActivity, this@MainActivity)
        }
        mBinding.btnPhoto.setOnClickListener {
            ///storage/emulated/0/Pictures/GPUImage/1699711140054.jpg
            val folderName = "GPUImage"
            val fileName = System.currentTimeMillis().toString() + ".jpg"
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

        mBinding.sdBright.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            if (supportExposureState) {
                val range = exposureState?.exposureCompensationRange!!
                val size = abs(range.lower) + abs(range.upper)
                val offset = 200 / size
                val brightness = range.lower + value / offset
                XLogger.d("-------------->${brightness}")
                mCamera?.cameraControl?.setExposureCompensationIndex(brightness.toInt())
            }
        })

        mBinding.filter.setOnClickListener {
            val group = GPUImageFilterGroup()
            group.addFilter(GPUImageSketchFilter())
            mBinding.gupImage.filter = group
        }

        mBinding.noFilter.setOnClickListener {
            val group = GPUImageFilter()
            mBinding.gupImage.filter = group
        }

        val scaleGestureDetector = ScaleGestureDetector(this, mScaleGestureListener)

        mBinding.gupImage.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (mCamera == null) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    // 单指触摸，执行对焦操作
                    handleTapToFocus(event)
                }

                else -> {}
            }
            return@setOnTouchListener true
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


    private val mScaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                mCamera?.also { camera ->
                    val zoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio
                    zoomRatio?.also { ratio ->
                        camera.cameraControl.setZoomRatio(ratio * detector.scaleFactor)
                    }
                }
                return true
            }
        }

    internal fun Float.clamped(scaleFactor: Float) = this * if (scaleFactor > 1f) {
        1.0f + (scaleFactor - 1.0f) * 2
    } else {
        1.0f - (1.0f - scaleFactor) * 2
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


    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalGetImage::class)
    private suspend fun bindCameraUseCases(
        context: Context,
//        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        } ?: return

//        val previewView = Preview.Builder().build()

        val width = mBinding.gupImage.width
        val screenAspectRatio = aspectRatio(width, width)
        val rotation = mBinding.gupImage.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val previewCase = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(Size(width, width))
            .build()


//            .also {
//                // 附加取景器的表面提供程序以预览用例
////                mBinding.viewFinder.
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }


        // ImageCapture
        mImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            // 我们请求宽高比，但没有分辨率来匹配预览配置，但让
            // CameraX 针对最适合我们用例的特定分辨率进行优化
            // .setTargetAspectRatio(screenAspectRatio)
            // 设置初始目标旋转，如果旋转发生变化，我们将不得不再次调用它
            // 在此用例的生命周期内
            .setTargetRotation(rotation)
            .setTargetResolution(Size(width, width))
//            .setCaptureOptionUnpacker { config, builder ->
//                config.config.
//            }

            .build()


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
//            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setTargetResolution(Size(width, width))
            .build()

        val yuvUtils = YuvUtils()

        val converter = YuvToRgbConverter(this)

        imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            val rotation = imageProxy.imageInfo.rotationDegrees
            //XLogger.d("=width===============>${imageProxy.width}")
//           imageProxy.planes[0].buffer[0]: alpha
//           imageProxy.planes[0].buffer[1]: red
//           imageProxy.planes[0].buffer[2]: green
//           imageProxy.planes[0].buffer[3]: blue

            val bitsPerPixel = ImageFormat.getBitsPerPixel(imageProxy.format)
//            XLogger.d("=rotation===============>${rotation}")
            imageProxy.image?.let { image ->
//                yuvUtils.rotate(image, ExifInterface.ORIENTATION_ROTATE_90)
                val bitmap = allocateBitmapIfNecessary(imageProxy.width, imageProxy.height)
                converter.yuvToRgb(image, bitmap)
                imageProxy.close()
                mBinding.gupImage.post {
                    mBinding.gupImage.setImage(bitmap)
//                    mBinding.gupImage.setImage(rotateMyBitmap(bitmap,90))
                }
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
            .addUseCase(mImageCapture!!)
            .addUseCase(imageAnalysis)
            .build()

        cameraProvider.unbindAll()
        if (mCamera != null) {
            // 必须从前一个摄像机实例中删除观察者
            mCamera!!.cameraInfo.cameraState.removeObservers(lifecycleOwner)
        }

//
        try {
//            // 可以在此处传递可变数量的用例 -
//            // 相机提供对 CameraControl 和 CameraInfo 的访问
////            camera = cameraProvider.bindToLifecycle(
////                this, cameraSelector, preview, imageCapture, imageAnalyzer
////            )
            mCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, useCaseGroup
            )
//
            val zoomState = mCamera!!.cameraInfo.zoomState
            minZoom = zoomState.value!!.minZoomRatio
            maxZoom = zoomState.value!!.maxZoomRatio
            exposureState = mCamera!!.cameraInfo.exposureState
            supportExposureState = exposureState?.isExposureCompensationSupported ?: true
            observeCameraState(mCamera?.cameraInfo!!, lifecycleOwner = lifecycleOwner)
        } catch (exc: Exception) {
            XLogger.d("Use case binding failed:${exc.message}")
        }
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

}

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit