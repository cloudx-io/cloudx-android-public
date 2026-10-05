package io.cloudx.demo.demoapp

import android.content.Intent

/**
 * The CloudX app key and ad unit IDs of the dashboard app made for the Android sample app
 * (package io.cloudx.sample), the same IDs the public Unity sample uses on Android.
 */
object DemoConfig {
    const val APP_KEY = "0qE4q2MoJzoOkFQQKAtkt"

    const val BANNER_AD_UNIT_ID = "guDml31r4Ys6O6HroPJia"
    const val MREC_AD_UNIT_ID = "TL6HTNWj7kkRUcodwGKSY"
    const val INTERSTITIAL_AD_UNIT_ID = "PwIOPhOD0KMCB_aqz8c89"
    const val ADMOB_INTERSTITIAL_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val APP_OPEN_AD_UNIT_ID = "BI0Whd5_o8ZIxkdHBS7X_"
    const val REWARDED_AD_UNIT_ID = "LZrqb2oz47LMG_TaaVtaR"
    const val NATIVE_AD_UNIT_ID = "K1dZFx-3JyCB4rufiYB5C"

    /**
     * String intent extra on the launch intent that gives every Arbiter/TPA AdMob bid a manual
     * price, in USD per impression, for one launch. QA sets it to run a round that compares prices:
     * Google's test unit reports 0, so without it the AdMob bid never has a price. A testing aid
     * only; leave it unset in a production app.
     */
    const val EXTRA_ADMOB_MANUAL_REVENUE_PER_IMPRESSION_USD = "DemoApp.AdMobManualRevenuePerImpressionUSD"

    /**
     * The manual AdMob price from [EXTRA_ADMOB_MANUAL_REVENUE_PER_IMPRESSION_USD] on [intent], or
     * null when it is unset or not a finite number of at least 0.
     */
    fun adMobManualRevenuePerImpressionUSD(intent: Intent): Double? =
        intent.getStringExtra(EXTRA_ADMOB_MANUAL_REVENUE_PER_IMPRESSION_USD)
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
}
