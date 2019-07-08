package ru.melodin.fast.database

import android.util.SparseArray
import ru.melodin.fast.api.model.VKGroup
import ru.melodin.fast.api.model.VKUser

object MemoryCache {
    private val users = SparseArray<VKUser>(20)
    private val groups = SparseArray<VKGroup>(20)

    fun getUser(id: Int): VKUser? {
        var user: VKUser? = users.get(id)
        if (user == null) {
            user = CacheStorage.getUser(id)
            if (user != null) {
                append(user)
            }
        }
        return user
    }

    fun getGroup(id: Int): VKGroup? {
        var group: VKGroup? = groups.get(id)
        if (group == null) {
            group = CacheStorage.getGroup(id)
            if (group != null) {
                append(group)
            }
        }
        return group
    }

    fun append(value: VKGroup) {
        groups.append(value.id, value)
    }

    fun append(value: VKUser) {
        users.append(value.id, value)
    }

    fun clear() {
        users.clear()
        groups.clear()
    }
}
