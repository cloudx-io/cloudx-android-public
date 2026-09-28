package io.cloudx.demo.demoapp.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R

/**
 * Root screen of the More tab: lists every [MoreDestination] that did not fit in the bottom bar.
 * [MainActivity.openMoreDestination] opens the tapped screen on top of this list, so Back returns
 * here.
 */
class MoreFragment : Fragment(R.layout.fragment_more) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.more_list)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recycler.adapter = MenuRowAdapter(
            items = MoreDestination.entries,
            toRow = { MenuRow(title = getString(it.titleRes), subtitle = null, iconRes = it.iconRes) },
        ) { destination ->
            DemoLog.i(TAG, "Selected: $destination")
            (requireActivity() as MainActivity).openMoreDestination(destination)
        }
    }

    companion object {
        private const val TAG = "MoreFragment"
    }
}
