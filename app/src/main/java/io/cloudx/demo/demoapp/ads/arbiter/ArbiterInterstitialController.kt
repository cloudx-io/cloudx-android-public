package io.cloudx.demo.demoapp.ads.arbiter

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXArbiterBid
import io.cloudx.sdk.CloudXArbiterConfiguration
import io.cloudx.sdk.CloudXArbiterListener
import io.cloudx.sdk.CloudXArbiterPlatform
import io.cloudx.sdk.CloudXArbiterResult
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInterstitialAd
import io.cloudx.sdk.CloudXInterstitialListener
import io.cloudx.sdk.CloudXRevenueData
import io.cloudx.sdk.CloudXRevenuePlatform
import io.cloudx.sdk.CloudXRevenuePrecision

/** Events from the arbiter flow. Each one names the platform it came from. */
sealed interface ArbiterEvent {
    data class Loaded(val platform: CloudXArbiterPlatform) : ArbiterEvent
    data class LoadFailed(val platform: CloudXArbiterPlatform, val message: String) : ArbiterEvent

    /** A round finished. [platform] is [CloudXArbiterPlatform.NONE] when no winner was selected. */
    data class ArbiterCompleted(val platform: CloudXArbiterPlatform, val bidCount: Int) : ArbiterEvent

    /** Both platforms settled without a fill, so there is nothing to arbitrate. */
    data object NoCandidates : ArbiterEvent

    data class Shown(val platform: CloudXArbiterPlatform) : ArbiterEvent
    data class ShowFailed(val platform: CloudXArbiterPlatform, val message: String) : ArbiterEvent
    data class Closed(val platform: CloudXArbiterPlatform) : ArbiterEvent
    data class Clicked(val platform: CloudXArbiterPlatform) : ArbiterEvent

    /**
     * An AdMob paid event forwarded to CloudX, with what [CloudX.reportRevenueData] returned. It is
     * the call's result, not proof that the price was kept: CloudX does not keep a revenue of 0 as
     * the AdMob bid's price, and 0 is what Google's test ad units pay.
     */
    data class RevenueReported(val revenue: Double, val currencyCode: String, val accepted: Boolean) : ArbiterEvent
}

/**
 * Trusted Arbiter for interstitials. CloudX and AdMob load in parallel, the ones that fill become
 * bids, and [CloudX.arbiter] picks the platform to show.
 *
 * The arbiter runs as soon as both platforms have settled (loaded or failed) and the winner is
 * stored, so [show] makes no arbiter or network call. It returns null when no winner is prepared,
 * and the app carries on without an ad. Call [load] after an ad closes to start the next round; it
 * reloads only the platform that has no fill.
 *
 * AdMob bids carry no price. CloudX prices them from the revenue this controller forwards after
 * every AdMob impression through [CloudX.reportRevenueData], so that forwarding is a required part
 * of the integration, not analytics.
 *
 * Pass false for [cloudXAvailable] when CloudX initialization failed or did not answer. AdMob is then
 * the only candidate and wins each round here, without a call into an SDK that is not initialized.
 */
class ArbiterInterstitialController(
    activity: Activity,
    cloudXAdUnitId: String,
    private val adMobAdUnitId: String,
    cloudXAvailable: Boolean,
    private val onEvent: (ArbiterEvent) -> Unit,
) {
    private val appContext = activity.applicationContext
    private var activity: Activity? = activity

    private var loadedCloudXAd: CloudXAd? = null
    private var cloudXLoading = false
    private var cloudXSettled = !cloudXAvailable

    private var adMobAd: InterstitialAd? = null
    private var adMobLoadedAtMs = 0L
    private var adMobLoading = false
    private var adMobSettled = false

    private var nextWinner: CloudXArbiterPlatform? = null
    private var arbiterInFlight = false
    private var showing = false
    private var disposed = false

    private val cloudXListener = object : CloudXInterstitialListener {
        override fun onAdLoaded(cloudXAd: CloudXAd) {
            if (disposed) return
            cloudXLoading = false
            loadedCloudXAd = cloudXAd
            cloudXSettled = true
            DemoLog.i(TAG, "CloudX bid from ${cloudXAd.networkName}, revenue ${cloudXAd.revenue}")
            onEvent(ArbiterEvent.Loaded(CloudXArbiterPlatform.CLOUDX))
            maybePrepareWinner()
        }

        override fun onAdLoadFailed(adUnitId: String, cloudXError: CloudXError) {
            if (disposed) return
            cloudXLoading = false
            loadedCloudXAd = null
            cloudXSettled = true
            onEvent(ArbiterEvent.LoadFailed(CloudXArbiterPlatform.CLOUDX, cloudXError.formattedMessage))
            maybePrepareWinner()
        }

        override fun onAdDisplayed(cloudXAd: CloudXAd) {
            if (!disposed) onEvent(ArbiterEvent.Shown(CloudXArbiterPlatform.CLOUDX))
        }

        override fun onAdDisplayFailed(cloudXAd: CloudXAd, cloudXError: CloudXError) {
            if (disposed) return
            loadedCloudXAd = null
            showing = false
            onEvent(ArbiterEvent.ShowFailed(CloudXArbiterPlatform.CLOUDX, cloudXError.formattedMessage))
        }

        override fun onAdHidden(cloudXAd: CloudXAd) {
            if (disposed) return
            loadedCloudXAd = null
            showing = false
            onEvent(ArbiterEvent.Closed(CloudXArbiterPlatform.CLOUDX))
        }

        override fun onAdClicked(cloudXAd: CloudXAd) {
            if (!disposed) onEvent(ArbiterEvent.Clicked(CloudXArbiterPlatform.CLOUDX))
        }
    }

    /*
     * One CloudX ad object for the controller's lifetime: a load after the ad closes runs a new
     * auction on it. Null when CloudX is not available.
     */
    private val cloudXInterstitial: CloudXInterstitialAd? = if (cloudXAvailable) {
        CloudX.createInterstitial(appContext, cloudXAdUnitId).also { it.listener = cloudXListener }
    } else {
        null
    }

    private val adMobCallback = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            if (!disposed) onEvent(ArbiterEvent.Shown(CloudXArbiterPlatform.ADMOB))
        }

        override fun onAdDismissedFullScreenContent() {
            adMobAd = null
            showing = false
            if (!disposed) onEvent(ArbiterEvent.Closed(CloudXArbiterPlatform.ADMOB))
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            adMobAd = null
            showing = false
            if (!disposed) onEvent(ArbiterEvent.ShowFailed(CloudXArbiterPlatform.ADMOB, error.message))
        }

        override fun onAdClicked() {
            if (!disposed) onEvent(ArbiterEvent.Clicked(CloudXArbiterPlatform.ADMOB))
        }
    }

    /** The platform [show] would use now, or null when no winner is prepared. */
    val preparedWinner: CloudXArbiterPlatform?
        get() = nextWinner

    /** True from a show call until that ad closes or fails to show. */
    val isShowing: Boolean
        get() = showing

    /** True while a load or an arbiter round is in flight, so another event is still coming. */
    val isBusy: Boolean
        get() = cloudXLoading || adMobLoading || arbiterInFlight

    private val cloudXReady: Boolean
        get() = loadedCloudXAd != null && cloudXInterstitial?.isAdReady == true

    /* A loaded AdMob interstitial can be shown once and expires after one hour. */
    private val adMobReady: Boolean
        get() {
            if (adMobAd != null && SystemClock.elapsedRealtime() - adMobLoadedAtMs >= ADMOB_AD_TTL_MS) {
                adMobAd = null
            }
            return adMobAd != null
        }

    /**
     * Loads each platform that holds no fill, then runs a new round once both have settled. Does
     * nothing while a round is in flight or an ad is showing.
     */
    fun load() {
        if (disposed || arbiterInFlight || showing) return

        val cloudX = cloudXInterstitial
        if (cloudX != null && !cloudXReady && !cloudXLoading) {
            loadedCloudXAd = null
            cloudXSettled = false
            cloudXLoading = true
            cloudX.load()
        }

        if (!adMobReady && !adMobLoading) {
            adMobSettled = false
            adMobLoading = true
            loadAdMob()
        }

        /*
         * Both platforms may still hold their fills with no winner stored, after a round with no
         * winner or a winner that went stale. Nothing loads then, so the round runs from here.
         */
        if (nextWinner == null) maybePrepareWinner()
    }

    /**
     * Shows the stored winner and returns its platform. Returns null when there is nothing to show:
     * no winner is prepared, an ad is already showing (check [isShowing]), or the winner's ad
     * expired, in which case [load] reloads that platform and runs a new round.
     */
    fun show(): CloudXArbiterPlatform? {
        val host = activity
        val winner = nextWinner
        if (disposed || showing || host == null || winner == null) return null
        nextWinner = null

        when (winner) {
            CloudXArbiterPlatform.CLOUDX -> {
                val cloudX = cloudXInterstitial
                if (cloudX == null || !cloudXReady) {
                    loadedCloudXAd = null
                    return null
                }
                showing = true
                cloudX.show(host)
            }
            CloudXArbiterPlatform.ADMOB -> {
                val ad = adMobAd
                if (ad == null || !adMobReady) return null
                showing = true
                ad.show(host)
            }
            else -> return null
        }
        return winner
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        activity = null
        cloudXInterstitial?.listener = null
        cloudXInterstitial?.destroy()
        adMobAd?.fullScreenContentCallback = null
        adMobAd?.onPaidEventListener = null
        adMobAd = null
    }

    /*
     * Runs one round once both platforms have settled and stores the winner. The SDK owns the
     * timeout and the fallback, and always completes: a single bid wins without a service call, and
     * several bids go to the arbiter service or, when it is unavailable, to the highest locally
     * comparable price. So nothing here times the call out or compares prices.
     */
    private fun maybePrepareWinner() {
        if (disposed || showing || arbiterInFlight || !cloudXSettled || !adMobSettled) return

        val bids = buildList {
            val cloudXAd = loadedCloudXAd
            if (cloudXAd != null && cloudXReady) add(CloudXArbiterBid.cloudX(cloudXAd))
            val ad = adMobAd
            if (ad != null && adMobReady) {
                add(CloudXArbiterBid.adMob(adUnitId = adMobAdUnitId, networkName = adSourceName(ad) ?: "admob"))
            }
        }
        if (bids.isEmpty()) {
            onEvent(ArbiterEvent.NoCandidates)
            return
        }

        if (cloudXInterstitial == null) {
            DemoLog.i(TAG, "CloudX is not initialized, so the AdMob bid wins without an arbiter call")
            storeWinner(CloudXArbiterPlatform.ADMOB, bids.size)
            return
        }

        arbiterInFlight = true
        DemoLog.i(TAG, "Running the arbiter with ${bids.size} bid(s)")
        CloudX.arbiter(
            CloudXArbiterConfiguration.builder(bids).build(),
            object : CloudXArbiterListener {
                override fun onCompleted(result: CloudXArbiterResult) {
                    arbiterInFlight = false
                    if (disposed) return
                    DemoLog.i(
                        TAG,
                        "Arbiter result: platform=${result.platform} platformName=${result.platformName} " +
                            "id=${result.id} bidId=${result.bidId ?: "-"} bids=${bids.size}",
                    )
                    storeWinner(result.platform, bids.size)
                }
            },
        )
    }

    /* NONE is not stored: a stored NONE would leave both fills held and no round left to run. */
    private fun storeWinner(platform: CloudXArbiterPlatform, bidCount: Int) {
        nextWinner = platform.takeUnless { it == CloudXArbiterPlatform.NONE }
        onEvent(ArbiterEvent.ArbiterCompleted(platform, bidCount))
    }

    private fun loadAdMob() {
        InterstitialAd.load(
            appContext,
            adMobAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    adMobLoading = false
                    if (disposed) return
                    ad.fullScreenContentCallback = adMobCallback
                    ad.setOnPaidEventListener { adValue -> reportAdMobPaidEvent(ad, adValue) }
                    adMobAd = ad
                    adMobLoadedAtMs = SystemClock.elapsedRealtime()
                    adMobSettled = true
                    onEvent(ArbiterEvent.Loaded(CloudXArbiterPlatform.ADMOB))
                    maybePrepareWinner()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adMobLoading = false
                    if (disposed) return
                    adMobSettled = true
                    onEvent(ArbiterEvent.LoadFailed(CloudXArbiterPlatform.ADMOB, error.message))
                    maybePrepareWinner()
                }
            },
        )
    }

    /*
     * Required: this is how CloudX learns what an AdMob bid is worth. Google reports the value in
     * micros of the currency, so it is divided by 1,000,000 for the revenue of one impression.
     */
    private fun reportAdMobPaidEvent(ad: InterstitialAd, adValue: AdValue) {
        val servedBy = ad.responseInfo.loadedAdapterResponseInfo
        val revenue = adValue.valueMicros / 1_000_000.0
        val accepted = CloudX.reportRevenueData(
            CloudXRevenueData.builder(CloudXRevenuePlatform.ADMOB, revenue, AD_FORMAT)
                .currencyCode(adValue.currencyCode)
                .precision(adValue.precisionType.toCloudXRevenuePrecision())
                .networkName(servedBy?.adSourceName)
                .adUnitId(adMobAdUnitId)
                .thirdPartyAdPlacementId(servedBy?.adSourceInstanceName)
                .build(),
        )
        DemoLog.i(TAG, "reportRevenueData(${adValue.valueMicros} micros ${adValue.currencyCode}) returned $accepted")
        if (!disposed) onEvent(ArbiterEvent.RevenueReported(revenue, adValue.currencyCode, accepted))
    }

    private fun adSourceName(ad: InterstitialAd): String? = ad.responseInfo.loadedAdapterResponseInfo?.adSourceName

    private fun Int.toCloudXRevenuePrecision(): CloudXRevenuePrecision = when (this) {
        AdValue.PrecisionType.PRECISE -> CloudXRevenuePrecision.EXACT
        AdValue.PrecisionType.ESTIMATED -> CloudXRevenuePrecision.ESTIMATED
        AdValue.PrecisionType.PUBLISHER_PROVIDED -> CloudXRevenuePrecision.PUBLISHER_DEFINED
        else -> CloudXRevenuePrecision.UNDEFINED
    }

    private companion object {
        const val TAG = "Arbiter"
        const val AD_FORMAT = "interstitial"
        const val ADMOB_AD_TTL_MS = 60 * 60 * 1000L
    }
}
