package io.cloudx.demo.demoapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R

/**
 * Demo-only launch screen that picks which demo flow to enter. It makes no SDK calls. General opens
 * [GeneralActivity], the CloudX integration sample. First Look and Arbiter/TPA are placeholders for
 * flows this app does not have yet, so their buttons are disabled.
 *
 * The screen finishes once a flow is picked, so it only shows again when the app starts from scratch.
 */
class OptionsActivity : AppCompatActivity(R.layout.activity_options) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btn_general).setOnClickListener { openGeneral() }
    }

    private fun openGeneral() {
        DemoLog.i(TAG, "Opening the General demo")
        startActivity(Intent(this, GeneralActivity::class.java))
        finish()
    }

    private companion object {
        const val TAG = "OptionsActivity"
    }
}
