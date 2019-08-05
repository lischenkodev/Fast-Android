package ru.melodin.fast.model

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

open class ListItem(var type: Int, var id: Int, var title: String, var icon: Drawable) : Model() {

    var isVisible = true

    constructor(title: String, icon: Drawable) : this(TYPE_ITEM, -1, title, icon)

    constructor(id: Int, title: String, icon: Drawable) : this(TYPE_ITEM, id, title, icon)

    constructor(type: Int) : this(
        type, -1, "", ColorDrawable()
    )

    constructor(divider: Boolean) : this(
        if (divider) TYPE_DIVIDER else TYPE_ITEM,
        -1,
        "",
        ColorDrawable()
    )

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_DIVIDER = 1
        const val TYPE_SHADOW_PADDING = 2
    }
}
