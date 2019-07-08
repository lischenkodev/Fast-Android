package ru.melodin.fast.concurrent

import android.app.Activity
import ru.melodin.fast.api.VKApi
import java.lang.ref.WeakReference

abstract class AsyncCallback(activity: Activity) : Runnable {

    private val ref: WeakReference<Activity>?

    init {
        this.ref = WeakReference(activity)
    }

    @Throws(Exception::class)
    abstract fun ready()

    abstract fun done()

    abstract fun error(e: Exception)

    override fun run() {
        try {
            ready()
        } catch (e: Exception) {
            e.printStackTrace()

            if (ref?.get() != null) {
                ref.get()!!.runOnUiThread {
                    error(e)
                    VKApi.checkError(ref.get()!!, e)
                }
            }
            return
        }

        if (ref?.get() != null) {
            ref.get()!!.runOnUiThread { this.done() }
        }
    }
}
