package io.cloudx.demo.demoapp.ui.screens

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import io.cloudx.demo.demoapp.R
import io.cloudx.demo.demoapp.ui.log.setupLogListView

/**
 * Load and Show buttons over the log list for one fullscreen sample from the `ads` package. The ad
 * lives as long as the fragment, so it survives view recreation.
 */
abstract class FullscreenAdFragment : Fragment(R.layout.fragment_fullscreen_ad) {

    /** The three calls the screen makes on its sample. */
    protected class AdControls(
        val load: () -> Unit,
        val show: (Activity) -> Unit,
        val destroy: () -> Unit,
    )

    private var ad: AdControls? = null

    /** Creates this screen's sample and returns its controls. */
    protected abstract fun createAd(context: Context): AdControls

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ad = createAd(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btn_load).setOnClickListener { ad?.load?.invoke() }
        view.findViewById<Button>(R.id.btn_show).setOnClickListener { ad?.show?.invoke(requireActivity()) }
        setupLogListView(view.findViewById(R.id.log_list))
    }

    override fun onDestroy() {
        ad?.destroy?.invoke()
        ad = null
        super.onDestroy()
    }
}
