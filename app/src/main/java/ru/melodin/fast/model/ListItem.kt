package ru.melodin.fast.model

import android.graphics.drawable.Drawable

class ListItem(var id: Int, var title: String, var icon: Drawable) : Model() {

    var isVisible = true

}
