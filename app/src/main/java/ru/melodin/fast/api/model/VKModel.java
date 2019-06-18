package ru.melodin.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

import ru.melodin.fast.common.AppGlobal;

public abstract class VKModel implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Object tag;
    protected boolean selected;

    protected VKModel() {
    }

    protected VKModel(JSONObject source) {
    }

    protected static String getString(int res) {
        return AppGlobal.getContext().getString(res);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }
}

