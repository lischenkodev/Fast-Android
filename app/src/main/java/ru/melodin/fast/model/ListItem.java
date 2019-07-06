package ru.melodin.fast.model;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class ListItem {

    @NonNull
    private String title = "";

    @NonNull
    private Drawable icon = new ColorDrawable(Color.TRANSPARENT);

    private int id = -1;
    private boolean visible = true;

    public ListItem(int id, @NonNull String title, @NonNull Drawable icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    public ListItem() {
    }

    public boolean isVisible() {
        return visible;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(@NonNull Drawable icon) {
        this.icon = icon;
    }
}
