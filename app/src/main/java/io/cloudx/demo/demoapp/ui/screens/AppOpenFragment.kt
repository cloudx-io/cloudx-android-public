package io.cloudx.demo.demoapp.ui.screens

import android.content.Context
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.ads.AppOpenAd

class AppOpenFragment : FullscreenAdFragment() {

    override fun createAd(context: Context): AdControls =
        AppOpenAd(context, DemoConfig.APP_OPEN_AD_UNIT_ID).let { AdControls(it::load, it::show, it::destroy) }
}
