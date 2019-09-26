package ru.melodin.fast.mvp.presenter

import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.base.mvp.PresenterBase
import ru.melodin.fast.mvp.contract.ConversationsContract

class ConversationsPresenter : PresenterBase<VKConversation, ConversationsContract.View>(),
    ConversationsContract.Presenter {

    override fun onFilledList() {
        view ?: return
        view!!.setRefreshing(false)
        view!!.setProgressBarVisible(false)
        view!!.setNoItemsViewVisible(false)
    }

    override fun viewIsReady() {

    }
}