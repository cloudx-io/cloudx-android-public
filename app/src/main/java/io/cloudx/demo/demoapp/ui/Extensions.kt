package io.cloudx.demo.demoapp.ui

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar

fun Activity.shortSnackbar(contextView: View, text: String) {
    Snackbar.make(contextView, text, Snackbar.LENGTH_SHORT).apply {
        anchorView = contextView
    }.show()
}
