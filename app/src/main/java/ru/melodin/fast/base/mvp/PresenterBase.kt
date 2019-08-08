package ru.melodin.fast.base.mvp

abstract class PresenterBase<T, V : MvpView<T>> : MvpPresenter<T, V> {

    var view: V? = null
        private set

    protected val isViewAttached: Boolean
        get() = view != null

    override fun attachView(mvpView: V) {
        view = mvpView
    }

    override fun detachView() {
        view = null
    }

    override fun destroy() {

    }

}