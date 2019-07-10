package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.collection.SparseArrayCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.melodin.fast.model.Model


abstract class RecyclerAdapter<T : Model, VH : RecyclerHolder> internal constructor(protected var context: Context, var viewRes: Int, values: ArrayList<T>) : RecyclerView.Adapter<RecyclerHolder>() {


    var values: ArrayList<T>? = null
        private set

    private var inflater: LayoutInflater
    private var cleanValues: ArrayList<T>? = null
    private var click: OnItemClickListener? = null
    private var longClick: OnItemLongClickListener? = null

    private val selectedItems = SparseArrayCompat<Model>()

    val isSelected: Boolean
        get() = selectedCount > 0

    val selectedMessages: ArrayList<T>
        get() {
            val items = ArrayList<T>()

            for (item in values!!)
                if (item.isSelected)
                    items.add(item)

            return items
        }

    open val selectedCount: Int
        get() = selectedItems.size()

    val isEmpty: Boolean
        get() = values == null || values!!.size == 0

    val lastPosition: Int
        get() = values!!.size - 1

    init {
        this.values = values

        this.inflater = LayoutInflater.from(context)
    }

    fun setSelected(position: Int, selected: Boolean) {
        val item = getItem(position)
        item.isSelected = selected

        if (selected) {
            selectedItems.append(position, item)
        } else {
            selectedItems.remove(position)
        }

        notifyItemChanged(position, -1)
    }


    fun clearSelected() {
        for (item in values!!) {
            item.isSelected = false
        }

        selectedItems.clear()
    }

    open fun toggleSelected(position: Int) {
        val item = getItem(position)

        val selected = !item.isSelected
        item.isSelected = selected

        if (selected) {
            selectedItems.append(position, item)
        } else {
            selectedItems.remove(position)
        }
    }


    @ColorInt
    protected fun getColor(resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {
        updateListeners(holder.itemView, position)
        holder.bind(position)
    }

    fun getView(parent: ViewGroup?): View? {
        return inflater.inflate(viewRes, parent, false)
    }

    override fun getItemCount(): Int {
        return if (values == null) 0 else values!!.size
    }

    fun insert(items: ArrayList<T>) {
        values!!.addAll(items)
    }

    open fun changeItems(items: ArrayList<T>) {
        this.values!!.clear()
        notifyDataSetChanged()
        this.values = ArrayList(items)
    }

    fun add(index: Int, item: T) {
        values!!.add(index, item)
    }

    fun add(item: T) {
        values!!.add(item)
    }

    fun remove(position: Int) {
        values!!.removeAt(position)
    }

    fun clear() {
        values!!.clear()
    }

    fun setOnItemClickListener(l: OnItemClickListener) {
        this.click = l
    }

    fun setOnItemLongClickListener(l: OnItemLongClickListener) {
        this.longClick = l
    }

    private fun updateListeners(v: View, position: Int) {
        if (click != null) {
            v.setOnClickListener { click!!.onItemClick(position) }
        }

        if (longClick != null) {
            v.setOnLongClickListener {
                longClick!!.onItemLongClick(position)
                click != null
            }
        }
    }

    open fun getItem(position: Int): T {
        return values!![position]
    }

    fun getString(res: Int): String {
        return context.getString(res)
    }

    fun getString(res: Int, vararg args: Any): String {
        return context.getString(res, *args)
    }

    protected fun getDrawable(res: Int): Drawable? {
        return ContextCompat.getDrawable(context, res)
    }

    open fun isSelected(position: Int): Boolean {
        return getItem(position).isSelected
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int)
    }

}