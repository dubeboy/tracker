package com.dubedivine.tracker.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.dubedivine.tracker.activity.MainActivity
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
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onConfigured(session: CameraCaptureSession?) {

        }

        override fun onReady(session: CameraCaptureSession) {
            this@CameraService.cameraCaptureSession = session
            try {
                // With this method, the camera device will continually capture images using the
                // settings in the provided CaptureRequest, at the maximum rate possible.
                session.setRepeatingRequest(createCaptureRequest(), null, null)
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
            img.close()
        }
    }

    fun analyseImage(image: File): String {
//        val ANDROID_DATA_DIR = "/data/data/com.sandro.openalprsample"
        val openAlprConfFile = MainActivity.ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf"
        return OpenALPR.Factory
                .create(this, MainActivity.ANDROID_DATA_DIR)
                .recognizeWithCountryRegionNConfig("us", "", image.absolutePath, openAlprConfFile, 10)
    }


    private fun processImage(image: Image) {
          var buffer: ByteBuffer? = null
        var bytes: ByteArray
        var success = false;
        var file: File =  File(Environment.getExternalStorageDirectory().toString() + "/Pictures/image.jpg")
        var output: FileOutputStream? = null

        if(image.format == ImageFormat.JPEG) {
            buffer = image.planes[0].buffer
            bytes =  ByteArray(buffer.remaining()) // makes byte array large enough to hold image
            buffer.get(bytes); // copies image from buffer to byte array
            try {
                output = FileOutputStream(file)
                output.write(bytes)    // write the byte array to file
//                j++;
                success = true
                val result = analyseImage(file)
                Log.d(TAG, "the result is $result")
            } catch ( e: FileNotFoundException) {
                e.printStackTrace()
            } catch ( e: IOException) {
                e.printStackTrace()
            } finally {
                image.close() // close this to free up buffer for other images
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
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cOrientation != CAMERA_CHOICE) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand flags  $flags startId $startId")

        readyCamera()

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        try {
            cameraCaptureSession!!.abortCaptures();
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
        }
        cameraCaptureSession!!.close()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate service")
        super.onCreate()
    }

    private fun readyCamera() {
         val manager =  getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val pickedCamera = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // send a notification to the top of the screen asking the user to open the app so that
                // we can grant it permissions
                return
            }
            manager.openCamera(pickedCamera, cameraDeviceStateCallback, null);
            imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
            imageReader!!.setOnImageAvailableListener(onImageAvailableListener, null)
            Log.d(TAG, "imageReader created");
        } catch ( e: CameraAccessException){
            Log.e(TAG, e.message)
        }
    }

    private fun createCaptureRequest(): CaptureRequest? {
        try {
            val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(imageReader!!.surface)
            return builder.build()
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
            return null
        }
    }

    private fun actOnReadyCameraDevice() {
        try {
            cameraDevice!!.createCaptureSession(Arrays.asList(imageReader!!.surface), cameraSessionStateCallBack, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.message)
        }
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }


    companion object {
        private const val TAG = "CameraService"
        private const val CAMERA_CHOICE = CameraCharacteristics.LENS_FACING_BACK
    }
}
