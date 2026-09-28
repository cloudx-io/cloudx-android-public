package io.cloudx.demo.demoapp.ui.screens

import android.content.Context
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.ads.InterstitialAd

class InterstitialFragment : FullscreenAdFragment() {

    override fun createAd(context: Context): AdControls =
        InterstitialAd(context, DemoConfig.INTERSTITIAL_AD_UNIT_ID).let { AdControls(it::load, it::show, it::destroy) }
}
