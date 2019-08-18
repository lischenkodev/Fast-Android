package ru.melodin.fast.mvp.contract

import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.base.mvp.MvpPresenter
import ru.melodin.fast.base.mvp.MvpView

interface AttachmentsContract {

    interface View : MvpView<VKModel> {

        fun loadAttachments(count: Int, offset: Int)

    }

    interface Presenter : MvpPresenter<VKModel, View>

}
