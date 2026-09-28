package io.cloudx.demo.demoapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.cloudx.demo.demoapp.R

/**
 * Icon, title and optional subtitle shown by one [MenuRowAdapter] row.
 */
data class MenuRow(
    val title: String,
    val subtitle: String?,
    @DrawableRes val iconRes: Int,
)

/**
 * Adapter for the demo's list menu ([MoreFragment]). [toRow] maps each of
 * [items] to the row it shows, and [onClick] receives the item whose row was tapped.
 */
class MenuRowAdapter<T>(
    private val items: List<T>,
    private val toRow: (T) -> MenuRow,
    private val onClick: (T) -> Unit,
) : RecyclerView.Adapter<MenuRowAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.menu_icon)
        val title: TextView = view.findViewById(R.id.menu_title)
        val subtitle: TextView = view.findViewById(R.id.menu_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val row = toRow(item)
        holder.title.text = row.title
        holder.subtitle.text = row.subtitle
        holder.subtitle.isVisible = row.subtitle != null
        holder.icon.setImageResource(row.iconRes)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
