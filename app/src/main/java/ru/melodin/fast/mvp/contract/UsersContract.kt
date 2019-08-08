package ru.melodin.fast.mvp.contract

import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.base.mvp.MvpPresenter
import ru.melodin.fast.base.mvp.MvpView

interface UsersContract {

    interface View : MvpView<VKUser> {

        fun getCachedUsers(count: Int, offset: Int)

        fun loadUsers(count: Int, offset: Int)

    }

    interface Presenter : MvpPresenter<VKUser, View>

}

