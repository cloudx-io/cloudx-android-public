package io.cloudx.demo.demoapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R

/**
 * Demo-only launch screen that picks which demo flow to enter. It makes no SDK calls.
 * [GeneralActivity] is the CloudX integration sample. [FirstLookActivity] demonstrates an
 * interstitial fallback. [ArbiterActivity] loads CloudX and AdMob in parallel and lets Trusted
 * Arbiter pick the interstitial to show.
 *
 * The screen finishes once a flow is picked, so it only shows again when the app starts from scratch.
 */
class OptionsActivity : AppCompatActivity(R.layout.activity_options) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btn_general).setOnClickListener { openGeneral() }
        findViewById<Button>(R.id.btn_first_look).setOnClickListener { openFirstLook() }
        findViewById<Button>(R.id.btn_arbiter_tpa).setOnClickListener { openArbiter() }
    }

    private fun openGeneral() {
        DemoLog.i(TAG, "Opening the General demo")
        startActivity(Intent(this, GeneralActivity::class.java))
        finish()
    }

    private fun openFirstLook() {
        DemoLog.i(TAG, "Opening the First Look demo")
        startActivity(Intent(this, FirstLookActivity::class.java))
        finish()
    }

    private fun openArbiter() {
        DemoLog.i(TAG, "Opening the Arbiter/TPA demo")
        val arbiterIntent = Intent(this, ArbiterActivity::class.java)
        // ArbiterActivity is not exported, so the QA launch extra reaches it only through this screen.
        intent.getStringExtra(DemoConfig.EXTRA_ADMOB_MANUAL_REVENUE_PER_IMPRESSION_USD)?.let {
            arbiterIntent.putExtra(DemoConfig.EXTRA_ADMOB_MANUAL_REVENUE_PER_IMPRESSION_USD, it)
        }
        startActivity(arbiterIntent)
        finish()
    }

    private companion object {
        const val TAG = "OptionsActivity"
    }
}
