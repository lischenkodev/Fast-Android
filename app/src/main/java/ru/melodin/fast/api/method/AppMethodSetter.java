package ru.melodin.fast.api.method;

/**
 * Method setter for apps
 */

import ru.melodin.fast.util.ArrayUtil;

public class AppMethodSetter extends MethodSetter {

    public AppMethodSetter(String name) {
        super(name);
    }

    public AppMethodSetter appId(int id) {
        put("app_id", id);
        return this;
    }

    public AppMethodSetter appIds(int... ids) {
        put("app_ids", ArrayUtil.toString(ids));
        return this;
    }

    public AppMethodSetter extended(boolean value) {
        put("extended", value);
        return this;
    }

    public AppMethodSetter returnFriends(boolean friends) {
        put("return_friends", friends);
        return this;
    }

}
