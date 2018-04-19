package com.dubedivine.tracker.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
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
import android.util.Log
import com.dubedivine.tracker.util.ActivityExtensions.toast
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.TextView
import com.dubedivine.tracker.BuildConfig
import com.dubedivine.tracker.service.CameraService
import kotlinx.android.synthetic.main.activity_main.*
import org.openalpr.OpenALPR
import java.io.File


class MainActivity : AppCompatActivity()/*, SurfaceHolder.Callback, Handler.Callback*/ {


    private lateinit var dialog: AlertDialog.Builder
    private var imageReader: ImageReader? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = createAlertDialog()

        val intent = Intent(this, CameraService::class.java)
        startService(intent)

        // the is for the over head
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
            initializeView()
        }





        // for now I can do it easy just capture the images at an interval
        fab_capture_plate.setOnClickListener({
          //  mCameraDevice?
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    private fun createAlertDialog(): AlertDialog.Builder {
        val view = layoutInflater.inflate(R.layout.layout_dialog_main_input_lost_number_plate, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(view).setPositiveButton("REPORT", DialogInterface.OnClickListener
        { dialog, which ->
            val numberPlate = view.findViewById<TextView>(R.id.et_number_plate)
            reportNumberPlate(numberPlate.text)
            dialog.dismiss()
        } ).create()

        return builder
    }

    private fun reportNumberPlate(text: CharSequence?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.menu_main_report -> {
                dialog.show()
            }
            R.id.menu_show_reported_cars -> {
               startActivity(StolenCars.getStartIntent(this))
            }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        //requesting permission
        val permissionCameraCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val permissionWriteExtStorageCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val permissionReadExtStorageCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val permissionCheck = permissionCameraCheck + permissionWriteExtStorageCheck + permissionReadExtStorageCheck // should be 0 + 0 + 0 = 0

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_CAMERA)
                toast("request permission")
            }
        } else {
            toast("PERMISSION_ALREADY_GRANTED")
            try {

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

        }
    }


    override fun onStop() {
        super.onStop()

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


    companion object {
        private const val CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084
        private const val TAG = "MAIN_ACTIVITY"
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1242
        private const val MSG_CAMERA_OPENED = 1
        private const val MSG_SURFACE_READY = 2

        const val ANDROID_DATA_DIR = "/data/data/${BuildConfig.APPLICATION_ID}"
    }
}
