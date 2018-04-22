package com.dubedivine.tracker.util

import android.app.Activity
import android.support.design.widget.Snackbar
import android.widget.Toast

object ActivityExtensions {

    fun Activity.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    fun Activity.snack(msg: String) {
        Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }
}