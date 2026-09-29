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
import io.cloudx.demo.demoapp.ads.firstlook.AdMobFirstLookSource
import io.cloudx.demo.demoapp.ads.firstlook.CloudXFirstLookSource
import io.cloudx.demo.demoapp.ads.firstlook.FirstLookEvent
import io.cloudx.demo.demoapp.ads.firstlook.FirstLookInterstitialController
import io.cloudx.demo.demoapp.ui.log.setupLogListView
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInitializationConfiguration
import io.cloudx.sdk.CloudXInitializationListener
import io.cloudx.sdk.CloudXSdkConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Demo host for a CloudX-first interstitial with a lazy AdMob fallback. It waits for both SDKs,
 * builds a [FirstLookInterstitialController], retries failed loads and shows with a capped
 * backoff and reports each step in the status lines.
 */
class FirstLookActivity : AppCompatActivity(R.layout.activity_first_look) {

    private lateinit var initializationStatus: TextView
    private lateinit var interstitialStatus: TextView
    private lateinit var showButton: Button
    private lateinit var cloudXStatus: String
    private lateinit var adMobStatus: String

    private var controller: FirstLookInterstitialController? = null
    private var retryJob: Job? = null
    private var retryCount = 0
    private var adMobReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializationStatus = findViewById(R.id.first_look_initialization_status)
        interstitialStatus = findViewById(R.id.first_look_interstitial_status)
        showButton = findViewById(R.id.btn_first_look_show)
        showButton.setOnClickListener { showInterstitial() }
        setupLogListView(findViewById(R.id.first_look_log_list))
        cloudXStatus = getString(R.string.first_look_cloudx_initializing)
        adMobStatus = getString(R.string.first_look_admob_initializing)
        publishInitializationStatus()

        initializeAdMob()
        initializeCloudX()
        lifecycleScope.launch {
            delay(INITIALIZATION_TIMEOUT_MS)
            if (controller == null) {
                cloudXStatus = getString(R.string.first_look_cloudx_no_response)
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

    private fun initializeAdMob() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(applicationContext) {
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        adMobReady = true
                        adMobStatus = getString(R.string.first_look_admob_ready)
                        publishInitializationStatus()
                        controller?.onAdMobInitialized()
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    adMobStatus = getString(R.string.first_look_admob_failed)
                    publishInitializationStatus()
                    DemoLog.e(TAG, "AdMob initialization failed: ${error.message}")
                }
            }
        }
    }

    /*
     * CloudXStartup already started initialization when the app launched. Calling initialize again
     * does not start a second one: the SDK reports the outcome of the attempt that is running, and
     * starts a new attempt only if that one failed. The listener runs on the main thread.
     */
    private fun initializeCloudX() {
        CloudX.initialize(
            context = applicationContext,
            configuration = CloudXInitializationConfiguration.builder(DemoConfig.APP_KEY).build(),
            listener = object : CloudXInitializationListener {
                override fun onInitialized(configuration: CloudXSdkConfiguration) {
                    if (isDestroyed) return
                    cloudXStatus = if (controller == null) {
                        getString(R.string.first_look_cloudx_initialized)
                    } else {
                        getString(R.string.first_look_cloudx_initialized_late)
                    }
                    publishInitializationStatus()
                    createController(cloudXAvailable = true)
                }

                override fun onInitializationFailed(cloudXError: CloudXError) {
                    if (isDestroyed) return
                    cloudXStatus = getString(R.string.first_look_cloudx_failed, cloudXError.formattedMessage)
                    publishInitializationStatus()
                    createController(cloudXAvailable = false)
                }
            },
        )
    }

    private fun createController(cloudXAvailable: Boolean) {
        if (controller != null) return
        val newController = FirstLookInterstitialController(
            cloudX = if (cloudXAvailable) CloudXFirstLookSource(this, DemoConfig.INTERSTITIAL_AD_UNIT_ID) else null,
            adMob = AdMobFirstLookSource(this, DemoConfig.ADMOB_INTERSTITIAL_TEST_AD_UNIT_ID),
            onEvent = ::onInterstitialEvent,
        )
        controller = newController
        interstitialStatus.setText(R.string.first_look_loading)
        showButton.isEnabled = true
        if (adMobReady) newController.onAdMobInitialized()
        newController.load()
    }

    private fun showInterstitial() {
        val source = controller?.show()
        when {
            source != null -> DemoLog.i(TAG, "Requested interstitial ($source)")
            // A tap must not skip the backoff of a retry that is already scheduled.
            retryJob?.isActive == true -> interstitialStatus.setText(R.string.first_look_retry_pending)
            else -> {
                interstitialStatus.setText(R.string.first_look_no_ad_ready)
                controller?.load()
            }
        }
    }

    private fun onInterstitialEvent(event: FirstLookEvent) {
        when (event) {
            is FirstLookEvent.Loaded -> {
                retryJob?.cancel()
                retryCount = 0
                interstitialStatus.text = getString(R.string.first_look_loaded, event.source)
                DemoLog.i(TAG, "Interstitial loaded (${event.source})")
            }
            is FirstLookEvent.LoadFailed ->
                scheduleRetry(getString(R.string.first_look_load_failed, event.source, event.message))
            is FirstLookEvent.Shown -> {
                interstitialStatus.text = getString(R.string.first_look_showing, event.source)
                DemoLog.i(TAG, "Interstitial shown (${event.source})")
            }
            is FirstLookEvent.ShowFailed ->
                scheduleRetry(getString(R.string.first_look_show_failed, event.source, event.message))
            is FirstLookEvent.Closed -> {
                interstitialStatus.text = getString(R.string.first_look_closed, event.source)
                DemoLog.i(TAG, "Interstitial closed (${event.source})")
                controller?.load()
            }
            is FirstLookEvent.Clicked -> DemoLog.i(TAG, "Interstitial clicked (${event.source})")
        }
    }

    /*
     * Retry policy after a load or show failure: 2 s, 4 s, 8 s and so on up to 60 s, reset by the
     * next successful load. A fixed short delay would turn sustained no-fill into a tight request
     * loop against the fallback network.
     */
    private fun scheduleRetry(message: String) {
        val delayMs = min(RETRY_BASE_DELAY_MS shl retryCount, RETRY_MAX_DELAY_MS)
        retryCount = min(retryCount + 1, RETRY_MAX_SHIFT)
        interstitialStatus.text = getString(R.string.first_look_retrying, message, delayMs / 1_000)
        DemoLog.w(TAG, "$message; retrying in ${delayMs / 1_000}s")
        retryJob?.cancel()
        retryJob = lifecycleScope.launch {
            delay(delayMs)
            controller?.load()
        }
    }

    private fun publishInitializationStatus() {
        initializationStatus.text = getString(R.string.first_look_initialization_status, cloudXStatus, adMobStatus)
    }

    private companion object {
        const val TAG = "FirstLook"
        const val INITIALIZATION_TIMEOUT_MS = 15_000L
        const val RETRY_BASE_DELAY_MS = 2_000L
        const val RETRY_MAX_DELAY_MS = 60_000L
        const val RETRY_MAX_SHIFT = 5
    }
}
