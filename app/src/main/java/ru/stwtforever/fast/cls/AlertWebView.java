package ru.stwtforever.fast.cls;

import android.annotation.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.webkit.*;

public class AlertWebView extends WebView {
	
    public AlertWebView(Context context) {
        super(context);
    }

    public AlertWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlertWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AlertWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
	}

    public AlertWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }
}
