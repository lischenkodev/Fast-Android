package ru.melodin.fast.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ru.melodin.fast.R
import ru.melodin.fast.model.ListItem
import java.util.*

class PopupAdapter(context: Context, values: ArrayList<ListItem>) : RecyclerAdapter<ListItem, PopupAdapter.ViewHolder>(context, R.layout.activity_messages_popup_item, values) {

    override fun changeItems(items: ArrayList<ListItem>) {
        super.changeItems(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        var icon: ImageView = v.findViewById(R.id.icon)
        var title: TextView = v.findViewById(R.id.title)

        override fun bind(position: Int) {
            val item = getItem(position)

            icon.setImageDrawable(item.icon)
            title.text = item.title
        }
    }

    companion object {
        const val ID_CLEAR_DIALOG = 0
        const val ID_NOTIFICATIONS = 1
        const val ID_LEAVE = 2
        const val ID_CHAT_INFO = 3
    }
}