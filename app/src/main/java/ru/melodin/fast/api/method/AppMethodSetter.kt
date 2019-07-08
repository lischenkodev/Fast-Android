package ru.melodin.fast.api.method

import ru.melodin.fast.util.ArrayUtil

class AppMethodSetter(name: String) : MethodSetter(name) {

    fun appId(id: Int): AppMethodSetter {
        put("app_id", id)
        return this
    }

    fun appIds(vararg ids: Int): AppMethodSetter {
        put("app_ids", ArrayUtil.toString(*ids))
        return this
    }

    override fun extended(value: Boolean): AppMethodSetter {
        put("extended", value)
        return this
    }

    fun returnFriends(friends: Boolean): AppMethodSetter {
        put("return_friends", friends)
        return this
    }

}