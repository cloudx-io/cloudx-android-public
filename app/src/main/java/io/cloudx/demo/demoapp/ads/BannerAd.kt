package io.cloudx.demo.demoapp.ads

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXAdRevenueListener
import io.cloudx.sdk.CloudXAdView
import io.cloudx.sdk.CloudXAdViewListener
import io.cloudx.sdk.CloudXError

/**
 * A CloudX 320x50 Banner. Creating it adds the ad view to [container]; from then on it loads and
 * refreshes on its own. Call [destroy] when the screen that owns it goes away.
 *
 * Copy this class as-is. [DemoLog] is its only dependency outside the CloudX SDK; replace it with
 * your own logging.
 */
class BannerAd(private val container: ViewGroup, adUnitId: String) : CloudXAdViewListener, CloudXAdRevenueListener {

    private val adView: CloudXAdView = CloudX.createBanner(container.context, adUnitId).also {
        it.listener = this
        it.revenueListener = this
        // To tag impressions, call it.setPlacement(...) and it.setCustomData(...) here.
        container.addView(it, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    }

    fun destroy() {
        container.removeView(adView)
        adView.destroy()
    }

    override fun onAdLoaded(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Banner loaded from ${cloudXAd.networkName}")
    }

    override fun onAdLoadFailed(adUnitId: String, cloudXError: CloudXError) {
        DemoLog.e(TAG, "Banner failed to load: ${cloudXError.formattedMessage}")
    }

    override fun onAdClicked(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Banner clicked")
    }

    override fun onAdExpanded(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Banner expanded")
    }

    override fun onAdCollapsed(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Banner collapsed")
    }

    override fun onAdRevenuePaid(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Banner revenue: ${cloudXAd.revenue} from ${cloudXAd.networkName}")
    }

    private companion object {
        const val TAG = "BannerAd"
    }
}
