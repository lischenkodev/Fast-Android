package ru.melodin.fast.mvp.presenter

import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.base.mvp.PresenterBase
import ru.melodin.fast.mvp.contract.MessagesContract

class MessagesPresenter : PresenterBase<VKMessage, MessagesContract.View>(), MessagesContract.Presenter {

    override fun onFilledList() {
        view!!.setRefreshing(false)
        view!!.setProgressBarVisible(false)
        view!!.setNoItemsViewVisible(false)
    }

    override fun viewIsReady() {

    }
}
