package ru.melodin.fast.base.mvp

interface MvpView<T> {

    fun createAdapter(items: ArrayList<T>?, offset: Int)

    fun setProgressBarVisible(visible: Boolean)

    fun setRefreshing(value: Boolean)

    fun setNoItemsViewVisible(visible: Boolean)

    fun clearList()

    fun showNoInternetToast()

    fun showErrorToast()
}
