package ru.melodin.fast.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerHolder extends RecyclerView.ViewHolder {

    RecyclerHolder(@NonNull View v) {
        super(v);
    }

    public abstract void bind(int position);
}
