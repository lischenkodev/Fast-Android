package ru.melodin.fast.common

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.annotations.Contract
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.method.MessageMethodSetter
import ru.melodin.fast.api.method.MethodSetter
import ru.melodin.fast.api.model.*
import ru.melodin.fast.concurrent.LowThread
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import java.util.*

class TaskManager {

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    fun onReceive(data: Array<Any>) {
        val key = data[0] as String
        when (key) {
            Keys.NEED_LOAD_ID -> {
                val id = data[1] as Int
                if (VKConversation.isChatId(id)) {
                    loadConversation(id, false, null)
                } else if (id < 0) {
                    loadGroup(id * -1, null)
                } else {
                    loadUser(id, null)
                }
            }
        }
    }

    interface OnCompleteListener {
        fun onComplete(models: ArrayList<*>?)

        fun onError(e: Exception)
    }

    private class Task internal constructor(internal val setter: MethodSetter, internal val listener: OnCompleteListener?)

    companion object {

        @get:Contract(pure = true)
        var instance: TaskManager? = null
            private set

        private val tasks = ArrayList<Task>()
        private val loadingIds = ArrayList<Int>()

        internal fun init() {
            if (instance == null) instance = TaskManager()
            EventBus.getDefault().register(instance!!)
        }

        private fun exists(task: Task): Boolean {
            return tasks.indexOf(task) != -1
        }

        fun loadConversation(peerId: Int, extended: Boolean, listener: OnCompleteListener?) {
            if (loadingIds.indexOf(peerId) != -1) return
            val setter = VKApi.messages().conversationsById.extended(extended).peerIds(peerId).extended(true).fields(VKUser.FIELDS_DEFAULT)
            loadingIds.add(peerId)
            addProcedure(setter, VKConversation::class.java, listener, arrayOf(Keys.UPDATE_CONVERSATION, peerId))
        }

        fun loadMessage(messageId: Int, extended: Boolean, listener: OnCompleteListener?) {
            if (loadingIds.contains(messageId)) return
            val setter = VKApi.messages().byId.messageIds(messageId).extended(extended).fields(if (extended) VKUser.FIELDS_DEFAULT else "")
            loadingIds.add(messageId)
            addProcedure(setter, VKMessage::class.java, listener, arrayOf(Keys.UPDATE_MESSAGE, messageId))
        }

        fun loadUser(userId: Int, listener: OnCompleteListener?) {
            if (loadingIds.contains(userId)) return
            val setter = VKApi.users().get().userIds(userId).fields(VKUser.FIELDS_DEFAULT)
            loadingIds.add(userId)
            addProcedure(setter, VKUser::class.java, listener, arrayOf(Keys.UPDATE_USER, userId))
        }

        fun loadGroup(groupId: Int, listener: OnCompleteListener?) {
            if (loadingIds.contains(groupId)) return
            val setter = VKApi.groups().byId.groupId(groupId).fields(VKGroup.FIELDS_DEFAULT)
            loadingIds.add(groupId)
            addProcedure(setter, VKGroup::class.java, listener, arrayOf(Keys.UPDATE_GROUP, groupId))
        }

        fun loadChat(chatId: Int, fields: String, listener: OnCompleteListener) {
            if (loadingIds.contains(chatId)) return
            val setter = VKApi.messages().chat.chatId(chatId).fields(fields)
            loadingIds.add(chatId)
            addProcedure(setter, VKChat::class.java, listener, null)
        }

        fun addProcedure(setter: MethodSetter, cls: Class<*>?, listener: OnCompleteListener?, pushData: Array<Any>?) {
            val task = Task(setter, listener)


            if (!exists(task)) {
                tasks.add(task)
            }

            execute {
                setter.execute(cls, object : VKApi.OnResponseListener {
                    override fun onSuccess(models: ArrayList<*>?) {
                        if (exists(task)) {
                            tasks.remove(task)
                        }

                        if (!ArrayUtil.isEmpty(pushData)) {
                            val key = pushData!![0] as String

                            if (!ArrayUtil.isEmpty(models)) {
                                val id = pushData[1] as Int * if (key == Keys.UPDATE_GROUP) -1 else 1
                                loadingIds.remove(id)
                                when (key) {
                                    Keys.UPDATE_GROUP -> CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, models!![0])
                                    Keys.UPDATE_USER -> CacheStorage.insert(DatabaseHelper.USERS_TABLE, models!![0])
                                    Keys.UPDATE_CONVERSATION -> CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, models!![0])
                                    Keys.UPDATE_MESSAGE -> CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, models!![0])
                                }

                                EventBus.getDefault().postSticky(pushData)
                            }
                        }

                        listener?.onComplete(models)
                    }


                    override fun onError(e: Exception) {
                        listener?.onError(e)
                    }
                })
            }
        }

        fun sendMessage(setter: MethodSetter, listener: OnCompleteListener?) {
            addProcedure(setter, Int::class.java, listener, null)
        }

        fun resendMessage(randomId: Long) {
            for (task in tasks) {
                val setter = task.setter as MessageMethodSetter
                if (setter.getParams().contains(randomId.toString())) {
                    sendMessage(setter, task.listener)
                    break
                }
            }
        }

        fun execute(runnable: () -> Unit) {
            LowThread(Runnable {runnable()}).start()
        }
    }
}
