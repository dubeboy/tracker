package com.dubedivine.tracker.util

import android.app.Activity
import android.app.Service
import android.content.Context
import android.os.Environment
import android.util.DisplayMetrics
import com.dubedivine.tracker.activity.MainActivity
import org.openalpr.OpenALPR
import java.io.File

fun getWindowPhoneWithAndHeight(context: Activity): Pair<Int, Int> {
    val displayMetrics = DisplayMetrics()
    context.windowManager.defaultDisplay.getMetrics(displayMetrics)
    val height = displayMetrics.heightPixels
    val width = displayMetrics.widthPixels
    return Pair(width, height)
}

fun analyseImage(context: Context, image: File): String {
//        val ANDROID_DATA_DIR = "/data/data/com.sandro.openalprsample"
    val openAlprConfFile = MainActivity.ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf"
    return OpenALPR.Factory
            .create(context, MainActivity.ANDROID_DATA_DIR)
            .recognizeWithCountryRegionNConfig("us", "", image.absolutePath, openAlprConfFile, 10)
}

val IMAGE_PATH = Environment.getExternalStorageDirectory().toString() + "/Pictures/image.jpg"
