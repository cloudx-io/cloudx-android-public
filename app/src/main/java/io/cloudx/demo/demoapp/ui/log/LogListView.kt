package io.cloudx.demo.demoapp.ui.log

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.cloudx.demo.demoapp.DemoLog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Shows every [DemoLog] entry written while [recyclerView]'s screen is alive, newest at the bottom.
 */
fun setupLogListView(recyclerView: RecyclerView) {
    val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        ?: throw IllegalStateException("couldn't find view tree lifecycle owner")

    val adapter = LogListAdapter(recyclerView)
    recyclerView.adapter = adapter

    DemoLog.entries.onEach(adapter::addEntry).launchIn(lifecycleOwner.lifecycleScope)
}
