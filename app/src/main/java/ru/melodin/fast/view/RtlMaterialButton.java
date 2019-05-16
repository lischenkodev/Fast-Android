package ru.melodin.fast.view;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

public class RtlMaterialButton extends MaterialButton {
    public RtlMaterialButton(Context context) {
        super(context);
    }

    public RtlMaterialButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RtlMaterialButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateIcon();
    }

    private void updateIcon() {
        TextViewCompat.setCompoundDrawablesRelative(this, null, null, getIcon(), null);
    }
}
