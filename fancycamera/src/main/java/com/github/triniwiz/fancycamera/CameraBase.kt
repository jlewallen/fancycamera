package com.github.triniwiz.fancycamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.OrientationEventListener
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


abstract class CameraBase @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    abstract var whiteBalance: WhiteBalance
    abstract var position: CameraPosition
    abstract var rotation: CameraOrientation
    abstract var flashMode: CameraFlashMode
    abstract var allowExifRotation: Boolean
    abstract var autoSquareCrop: Boolean
    abstract var autoFocus: Boolean
    abstract var saveToGallery: Boolean
    abstract var maxAudioBitRate: Int
    abstract var maxVideoBitrate: Int
    abstract var maxVideoFrameRate: Int
    abstract var disableHEVC: Boolean
    abstract var quality: Quality
    abstract val db: Double
    abstract val amplitude: Double
    abstract val amplitudeEMA: Double
    abstract var isAudioLevelsEnabled: Boolean
    abstract val numberOfCameras: Int
    abstract var detectorType: DetectorType
    var overridePhotoWidth: Int = -1
    var overridePhotoHeight: Int = -1
    abstract fun stop()
    abstract fun release()
    abstract fun startPreview()
    abstract fun stopPreview()
    abstract fun startRecording()
    abstract fun stopRecording()
    abstract fun takePhoto()
    abstract fun hasFlash(): Boolean
    abstract fun cameraRecording(): Boolean
    abstract fun toggleCamera()
    abstract fun getSupportedRatios(): Array<String>
    abstract fun getAvailablePictureSizes(ratio: String): Array<Size>
    abstract var displayRatio: String
    abstract var pictureSize: String
    abstract var zoom: Float
    internal var onBarcodeScanningListener: ImageAnalysisCallback? = null
    internal var onFacesDetectedListener: ImageAnalysisCallback? = null
    internal var onImageLabelingListener: ImageAnalysisCallback? = null
    internal var onObjectDetectedListener: ImageAnalysisCallback? = null
    internal var onPoseDetectedListener: ImageAnalysisCallback? = null
    internal var onTextRecognitionListener: ImageAnalysisCallback? = null
    internal var onSurfaceUpdateListener: SurfaceUpdateListener? = null
    fun setonSurfaceUpdateListener(callback: SurfaceUpdateListener?) {
        onSurfaceUpdateListener = callback
    }

    fun setOnBarcodeScanningListener(callback: ImageAnalysisCallback?) {
        onBarcodeScanningListener = callback
    }

    fun setOnFacesDetectedListener(callback: ImageAnalysisCallback?) {
        onFacesDetectedListener = callback
    }

    fun setOnImageLabelingListener(callback: ImageAnalysisCallback?) {
        onImageLabelingListener = callback
    }

    fun setOnObjectDetectedListener(callback: ImageAnalysisCallback?) {
        onObjectDetectedListener = callback
    }

    fun setOnPoseDetectedListener(callback: ImageAnalysisCallback?) {
        onPoseDetectedListener = callback
    }

    fun setOnTextRecognitionListener(callback: ImageAnalysisCallback?) {
        onTextRecognitionListener = callback
    }

    internal fun stringSizeToSize(value: String): Size {
        val size = value.split("x")
        val width = size[0].toIntOrNull(10) ?: 0
        val height = size[1].toIntOrNull() ?: 0
        return Size(width, height)
    }

    abstract val previewSurface: Any

    internal val mainHandler = Handler(Looper.getMainLooper())

    internal var isBarcodeScanningSupported = false
    internal var isFaceDetectionSupported = false
    internal var isImageLabelingSupported = false
    internal var isObjectDetectionSupported = false
    internal var isPoseDetectionSupported = false
    internal var isTextRecognitionSupported = false
    internal var analysisExecutor = Executors.newCachedThreadPool()
    internal val isMLSupported: Boolean
        get() {
            return isFaceDetectionSupported || isTextRecognitionSupported || isBarcodeScanningSupported ||
                    isPoseDetectionSupported || isImageLabelingSupported || isObjectDetectionSupported
        }

    internal var barcodeScannerOptions: Any? = null
    fun setBarcodeScannerOptions(value: Any) {
        if (!isBarcodeScanningSupported) return
        if (barcodeScannerOptions != null) {
            val BarcodeScannerOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.barcodescanning.BarcodeScanner\$Options")
            if (!BarcodeScannerOptionsClazz.isInstance(value)) return
            barcodeScannerOptions = value
        }
    }


    internal var faceDetectionOptions: Any? = null
    fun setFaceDetectionOptions(value: Any) {
        if (!isFaceDetectionSupported) return
        if (faceDetectionOptions != null) {
            val FaceDetectionOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.facedetection.FaceDetection\$Options")
            if (!FaceDetectionOptionsClazz.isInstance(value)) return
            faceDetectionOptions = value
        }
    }

    internal var imageLabelingOptions: Any? = null
    fun setImageLabelingOptions(value: Any) {
        if (!isImageLabelingSupported) return
        if (imageLabelingOptions != null) {
            val ImageLabelingOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.imagelabeling.ImageLabeling\$Options")
            if (!ImageLabelingOptionsClazz.isInstance(value)) return
            imageLabelingOptions = value
        }
    }


    internal var objectDetectionOptions: Any? = null
    fun setObjectDetectionOptions(value: Any) {
        if (!isObjectDetectionSupported) return
        if (objectDetectionOptions != null) {
            val ObjectDetectionOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.objectdetection.ObjectDetection\$Options")
            if (!ObjectDetectionOptionsClazz.isInstance(value)) return
            objectDetectionOptions = value
        }
    }

    internal fun detectSupport() {

        try {
            Class.forName("com.github.triniwiz.fancycamera.barcodescanning.BarcodeScanner")
            val BarcodeScannerOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.barcodescanning.BarcodeScanner\$Options")
            barcodeScannerOptions = BarcodeScannerOptionsClazz.newInstance()
            isBarcodeScanningSupported = true
        } catch (e: ClassNotFoundException) {
            isBarcodeScanningSupported = false
        }


        try {
            Class.forName("com.github.triniwiz.fancycamera.facedetection.FaceDetection")
            val FaceDetectionOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.facedetection.FaceDetection\$Options")
            faceDetectionOptions = FaceDetectionOptionsClazz.newInstance()
            isFaceDetectionSupported = true
        } catch (e: ClassNotFoundException) {
            isFaceDetectionSupported = false
        }

        try {
            Class.forName("com.github.triniwiz.fancycamera.imagelabeling.ImageLabeling")
            val ImageLabelingOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.imagelabeling.ImageLabeling\$Options")
            imageLabelingOptions = ImageLabelingOptionsClazz.newInstance()
            isImageLabelingSupported = true
        } catch (e: ClassNotFoundException) {
            isImageLabelingSupported = false
        }

        isPoseDetectionSupported = try {
            Class.forName("com.github.triniwiz.fancycamera.posedetection.PoseDetection")
            true
        } catch (e: ClassNotFoundException) {
            false
        }


        try {
            Class.forName("com.github.triniwiz.fancycamera.objectdetection.ObjectDetection")
            val ObjectDetectionOptionsClazz = Class.forName("com.github.triniwiz.fancycamera.objectdetection.ObjectDetection\$Options")
            objectDetectionOptions = ObjectDetectionOptionsClazz.newInstance()
            isObjectDetectionSupported = true
        } catch (e: ClassNotFoundException) {
            isObjectDetectionSupported = false
        }


        isTextRecognitionSupported = try {
            Class.forName("com.github.triniwiz.fancycamera.textrecognition.TextRecognition")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    }

    /** Device orientation in degrees 0-359 */
    internal var currentOrientation: Int = OrientationEventListener.ORIENTATION_UNKNOWN

    internal abstract fun orientationUpdated();

    internal val VIDEO_RECORDER_PERMISSIONS_REQUEST = 868
    internal val VIDEO_RECORDER_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

    internal var mTimer: Timer? = null
    internal var mTimerTask: TimerTask? = null

    internal var isGettingAudioLevels = false
    private var mEMA = 0.0

    val duration: Long
        get() {
            return mDuration
        }

    internal var recorder: MediaRecorder = MediaRecorder()

    internal var listener: CameraEventListener? = null

    private val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            // Based on: https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#setTargetRotation(int)
            val newOrientation = when (orientation) {
                in 0..20, in 340..359 -> 0
                in 70..110 -> 90
                in 250..290 -> 270
                in 160..200 -> 180
                else -> currentOrientation
            }
            if (newOrientation != currentOrientation) {
                Log.d("CameraBase", "Event: onOrientationChanged($orientation) -> currentOrientation = $currentOrientation to $newOrientation");
                currentOrientation = newOrientation
            }
        }
    }

    init {
        orientationEventListener.enable()
    }


    private var timerLock: Any = Any()

    @Volatile
    internal var mDuration = 0L
    internal fun startDurationTimer() {
        mTimer = Timer()
        mTimerTask = object : TimerTask() {
            override fun run() {
                synchronized(timerLock) {
                    mDuration += 1
                }
            }
        }
        mTimer?.schedule(mTimerTask, 0, 1000)
    }

    internal fun stopDurationTimer() {
        mTimerTask?.cancel()
        mTimer?.cancel()
        mDuration = 0
    }


    internal fun initListener(instance: MediaRecorder? = null) {
        if (isAudioLevelsEnabled) {
            if (!hasPermission()) {
                return
            }
            deInitListener()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile("/dev/null")
            try {
                recorder.prepare()
                recorder.start()
                isGettingAudioLevels = true
                mEMA = 0.0
            } catch (e: IOException) {
            } catch (e: Exception) {
            }

        }
    }

    internal fun deInitListener() {
        if (isAudioLevelsEnabled && isGettingAudioLevels) {
            try {
                recorder.stop()
                recorder.reset()
                isGettingAudioLevels = false
            } catch (e: Exception) {
            }

        }
    }

    internal fun getCamcorderProfile(quality: Quality): CamcorderProfile {
        var profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
        when (quality) {
            Quality.MAX_480P -> profile = if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
                CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
            } else {
                getCamcorderProfile(Quality.QVGA)
            }
            Quality.MAX_720P -> profile = if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
                CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
            } else {
                getCamcorderProfile(Quality.MAX_480P)
            }
            Quality.MAX_1080P -> profile = if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
                CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)
            } else {
                getCamcorderProfile(Quality.MAX_720P)
            }
            Quality.MAX_2160P -> profile = try {
                CamcorderProfile.get(CamcorderProfile.QUALITY_2160P)
            } catch (e: Exception) {
                getCamcorderProfile(Quality.HIGHEST)
            }

            Quality.HIGHEST -> profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
            Quality.LOWEST -> profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
            Quality.QVGA -> profile = if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
                CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA)
            } else {
                getCamcorderProfile(Quality.LOWEST)
            }
        }
        return profile
    }

    internal val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
        public override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy:MM:dd", Locale.US)
        }
    }
    internal val TIME_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
        public override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm:ss", Locale.US)
        }
    }
    internal val DATETIME_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
        public override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        }
    }

    internal fun convertToExifDateTime(timestamp: Long): String {
        return DATETIME_FORMAT.get()!!.format(Date(timestamp))
    }

    @Throws(ParseException::class)
    internal fun convertFromExifDateTime(dateTime: String): Date {
        return DATETIME_FORMAT.get()!!.parse(dateTime)!!
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT < 23) {
            true
        } else ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission() {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 868)
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA), VIDEO_RECORDER_PERMISSIONS_REQUEST)
    }

    fun requestAudioPermission() {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.RECORD_AUDIO), VIDEO_RECORDER_PERMISSIONS_REQUEST)
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(context as Activity, VIDEO_RECORDER_PERMISSIONS, VIDEO_RECORDER_PERMISSIONS_REQUEST)
    }

    fun hasPermission(): Boolean {
        return hasCameraPermission() && hasAudioPermission()
    }

    fun hasCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < 23) {
            true
        } else ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < 23) {
            true
        } else ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        internal val EMA_FILTER = 0.6
    }
}