package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ru.melodin.fast.R
import ru.melodin.fast.model.ListItem
import java.util.*

class ItemAdapter(context: Context, values: ArrayList<ListItem>) :
    RecyclerAdapter<ListItem, ItemAdapter.ViewHolder>(
        context,
        R.layout.fragment_items_item,
        values
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return when (viewType) {
            ListItem.TYPE_ITEM -> ViewHolder(getView(parent)!!)
            ListItem.TYPE_DIVIDER -> Holder(inflater.inflate(R.layout.view_divider, parent, false))
            ListItem.TYPE_SHADOW_PADDING -> Holder(inflater.inflate(R.layout.view_shadow_padding, parent, false))
            else -> Holder(View(context))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    inner class Holder(v: View) : RecyclerHolder(v)

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var icon: ImageView = v.findViewById(R.id.item_icon)
        private var title: TextView = v.findViewById(R.id.item_title)

        private var color = -1

        init {
            color = title.textColors.defaultColor
        }

        override fun bind(position: Int) {
            super.bind(position)

            val item = getItem(position)

            icon.setImageDrawable(item.icon)
            title.text = item.title

            if (item.id == 255) {
                icon.drawable.setTint(Color.RED)
                title.setTextColor(Color.RED)
            } else {
                icon.drawable.setTint(color)
                title.setTextColor(color)
            }
        }
    }
}
