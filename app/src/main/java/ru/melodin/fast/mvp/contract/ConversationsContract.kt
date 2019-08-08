package ru.melodin.fast.mvp.contract

import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.base.mvp.MvpPresenter
import ru.melodin.fast.base.mvp.MvpView

interface ConversationsContract {

    interface View : MvpView<VKConversation> {

        fun getCachedConversations(count: Int, offset: Int)

        fun loadConversations(count: Int, offset: Int)

    }

    interface Presenter : MvpPresenter<VKConversation, View>

}
