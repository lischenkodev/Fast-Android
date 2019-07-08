package ru.melodin.fast.concurrent

import android.os.Process

class LowThread(target: () -> Unit) : Thread() {


    private fun getRunnable(target: () -> Unit): Runnable = Runnable {
        target()
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.run()
    }
}
