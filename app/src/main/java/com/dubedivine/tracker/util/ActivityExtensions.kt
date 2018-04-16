package com.dubedivine.tracker.util

import android.app.Activity
import android.widget.Toast

object ActivityExtensions {

    fun Activity.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}