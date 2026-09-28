package io.cloudx.demo.demoapp.ui.screens

import android.view.ViewGroup
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.ads.BannerAd

class BannerFragment : AdViewFragment() {

    override fun createAd(container: ViewGroup): () -> Unit =
        BannerAd(container, DemoConfig.BANNER_AD_UNIT_ID)::destroy
}
