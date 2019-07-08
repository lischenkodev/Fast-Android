package ru.melodin.fast.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

open class RecyclerHolder internal constructor(v: View) : RecyclerView.ViewHolder(v) {

    open fun bind(position: Int) {}
}
