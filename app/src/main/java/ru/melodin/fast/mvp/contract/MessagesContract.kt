package ru.melodin.fast.mvp.contract

import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.base.mvp.MvpPresenter
import ru.melodin.fast.base.mvp.MvpView

interface MessagesContract {

    interface View : MvpView<VKMessage> {

        fun getCachedMessages(count: Int, offset: Int)

        fun loadMessages(count: Int, offset: Int)

        fun sendCurrentMessage()

        fun loadConversation(peerId: Int)

        fun loadMessage(messageId: Int)

        fun showCantWrite(reason: Int)

        fun hideCantWrite()

        fun selectMessage(message: VKMessage)

        fun showPinnedMessage(message: VKMessage)

        fun hidePinnedMessage()

        fun editMessage(message: VKMessage)

        fun deleteMessages(messages: ArrayList<VKMessage>, forAll: Boolean?)

        fun removeMessages(messages: ArrayList<VKMessage>)

        fun confirmReplyMessages(messages: ArrayList<VKMessage>)

        fun confirmForwardMessages(messages: ArrayList<VKMessage>)

        fun replyMessage(replyId: Int, text: String)

        fun forwardMessages(peerId: Int, text: String, messages: ArrayList<VKMessage>)

        fun leaveFromChat()

        fun returnToChat()

        fun enableNotifications()

        fun disableNotifications()
    }

    interface Presenter : MvpPresenter<VKMessage, View>

}

