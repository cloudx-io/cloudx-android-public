package io.cloudx.demo.demoapp.ui.screens

import android.view.ViewGroup
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.ads.MrecAd

class MrecFragment : AdViewFragment() {

    override fun createAd(container: ViewGroup): () -> Unit =
        MrecAd(container, DemoConfig.MREC_AD_UNIT_ID)::destroy
}
