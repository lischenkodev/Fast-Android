package ru.melodin.fast.api

interface OnCompleteListener {
    fun onComplete(models: ArrayList<*>?)
    fun onError(e: Exception)
}
