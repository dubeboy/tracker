package com.dubedivine.tracker.broadcastReciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import com.dubedivine.tracker.service.CameraService

class ReceiveMessages : BroadcastReceiver {
    private lateinit var tvStatus: TextView

    constructor() : super()

    constructor(tvStatus: TextView) : this() {
        this.tvStatus = tvStatus
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val numberPlate: String = intent?.getStringExtra(CameraService.EXTRA_IMAGE_RESULT)
                ?: "Scanning..."
        Log.d(TAG, "got the number plate : $numberPlate")
        tvStatus.text = numberPlate
    }

    companion object {
        const val TAG = "ReceiveMessages"
    }
}
