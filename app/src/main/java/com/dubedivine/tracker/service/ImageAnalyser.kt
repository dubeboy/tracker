package com.dubedivine.tracker.service

import android.content.Context
import com.dubedivine.tracker.util.analyseImage
import java.io.Console
import java.io.File

class ImageAnalyser(private val image: File, private val context: Context) : Runnable {
    override fun run() {
        analyseImage(context, image)
    }
}