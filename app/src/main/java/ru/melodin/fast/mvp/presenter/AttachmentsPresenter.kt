package ru.melodin.fast.mvp.presenter

import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.base.mvp.PresenterBase
import ru.melodin.fast.mvp.contract.AttachmentsContract

class AttachmentsPresenter : PresenterBase<VKModel, AttachmentsContract.View>(),
    AttachmentsContract.Presenter {

    override fun onFilledList() {
        view!!.setRefreshing(false)
        view!!.setProgressBarVisible(false)
        view!!.setNoItemsViewVisible(false)
    }

    override fun viewIsReady() {

    }
}