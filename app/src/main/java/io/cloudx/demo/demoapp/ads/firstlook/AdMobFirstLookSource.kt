package io.cloudx.demo.demoapp.ads.firstlook

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import io.cloudx.demo.demoapp.DemoLog

/** The lazy AdMob fallback. A loaded ad can be shown once and expires after one hour. */
class AdMobFirstLookSource(activity: Activity, private val adUnitId: String) : FirstLookInterstitialSource {

    private val appContext = activity.applicationContext
    private var activity: Activity? = activity
    private var ad: InterstitialAd? = null
    private var loadedAtMs = 0L
    private var loading = false
    private var disposed = false

    override var onEvent: ((FirstLookEvent) -> Unit)? = null

    override val isReady: Boolean
        get() {
            if (disposed) return false
            if (ad != null && SystemClock.elapsedRealtime() - loadedAtMs >= AD_TTL_MS) {
                ad = null
            }
            return ad != null
        }

    override fun load() {
        if (disposed || loading || isReady) return
        loading = true
        DemoLog.i(TAG, "Loading AdMob interstitial fallback")
        try {
            InterstitialAd.load(
                appContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(loadedAd: InterstitialAd) {
                        loading = false
                        if (disposed) return
                        ad = loadedAd
                        loadedAtMs = SystemClock.elapsedRealtime()
                        loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                if (!disposed) onEvent?.invoke(FirstLookEvent.Shown(FirstLookSource.ADMOB))
                            }

                            override fun onAdDismissedFullScreenContent() {
                                if (!disposed) onEvent?.invoke(FirstLookEvent.Closed(FirstLookSource.ADMOB))
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                if (!disposed) {
                                    onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.ADMOB, error.message))
                                }
                            }

                            override fun onAdClicked() {
                                if (!disposed) onEvent?.invoke(FirstLookEvent.Clicked(FirstLookSource.ADMOB))
                            }
                        }
                        onEvent?.invoke(FirstLookEvent.Loaded(FirstLookSource.ADMOB))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading = false
                        if (!disposed) {
                            onEvent?.invoke(FirstLookEvent.LoadFailed(FirstLookSource.ADMOB, error.message))
                        }
                    }
                },
            )
        } catch (error: Exception) {
            loading = false
            onEvent?.invoke(FirstLookEvent.LoadFailed(FirstLookSource.ADMOB, error.message ?: "Load failed"))
        }
    }

    override fun show() {
        val loadedAd = if (isReady) ad else null
        val host = activity
        if (loadedAd == null || host == null) {
            onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.ADMOB, "Ad is no longer ready"))
            return
        }
        ad = null
        try {
            loadedAd.show(host)
        } catch (error: Exception) {
            onEvent?.invoke(FirstLookEvent.ShowFailed(FirstLookSource.ADMOB, error.message ?: "Show failed"))
        }
    }

    override fun dispose() {
        disposed = true
        activity = null
        ad?.fullScreenContentCallback = null
        ad = null
        onEvent = null
    }

    private companion object {
        const val TAG = "FirstLook"
        const val AD_TTL_MS = 60 * 60 * 1000L
    }
}
