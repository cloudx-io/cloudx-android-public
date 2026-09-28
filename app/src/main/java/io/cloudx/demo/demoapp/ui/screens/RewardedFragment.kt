package io.cloudx.demo.demoapp.ui.screens

import android.content.Context
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.ads.RewardedAd

class RewardedFragment : FullscreenAdFragment() {

    override fun createAd(context: Context): AdControls =
        RewardedAd(context, DemoConfig.REWARDED_AD_UNIT_ID).let { AdControls(it::load, it::show, it::destroy) }
}
