package ru.melodin.fast.concurrent

import android.os.Process

class LowThread(target: Runnable?) : Thread(target) {

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.run()
    }
}