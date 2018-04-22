package com.dubedivine.tracker.service

import java.util.concurrent.ThreadPoolExecutor
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit



// single instance class
object AnalyseImageThreadPool {
    private val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    private var mHandler: Handler
    private val tp: ThreadPoolExecutor

    init {
        mHandler = object: Handler(Looper.getMainLooper())  {
            override fun handleMessage(msg: Message) {
                when (msg.what) {

                }
            }
        }

        tp = ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES,
                1,
                TimeUnit.SECONDS,
                LinkedBlockingDeque()
        )
    }

    fun execute(r: Runnable) {
        tp.execute(r)
    }

}