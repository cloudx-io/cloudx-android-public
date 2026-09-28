package io.cloudx.demo.demoapp.ads

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXAdRevenueListener
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXNativeAdListener
import io.cloudx.sdk.CloudXNativeAdLoader
import io.cloudx.sdk.CloudXNativeAdView
import io.cloudx.sdk.CloudXNativeAdViewBinder
import kotlin.math.roundToInt

/**
 * A CloudX native ad rendered into your own layout and shown in [container]. Pick one loading flow:
 * [loadWithView] hands the SDK a view to fill, [loadThenRender] loads first and renders into a new
 * view once the ad arrives. Call [destroy] when the screen that owns it goes away.
 *
 * Copy this file with `res/layout/native_ad_layout.xml`. [DemoLog] is its only other dependency
 * outside the CloudX SDK; replace it with your own logging.
 */
class NativeAd(private val container: ViewGroup, adUnitId: String) :
    CloudXNativeAdListener, CloudXAdRevenueListener {

    private val context = container.context

    // Maps each asset to its view in native_ad_layout.xml. The options view is required: it holds
    // the network's AdChoices or privacy icon.
    private val binder = CloudXNativeAdViewBinder.Builder(R.layout.native_ad_layout)
        .setTitleTextViewId(R.id.native_ad_title)
        .setBodyTextViewId(R.id.native_ad_body)
        .setIconImageViewId(R.id.native_ad_icon)
        .setMediaContentViewGroupId(R.id.native_ad_media_container)
        .setCallToActionButtonId(R.id.native_ad_call_to_action)
        .setOptionsContentViewGroupId(R.id.native_ad_options)
        .setAdvertiserTextViewId(R.id.native_ad_advertiser)
        .setStarRatingContentViewGroupId(R.id.native_ad_star_rating)
        .build()

    private val loader: CloudXNativeAdLoader = CloudX.createNativeAdLoader(context, adUnitId).also {
        it.nativeAdListener = this
        it.revenueListener = this
    }

    private var currentAd: CloudXAd? = null

    fun loadWithView() {
        removeCurrentAd()
        loader.loadAd(CloudXNativeAdView(context, binder))
    }

    fun loadThenRender() {
        removeCurrentAd()
        loader.loadAd()
    }

    fun destroy() {
        removeCurrentAd()
        loader.destroy()
    }

    override fun onNativeAdLoaded(adView: CloudXNativeAdView?, ad: CloudXAd) {
        DemoLog.i(TAG, "Native ad loaded from ${ad.networkName}")
        currentAd = ad

        // loadWithView() gets its filled view back here; loadThenRender() gets null and renders
        // into a new view before adding it to the screen.
        val view = adView ?: CloudXNativeAdView(context, binder).also { loader.render(it, ad) }
        container.removeAllViews()
        container.addView(view)
        NativeMediaSizing.apply(view.mediaContentViewGroup, ad.nativeAd?.mediaContentAspectRatio ?: 0f)
    }

    override fun onNativeAdLoadFailed(adUnitId: String, error: CloudXError) {
        DemoLog.e(TAG, "Native ad failed to load: ${error.formattedMessage}")
    }

    override fun onNativeAdClicked(ad: CloudXAd) {
        DemoLog.i(TAG, "Native ad clicked")
    }

    override fun onNativeAdExpired(ad: CloudXAd) {
        // Loaded native ads expire one hour after load. Replace the expired ad with a fresh one.
        DemoLog.i(TAG, "Native ad expired, loading a new one")
        removeCurrentAd()
        loader.loadAd()
    }

    override fun onNativeAdClosed(ad: CloudXAd) {
        DemoLog.i(TAG, "Native ad dismissed by the user")
        removeCurrentAd()
    }

    override fun onAdRevenuePaid(cloudXAd: CloudXAd) {
        DemoLog.i(TAG, "Native revenue: ${cloudXAd.revenue} from ${cloudXAd.networkName}")
    }

    private fun removeCurrentAd() {
        currentAd?.let { loader.destroy(it) }
        currentAd = null
        container.removeAllViews()
    }

    private companion object {
        const val TAG = "NativeAd"
    }
}

/**
 * Sizes a native ad media container so a portrait creative renders at its true
 * shape instead of being letterboxed. Used by [NativeAd].
 *
 * For a portrait creative the container becomes a fixed, centered rectangle
 * computed once from a 320dp design width and the reported ratio, capped at
 * [MAX_HEIGHT_DP] dp tall. It is never pinned to the parent's width, so a wider
 * slot yields symmetric side margins rather than a stretched box. A 9:16
 * creative renders as a 225x400 dp box centered in the row.
 *
 * Landscape creatives keep the layout's default full-width box. They are the
 * common case and already fill the row acceptably; deriving their width from
 * the design width instead would shrink them and stop them scaling with the
 * screen. An unknown ratio (<= 0 — some adapters report 0 when the network
 * exposes none) takes the same default, so the container never collapses to
 * zero height and no ratio is guessed.
 *
 * Call after the ad loads (the ratio is unknown until then) and on every rebind
 * so a recycled row never keeps the previous ad's dimensions.
 */
private object NativeMediaSizing {

    private const val DESIGN_UNIT_WIDTH_DP = 320f
    private const val PADDING_DP = 8f
    private const val MAX_HEIGHT_DP = 400f
    private const val DEFAULT_HEIGHT_DP = 200f
    private const val PORTRAIT_RATIO_CEILING = 1f

    fun apply(container: View?, aspectRatio: Float) {
        val view = container ?: return
        val density = view.resources.displayMetrics.density

        val lp = view.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(density, DEFAULT_HEIGHT_DP),
            )

        if (aspectRatio <= 0f || aspectRatio >= PORTRAIT_RATIO_CEILING) {
            // Unknown or landscape ratio: keep the default full-width box.
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.height = dpToPx(density, DEFAULT_HEIGHT_DP)
            lp.gravity = Gravity.NO_GRAVITY
        } else {
            val availableWidthPx = dpToPx(density, DESIGN_UNIT_WIDTH_DP - 2 * PADDING_DP)
            val naturalHeightPx = availableWidthPx / aspectRatio
            val maxHeightPx = dpToPx(density, MAX_HEIGHT_DP)
            val heightPx = minOf(naturalHeightPx, maxHeightPx.toFloat())
            lp.width = (heightPx * aspectRatio).roundToInt()
            lp.height = heightPx.roundToInt()
            lp.gravity = Gravity.CENTER_HORIZONTAL
        }

        view.layoutParams = lp
    }

    private fun dpToPx(density: Float, dp: Float): Int = (dp * density).roundToInt()
}
