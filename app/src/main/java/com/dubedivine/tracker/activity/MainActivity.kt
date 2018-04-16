package com.dubedivine.tracker.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.dubedivine.tracker.R
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Button
import com.dubedivine.tracker.service.MainControlService
import android.widget.Toast
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.util.Log
import com.dubedivine.tracker.util.ActivityExtensions.toast
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.support.v4.content.ContextCompat
import android.view.Surface


class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, Handler.Callback {


    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mCameraManager: CameraManager
    private lateinit var mCameraIDsList: Array<out String>
    private lateinit var mCameraStateCB: CameraDevice.StateCallback
    private var mCameraDevice: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private val mHandler = Handler(this)
    private var mIsCameraConfigured: Boolean = false
    private var mSurfaceCreated = true
    private var mCameraSurface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
            initializeView()
        }

//        this.mBtnCapture = findViewById(R.id.surfaceView) as Button
        this.mSurfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        this.mSurfaceHolder = this.mSurfaceView.getHolder()
        this.mSurfaceHolder.addCallback(this)
        this.mCameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            mCameraIDsList = this.mCameraManager.cameraIdList
            for (id in mCameraIDsList) {
                Log.v(TAG, "CameraID: $id")
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            toast("Your phone does not have a camera")
            finish()
        }

        mCameraStateCB = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                toast("onOpened")
                mCameraDevice = camera
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED)
            }

            override fun onDisconnected(camera: CameraDevice) {
                toast("onDisconnected")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                toast("onError")
            }
        }


    }

    override fun onStart() {
        super.onStart()

        //requesting permission
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
                toast("request permission")
            }
        } else {
            toast("PERMISSION_ALREADY_GRANTED")
            try {
                mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, Handler())
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

        }
    }


    override fun onStop() {
        super.onStop()
        try {
            if (mCaptureSession != null) {
                mCaptureSession!!.stopRepeating()
                mCaptureSession!!.close()
                mCaptureSession = null
            }

            mIsCameraConfigured = false
        } catch (e: CameraAccessException) {
            // Doesn't matter, cloising device anyway
            e.printStackTrace()
        } catch (e2: IllegalStateException) {
            // Doesn't matter, cloising device anyway
            e2.printStackTrace()
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
                mCameraDevice = null
                mCaptureSession = null
            }
        }
    }

    /**
     * Set and initialize the view elements.
     */
    private fun initializeView() {
        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            startService(Intent(this@MainActivity, MainControlService::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            if (resultCode == Activity.RESULT_OK) {
                initializeView()
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mCameraSurface = holder!!.surface
        mSurfaceCreated = true
        mHandler.sendEmptyMessage(MSG_SURFACE_READY)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mSurfaceCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mCameraSurface = holder!!.getSurface();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                try {
                    mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, Handler())
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }

        }
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_CAMERA_OPENED, MSG_SURFACE_READY ->
                // if both surface is created and camera device is opened
                // - ready to set up preview and other things
                if (mSurfaceCreated && mCameraDevice != null
                        && !mIsCameraConfigured) {
                    configureCamera()
                }
        }

        return true
    }


    private fun configureCamera() {
        // prepare list of surfaces to be used in capture requests
        val sfl = ArrayList<Surface>()

        sfl.add(mCameraSurface!!) // surface for viewfinder preview

        // configure camera with all the surfaces to be ever used
        try {
            mCameraDevice!!.createCaptureSession(sfl, CaptureSessionListener(), null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        mIsCameraConfigured = true
    }

    inner class CaptureSessionListener : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession?) {
            Log.d(TAG, "CaptureSessionConfigure failed")
        }

        override fun onConfigured(session: CameraCaptureSession?) {
            Log.d(TAG, "CaptureSessionConfigure onConfigured")
            mCaptureSession = session

            try {
                val previewRequestBuilder = mCameraDevice!!
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder.addTarget(mCameraSurface)
                mCaptureSession!!.setRepeatingRequest(previewRequestBuilder.build(),
                        null, null)
            } catch (e: CameraAccessException) {
                Log.d(TAG, "setting up preview failed");
                e.printStackTrace();
            }
        }

    }


    companion object {
        private const val CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084
        private const val TAG = "MAIN_ACTIVITY"
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1242
        private val MSG_CAMERA_OPENED = 1
        private val MSG_SURFACE_READY = 2

    }
}
