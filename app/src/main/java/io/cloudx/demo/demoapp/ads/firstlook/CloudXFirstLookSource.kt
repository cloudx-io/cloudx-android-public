package io.cloudx.demo.demoapp.ads.firstlook

import android.app.Activity
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXAdRevenueListener
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInterstitialAd
import io.cloudx.sdk.CloudXInterstitialListener

/** CloudX's leg of the First Look interstitial flow. */
class CloudXFirstLookSource(activity: Activity, adUnitId: String) :
    FirstLookInterstitialSource,
    CloudXInterstitialListener,
    CloudXAdRevenueListener {

    private var activity: Activity? = activity
    private val ad: CloudXInterstitialAd = CloudX.createInterstitial(activity.applicationContext, adUnitId).also {
        it.listener = this
        it.revenueListener = this
    }

    override var onEvent: ((FirstLookEvent) -> Unit)? = null

    override val isReady: Boolean
        get() = ad.isAdReady

    override fun load() {
        try {
            ad.load()
        } catch (error: Exception) {
            onEvent?.invoke(FirstLookEvent.LoadFailed(FirstLookSource.CLOUDX, error.message ?: "Load failed"))
        }
    }

    override fun show() {
        val host = activity
        if (host == null) {
            onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.CLOUDX, "Activity unavailable"))
        } else {
            try {
                ad.show(host)
            } catch (error: Exception) {
                onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.CLOUDX, error.message ?: "Show failed"))
            }
        }
    }

    override fun dispose() {
        activity = null
        ad.listener = null
        ad.revenueListener = null
        ad.destroy()
        onEvent = null
    }

    override fun onAdLoaded(cloudXAd: CloudXAd) {
        onEvent?.invoke(FirstLookEvent.Loaded(FirstLookSource.CLOUDX))
    }

    override fun onAdLoadFailed(adUnitId: String, cloudXError: CloudXError) {
        DemoLog.w(TAG, "CloudX interstitial failed to load: ${cloudXError.formattedMessage}; trying AdMob")
        onEvent?.invoke(FirstLookEvent.LoadFailed(FirstLookSource.CLOUDX, cloudXError.formattedMessage))
    }

    override fun onAdDisplayed(cloudXAd: CloudXAd) {
        onEvent?.invoke(FirstLookEvent.Shown(FirstLookSource.CLOUDX))
    }

    override fun onAdDisplayFailed(cloudXAd: CloudXAd, cloudXError: CloudXError) {
        onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.CLOUDX, cloudXError.formattedMessage))
    }

    override fun onAdHidden(cloudXAd: CloudXAd) {
        onEvent?.invoke(FirstLookEvent.Closed(FirstLookSource.CLOUDX))
    }

    override fun onAdClicked(cloudXAd: CloudXAd) {
        onEvent?.invoke(FirstLookEvent.Clicked(FirstLookSource.CLOUDX))
    }

    override fun onAdRevenuePaid(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "CloudX interstitial revenue: ${cloudXAd.revenue}")
    }

    private companion object {
        const val TAG = "FirstLook"
    }
}
