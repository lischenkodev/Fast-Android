package ru.melodin.fast.base.mvp

interface MvpPresenter<T, V : MvpView<T>> {

    fun attachView(mvpView: V)

    fun viewIsReady()

    fun onFilledList()

    fun detachView()

    fun destroy()

}
