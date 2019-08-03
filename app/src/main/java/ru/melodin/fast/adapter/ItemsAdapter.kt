package ru.melodin.fast.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ru.melodin.fast.R
import ru.melodin.fast.model.ListItem
import java.util.*

class ItemsAdapter(context: Context, values: ArrayList<ListItem>) :
    RecyclerAdapter<ListItem, ItemsAdapter.ViewHolder>(
        context,
        R.layout.fragment_items_item,
        values
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var icon: ImageView = v.findViewById(R.id.item_icon)
        private var title: TextView = v.findViewById(R.id.item_title)

        override fun bind(position: Int) {
            super.bind(position)

            val item = getItem(position)

            icon.setImageDrawable(item.icon)
            title.text = item.title
        }
    }
}
