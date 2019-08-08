package ru.melodin.fast.api

interface OnResponseListener {
    fun onComplete(models: ArrayList<*>?)
    fun onError(e: Exception)
}
