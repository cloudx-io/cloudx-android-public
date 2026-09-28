package io.cloudx.demo.demoapp.ui.screens

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.cloudx.demo.demoapp.R
import io.cloudx.demo.demoapp.ui.log.setupLogListView

/**
 * Load/Show and Stop buttons, the log list and the ad slot for one banner-style sample from the
 * `ads` package. Load/Show creates the sample into the slot; Stop destroys it.
 */
abstract class AdViewFragment : Fragment(R.layout.fragment_ad_view) {

    private lateinit var loadShowButton: Button
    private lateinit var stopButton: Button
    private lateinit var adContainer: ViewGroup

    private var destroyAd: (() -> Unit)? = null

    /** Creates this screen's sample into [container] and returns the call that destroys it. */
    protected abstract fun createAd(container: ViewGroup): () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadShowButton = view.findViewById(R.id.btn_load_show)
        stopButton = view.findViewById(R.id.btn_stop)
        adContainer = view.findViewById(R.id.ad_container)

        loadShowButton.setOnClickListener {
            destroyAd = createAd(adContainer)
            showStopButton(true)
        }
        stopButton.setOnClickListener {
            removeAd()
            showStopButton(false)
        }

        setupLogListView(view.findViewById(R.id.log_list))
    }

    override fun onDestroyView() {
        removeAd()
        super.onDestroyView()
    }

    private fun removeAd() {
        destroyAd?.invoke()
        destroyAd = null
    }

    private fun showStopButton(show: Boolean) {
        loadShowButton.isVisible = !show
        stopButton.isVisible = show
    }
}
