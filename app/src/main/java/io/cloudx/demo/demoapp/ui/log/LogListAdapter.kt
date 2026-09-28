package io.cloudx.demo.demoapp.ui.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lists [DemoLog] entries with their time, coloured by level, and keeps the newest one in view.
 */
class LogListAdapter(recyclerView: RecyclerView) : RecyclerView.Adapter<LogListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.text_time)
        val msg: TextView = view.findViewById(R.id.text_msg)
    }

    private val entries = mutableListOf<DemoLog.Entry>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

    init {
        registerAdapterDataObserver(ScrollToBottomDataObserver(recyclerView))
    }

    fun addEntry(entry: DemoLog.Entry) {
        entries += entry
        notifyItemInserted(entries.size - 1)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.log_item, viewGroup, false))

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val entry = entries[position]
        viewHolder.time.text = timeFormat.format(Date(entry.timestamp))
        viewHolder.msg.text = entry.message
        viewHolder.msg.setTextColor(ContextCompat.getColor(viewHolder.msg.context, entry.level.colorRes()))
    }

    override fun getItemCount(): Int = entries.size
}

private fun DemoLog.Level.colorRes(): Int = when (this) {
    DemoLog.Level.INFO -> R.color.log_info
    DemoLog.Level.WARN -> R.color.log_warn
    DemoLog.Level.ERROR -> R.color.log_error
}

/**
 * Scrolls to a new entry when the list was already showing the bottom, so reading older entries is
 * not interrupted.
 */
private class ScrollToBottomDataObserver(
    private val recyclerView: RecyclerView
) : RecyclerView.AdapterDataObserver() {

    private val layoutManager = recyclerView.layoutManager as LinearLayoutManager

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()

        if (lastVisiblePosition == -1 || positionStart - lastVisiblePosition <= 2) {
            recyclerView.scrollToPosition(positionStart)
        }
    }
}
