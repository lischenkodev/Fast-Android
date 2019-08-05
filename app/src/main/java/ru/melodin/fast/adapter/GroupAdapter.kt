package ru.melodin.fast.adapter

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.picasso.Picasso
import ru.melodin.fast.R
import ru.melodin.fast.api.VKUtil
import ru.melodin.fast.api.model.VKGroup
import ru.melodin.fast.view.CircleImageView
import java.util.*

class GroupAdapter(context: Context, values: ArrayList<VKGroup>) :
    RecyclerAdapter<VKGroup, GroupAdapter.ViewHolder>(context, R.layout.item_group, values) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: CircleImageView = v.findViewById(R.id.userAvatar)
        private var name: TextView = v.findViewById(R.id.name)
        private var description: TextView = v.findViewById(R.id.description)

        override fun bind(position: Int) {
            val group = getItem(position)

            name.text = group.name

            val text = VKUtil.getGroupStringType(
                context,
                group.type
            ) + " • " + getString(R.string.members_count, group.membersCount)
            description.text = text

            if (!TextUtils.isEmpty(group.photo200)) {
                Picasso.get()
                    .load(group.photo200)
                    .priority(Picasso.Priority.HIGH)
                    .into(avatar)
            }
        }
    }
}
