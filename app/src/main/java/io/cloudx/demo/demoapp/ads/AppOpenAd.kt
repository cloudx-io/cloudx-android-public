package io.cloudx.demo.demoapp.ads

import android.app.Activity
import android.content.Context
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXAdRevenueListener
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXAppOpenAd
import io.cloudx.sdk.CloudXAppOpenListener

/**
 * A CloudX App Open ad: create it once, [load] it ahead of time, [show] it at a natural break, and
 * [destroy] it together with the screen that owns it.
 *
 * Copy this class as-is. [DemoLog] is its only dependency outside the CloudX SDK; replace it with
 * your own logging.
 */
class AppOpenAd(context: Context, adUnitId: String) : CloudXAppOpenListener, CloudXAdRevenueListener {

    private val ad: CloudXAppOpenAd = CloudX.createAppOpen(context, adUnitId).also {
        it.listener = this
        it.revenueListener = this
    }

    fun load() {
        ad.load()
    }

    fun show(activity: Activity) {
        if (ad.isAdReady) {
            // To tag the impression, use ad.show(activity, placement, customData) instead.
            ad.show(activity)
        } else {
            DemoLog.w(TAG, "App Open ad is not ready yet")
        }
    }

    fun destroy() {
        ad.destroy()
    }

    override fun onAdLoaded(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "App Open ad loaded from ${cloudXAd.networkName}")
    }

    override fun onAdLoadFailed(adUnitId: String, cloudXError: CloudXError) {
        DemoLog.e(TAG, "App Open ad failed to load: ${cloudXError.formattedMessage}")
    }

    override fun onAdDisplayed(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "App Open ad displayed")
    }

    override fun onAdDisplayFailed(cloudXAd: CloudXAd, cloudXError: CloudXError) {
        DemoLog.e(TAG, "App Open ad failed to display: ${cloudXError.formattedMessage}")
    }

    override fun onAdHidden(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "App Open ad hidden")
    }

    override fun onAdClicked(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "App Open ad clicked")
    }

    override fun onAdRevenuePaid(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "App Open revenue: ${cloudXAd.revenue} from ${cloudXAd.networkName}")
    }

    private companion object {
        const val TAG = "AppOpenAd"
    }
}
