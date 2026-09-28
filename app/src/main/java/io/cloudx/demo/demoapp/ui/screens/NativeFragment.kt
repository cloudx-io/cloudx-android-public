package io.cloudx.demo.demoapp.ui.screens

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import io.cloudx.demo.demoapp.DemoConfig
import io.cloudx.demo.demoapp.R
import io.cloudx.demo.demoapp.ads.NativeAd
import io.cloudx.demo.demoapp.ui.log.setupLogListView

/**
 * Buttons for both loading flows of [NativeAd], the ad slot and the log list. Destroy releases the
 * sample; the next load creates a new one.
 */
class NativeFragment : Fragment(R.layout.fragment_native) {

    private lateinit var adContainer: ViewGroup
    private var ad: NativeAd? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adContainer = view.findViewById(R.id.native_ad_container)

        view.findViewById<Button>(R.id.btn_load_with_view).setOnClickListener { nativeAd().loadWithView() }
        view.findViewById<Button>(R.id.btn_load_then_render).setOnClickListener { nativeAd().loadThenRender() }
        view.findViewById<Button>(R.id.btn_destroy).setOnClickListener { destroyAd() }

        setupLogListView(view.findViewById(R.id.log_list))
    }

    override fun onDestroyView() {
        destroyAd()
        super.onDestroyView()
    }

    private fun nativeAd(): NativeAd =
        ad ?: NativeAd(adContainer, DemoConfig.NATIVE_AD_UNIT_ID).also { ad = it }

    private fun destroyAd() {
        ad?.destroy()
        ad = null
    }
}
