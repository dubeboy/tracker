package com.dubedivine.tracker.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.dubedivine.tracker.BuildConfig
import com.dubedivine.tracker.activity.MainActivity
import com.dubedivine.tracker.data.remote.FireStorePersitanceHelper
import com.dubedivine.tracker.util.IMAGE_PATH
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import org.openalpr.OpenALPR
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class CameraService : Service() {

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var mFireStorePersitanceHelper: FireStorePersitanceHelper
    private lateinit var intent: Intent

    //   A callback object for receiving updates about the state of a camera capture session.
    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice?) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened")
            cameraDevice = camera
            actOnReadyCameraDevice()
        }

        override fun onDisconnected(camera: CameraDevice?) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected")
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            Log.e(TAG, "CameraDevice.StateCallback onError $error")
        }

    }

    private val cameraSessionStateCallBack = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession?) {

        }

        override fun onConfigured(session: CameraCaptureSession?) {

        }

        override fun onReady(session: CameraCaptureSession) {
            this@CameraService.cameraCaptureSession = session
            try {
                // With this method, the camera device will continually capture images using the
                // settings in the provided CaptureRequest, at the maximum rate possible.
                session.setRepeatingRequest(createCaptureRequest(), null, null)
                Thread.sleep(2000)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "CameraAccessException thrown")
                e.printStackTrace()
            }
        }

    }


    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        Log.d(TAG, "Image is availbale mate")
            val img: Image? = reader?.acquireLatestImage()
            if (img != null) {
                processImage(img)
            }
    }

    fun analyseImage(image: File): String {
//        val ANDROID_DATA_DIR = "/data/data/com.sandro.openalprsample"
        val openAlprConfFile = MainActivity.ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf"
        return OpenALPR.Factory
                .create(this, MainActivity.ANDROID_DATA_DIR)
                .recognizeWithCountryRegionNConfig("eu", "", image.absolutePath, openAlprConfFile, 10)
    }

    private fun processImage(image: Image) {
            var buffer: ByteBuffer? = null
            var bytes: ByteArray
            var success = false
            var file = File(IMAGE_PATH)
            var output: FileOutputStream? = null

            if (image.format == ImageFormat.JPEG) {
                buffer = image.planes[0].buffer
                bytes = ByteArray(buffer.remaining()) // makes byte array large enough to hold image
                buffer.get(bytes) // copies image from buffer to byte array
                try {
                    output = FileOutputStream(file)
                    output.write(bytes)    // write the byte array to file
                    success = true

                    //Todo these threads should be added to thread pool please

                    AnalyseImageThreadPool.execute(Runnable {
                        val result: String? = analyseImage(file)
                        if (result != null && result.isNotBlank()) {
                            Log.d(TAG, "got number plate: $result")
                            mFireStorePersitanceHelper.persistNumberPlateToCloud(result)
                            intent.putExtra(EXTRA_IMAGE_RESULT, "Sent number plate")
                            sendBroadcast(intent)
                        } else {
                            Log.w(TAG, "could not get the number plate")
                            intent.putExtra(EXTRA_IMAGE_RESULT, "0000000")
                            sendBroadcast(intent) // send some message to the MainActivity
                        }
                    })
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    image.close()
                    // close this to free up buffer for other images
                    if (null != output) {
                        try {
                            output.close();
                        } catch (e: IOException) {
                            e.printStackTrace();
                        }
                    }
                }

            }

    }


    private fun getCamera(manager: CameraManager): String? {
        return try {
            manager.cameraIdList[0]
        } catch (ce: CameraAccessException) {
            ce.printStackTrace(); null
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand flags  $flags startId $startId")

        val width = intent.getIntExtra(WIDTH, 0)
        val height = intent.getIntExtra(HEIGHT, 0)

        readyCamera(width, height)

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        try {
            cameraCaptureSession!!.abortCaptures()
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
        }
        cameraCaptureSession!!.close()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate service")
        mFireStorePersitanceHelper =
                FireStorePersitanceHelper(
                        this,
                        OnSuccessListener {
                            Log.d(TAG, "saved item successfully here id the Id:  ${it.id}")
                        }, OnFailureListener {
                    Log.e(TAG, "failed to save to fireStore. Error:   ${it.message}")
                }
                )

        intent = Intent(BuildConfig.APPLICATION_ID)
    }

    private fun readyCamera(width: Int, height: Int) {

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val pickedCamera = getCamera(manager)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // send a notification to the top of the screen asking the user to open the app so that
                // we can grant it permissions
                return
            }
            manager.openCamera(pickedCamera, cameraDeviceStateCallback, null)
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2 /* images buffered */)
            imageReader!!.setOnImageAvailableListener(onImageAvailableListener, null)
            Log.d(TAG, "imageReader created")
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
        }
    }

    private fun createCaptureRequest(): CaptureRequest? {
        return try {
            val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(imageReader!!.surface)
            builder.build()
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
            null
        }
    }

    private fun actOnReadyCameraDevice() {
        try {
            cameraDevice!!.createCaptureSession(Arrays.asList(imageReader!!.surface), cameraSessionStateCallBack, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    companion object {
        private const val TAG = "CameraService"
        const val WIDTH: String = "width"
        const val HEIGHT: String = "height"
        private const val CAMERA_CHOICE = CameraCharacteristics.LENS_FACING_BACK
        const val EXTRA_IMAGE_RESULT = "extra_img_res"

    }
}
