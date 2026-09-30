package io.cloudx.demo.demoapp.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R
import io.cloudx.demo.demoapp.ads.CloudXInit
import io.cloudx.demo.demoapp.ads.arbiter.ArbiterEvent
import io.cloudx.demo.demoapp.ads.arbiter.ArbiterInterstitialController
import io.cloudx.demo.demoapp.ui.log.setupLogListView
import io.cloudx.sdk.CloudXArbiterPlatform
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInitializationListener
import io.cloudx.sdk.CloudXSdkConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Demo host for the Trusted Arbiter interstitial. It brings up both SDKs, builds an
 * [ArbiterInterstitialController], turns its events into status lines and retries with a capped
 * backoff when neither platform fills or a show fails. Every load, show, arbiter and revenue call
 * lives in the controller.
 */
class ArbiterActivity : AppCompatActivity(R.layout.activity_arbiter) {

    private lateinit var initializationStatus: TextView
    private lateinit var arbiterStatus: TextView
    private lateinit var interstitialStatus: TextView
    private lateinit var revenueStatus: TextView
    private lateinit var showButton: Button
    private lateinit var cloudXStatus: String
    private lateinit var adMobStatus: String

    private var controller: ArbiterInterstitialController? = null
    private var retryJob: Job? = null
    private var retryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializationStatus = findViewById(R.id.arbiter_initialization_status)
        arbiterStatus = findViewById(R.id.arbiter_round_status)
        interstitialStatus = findViewById(R.id.arbiter_interstitial_status)
        revenueStatus = findViewById(R.id.arbiter_revenue_status)
        showButton = findViewById(R.id.btn_arbiter_show)
        showButton.setOnClickListener { showInterstitial() }
        setupLogListView(findViewById(R.id.arbiter_log_list))
        cloudXStatus = getString(R.string.sdk_init_cloudx_initializing)
        adMobStatus = getString(R.string.sdk_init_admob_initializing)
        publishInitializationStatus()

        initializeAdMob()
        initializeCloudX()
        lifecycleScope.launch {
            delay(INITIALIZATION_TIMEOUT_MS)
            if (controller == null) {
                cloudXStatus = getString(R.string.sdk_init_cloudx_no_response)
                publishInitializationStatus()
                createController(cloudXAvailable = false)
            }
        }
    }

    override fun onDestroy() {
        retryJob?.cancel()
        controller?.dispose()
        controller = null
        super.onDestroy()
    }

    /*
     * Loads do not wait for this. Google asks apps to wait for the completion listener only when they
     * use AdMob Mediation, which this flow does not.
     */
    private fun initializeAdMob() {
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(applicationContext) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    adMobStatus = getString(R.string.sdk_init_admob_ready)
                    publishInitializationStatus()
                }
            }
        }
    }

    private fun initializeCloudX() {
        CloudXInit.initialize(
            applicationContext,
            listener = object : CloudXInitializationListener {
                override fun onInitialized(configuration: CloudXSdkConfiguration) {
                    if (isDestroyed) return
                    cloudXStatus = if (controller == null) {
                        getString(R.string.sdk_init_cloudx_initialized)
                    } else {
                        getString(R.string.sdk_init_cloudx_initialized_late)
                    }
                    publishInitializationStatus()
                    createController(cloudXAvailable = true)
                }

                override fun onInitializationFailed(cloudXError: CloudXError) {
                    if (isDestroyed) return
                    cloudXStatus = getString(R.string.sdk_init_cloudx_failed, cloudXError.formattedMessage)
                    publishInitializationStatus()
                    createController(cloudXAvailable = false)
                }
            },
        )
    }

    private fun createController(cloudXAvailable: Boolean) {
        if (controller != null) return
        val newController = ArbiterInterstitialController(
            activity = this,
            cloudXAdUnitId = DemoConfig.INTERSTITIAL_AD_UNIT_ID,
            adMobAdUnitId = DemoConfig.ADMOB_INTERSTITIAL_TEST_AD_UNIT_ID,
            cloudXAvailable = cloudXAvailable,
            onEvent = ::onArbiterEvent,
        )
        controller = newController
        interstitialStatus.setText(R.string.arbiter_loading)
        showButton.isEnabled = true
        newController.load()
    }

    private fun showInterstitial() {
        val controller = controller ?: return
        val platform = controller.show()
        when {
            platform != null -> DemoLog.i(TAG, "Requested interstitial ($platform)")
            controller.isShowing -> Unit
            // A load or a round is still running, and its result will update the status lines.
            controller.isBusy -> arbiterStatus.setText(R.string.arbiter_round_in_progress)
            // A tap must not skip the backoff of a retry that is already scheduled.
            retryJob?.isActive == true -> interstitialStatus.setText(R.string.arbiter_retry_pending)
            else -> {
                /*
                 * A real app carries on without an ad here. The demo reloads so that the next tap
                 * has a winner to show.
                 */
                interstitialStatus.setText(R.string.arbiter_no_winner_reloading)
                controller.load()
            }
        }
    }

    private fun onArbiterEvent(event: ArbiterEvent) {
        when (event) {
            is ArbiterEvent.Loaded -> {
                retryJob?.cancel()
                retryCount = 0
                interstitialStatus.text = getString(R.string.arbiter_loaded, event.platform)
                DemoLog.i(TAG, "Interstitial loaded (${event.platform})")
            }
            is ArbiterEvent.LoadFailed -> {
                interstitialStatus.text = getString(R.string.arbiter_load_failed, event.platform, event.message)
                DemoLog.w(TAG, "Interstitial failed to load (${event.platform}): ${event.message}")
            }
            is ArbiterEvent.ArbiterCompleted -> {
                val bids = resources.getQuantityString(R.plurals.arbiter_bids, event.bidCount, event.bidCount)
                arbiterStatus.text = if (event.platform == CloudXArbiterPlatform.NONE) {
                    getString(R.string.arbiter_round_none, bids)
                } else {
                    getString(R.string.arbiter_round_winner, event.platform, bids)
                }
            }
            ArbiterEvent.NoCandidates -> {
                arbiterStatus.setText(R.string.arbiter_round_no_candidates)
                scheduleRetry(getString(R.string.arbiter_no_fill))
            }
            is ArbiterEvent.Shown -> {
                interstitialStatus.text = getString(R.string.arbiter_showing, event.platform)
                DemoLog.i(TAG, "Interstitial shown (${event.platform})")
            }
            is ArbiterEvent.ShowFailed ->
                scheduleRetry(getString(R.string.arbiter_show_failed, event.platform, event.message))
            is ArbiterEvent.Closed -> {
                interstitialStatus.text = getString(R.string.arbiter_closed, event.platform)
                DemoLog.i(TAG, "Interstitial closed (${event.platform})")
                controller?.load()
            }
            is ArbiterEvent.Clicked -> DemoLog.i(TAG, "Interstitial clicked (${event.platform})")
            is ArbiterEvent.RevenueReported -> revenueStatus.text =
                getString(R.string.arbiter_revenue_reported, event.revenue, event.currencyCode, event.accepted)
        }
    }

    /*
     * Retry policy when neither platform filled or a show failed: 2 s, 4 s, 8 s and so on up to 60 s,
     * reset by the next successful load. A fixed short delay would turn sustained no-fill into a tight
     * request loop against both networks.
     */
    private fun scheduleRetry(message: String) {
        val delayMs = min(RETRY_BASE_DELAY_MS shl retryCount, RETRY_MAX_DELAY_MS)
        retryCount = min(retryCount + 1, RETRY_MAX_SHIFT)
        interstitialStatus.text = getString(R.string.arbiter_retrying, message, delayMs / 1_000)
        DemoLog.w(TAG, "$message; retrying in ${delayMs / 1_000}s")
        retryJob?.cancel()
        retryJob = lifecycleScope.launch {
            delay(delayMs)
            controller?.load()
        }
    }

    private fun publishInitializationStatus() {
        initializationStatus.text = getString(R.string.sdk_init_status, cloudXStatus, adMobStatus)
    }

    private companion object {
        const val TAG = "Arbiter"
        const val INITIALIZATION_TIMEOUT_MS = 15_000L
        const val RETRY_BASE_DELAY_MS = 2_000L
        const val RETRY_MAX_DELAY_MS = 60_000L
        const val RETRY_MAX_SHIFT = 5
    }
}
