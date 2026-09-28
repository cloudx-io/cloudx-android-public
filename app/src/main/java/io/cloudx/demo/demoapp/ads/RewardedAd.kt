package io.cloudx.demo.demoapp.ads

import android.app.Activity
import android.content.Context
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXAdRevenueListener
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXReward
import io.cloudx.sdk.CloudXRewardedAd
import io.cloudx.sdk.CloudXRewardedListener

/**
 * A CloudX Rewarded ad: create it once, [load] it ahead of time, [show] it at a natural break, and
 * [destroy] it together with the screen that owns it.
 *
 * Copy this class as-is. [DemoLog] is its only dependency outside the CloudX SDK; replace it with
 * your own logging.
 */
class RewardedAd(context: Context, adUnitId: String) : CloudXRewardedListener, CloudXAdRevenueListener {

    private val ad: CloudXRewardedAd = CloudX.createRewarded(context, adUnitId).also {
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
            DemoLog.w(TAG, "Rewarded ad is not ready yet")
        }
    }

    fun destroy() {
        ad.destroy()
    }

    override fun onAdLoaded(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Rewarded ad loaded from ${cloudXAd.networkName}")
    }

    override fun onAdLoadFailed(adUnitId: String, cloudXError: CloudXError) {
        DemoLog.e(TAG, "Rewarded ad failed to load: ${cloudXError.formattedMessage}")
    }

    override fun onAdDisplayed(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Rewarded ad displayed")
    }

    override fun onAdDisplayFailed(cloudXAd: CloudXAd, cloudXError: CloudXError) {
        DemoLog.e(TAG, "Rewarded ad failed to display: ${cloudXError.formattedMessage}")
    }

    override fun onAdHidden(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Rewarded ad hidden")
    }

    override fun onAdClicked(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Rewarded ad clicked")
    }

    override fun onUserRewarded(cloudXAd: CloudXAd, reward: CloudXReward) {
        DemoLog.i(TAG, "User rewarded: ${reward.amount} ${reward.label}")
    }

    override fun onAdRevenuePaid(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Rewarded revenue: ${cloudXAd.revenue} from ${cloudXAd.networkName}")
    }

    private companion object {
        const val TAG = "RewardedAd"
    }
}
