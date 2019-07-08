package ru.melodin.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

import ru.melodin.fast.model.Model;

public abstract class VKModel extends Model implements Serializable {
    private static final long serialVersionUID = 1L;

    public VKModel() {
    }

    public VKModel(JSONObject source) {
    }
}

