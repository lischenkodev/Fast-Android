package ru.melodin.fast.mvp.presenter

import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.base.mvp.PresenterBase
import ru.melodin.fast.mvp.contract.UsersContract

class UsersPresenter : PresenterBase<VKUser, UsersContract.View>(), UsersContract.Presenter {

    override fun onFilledList() {
        view!!.setRefreshing(false)
        view!!.setProgressBarVisible(false)
        view!!.setNoItemsViewVisible(false)
    }

    override fun viewIsReady() {

    }
}