package ru.melodin.fast.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.collection.ArrayMap
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import ru.melodin.fast.api.LongPollEvents
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKLongPollServer
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.net.HttpRequest
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util

class LongPollService : Service() {

    private var isRunning: Boolean = false
    private var needRefresh: Boolean = false
    private var server: VKLongPollServer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        launchLongPoll()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        isRunning = false
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun launchLongPoll() {
        isRunning = true

        TaskManager.execute {
            MessageUpdater().run()
        }
    }

    private inner class MessageUpdater : Runnable {
        override fun run() {
            if (!isRunning) return
            while (isRunning) {
                if (!isRunning) break
                if (!Util.hasConnection()) {
                    needRefresh = true
                    Log.e(TAG, "no connection")
                    sleep()
                    continue
                }

                if (!UserConfig.isLoggedIn) {
                    needRefresh = false
                    sleep()
                    continue
                }

                if (needRefresh) {
                    EventBus.getDefault().postSticky(arrayOf<Any>(Keys.CONNECTED))
                    needRefresh = false
                }

                try {
                    if (server == null) {
                        server = VKApi.messages().longPollServer
                            .execute(VKLongPollServer::class.java)!![0] as VKLongPollServer?
                    }

                    val response = getResponse(server!!)
                    if (response.has("failed")) {
                        Log.w(TAG, "Failed to get response from")
                        Thread.sleep(1000)
                        server = null
                        continue
                    }

                    val tsResponse = response.optLong("ts")
                    val updates = response.optJSONArray("updates")
                    Log.i(TAG, "updates: " + updates!!)

                    server?.ts = tsResponse
                    if ((if (!ArrayUtil.isEmpty(updates)) updates.length() else 0) != 0) {
                        LongPollEvents.process(updates)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error: $e    Log below...")
                    e.printStackTrace()
                    server = null
                    continue
                }

            }
        }

        private fun sleep() {
            try {
                Thread.sleep(5000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Throws(Exception::class)
        private fun getResponse(server: VKLongPollServer): JSONObject {
            val params = ArrayMap<String, String>()
            params["act"] = "a_check"
            params["key"] = server.key
            params["ts"] = server.ts.toString()
            params["wait"] = "25"
            params["mode"] = "490"
            params["version"] = "7"

            val url = "https://" + server.server

            val buffer = HttpRequest[url, params].asString()
            return JSONObject(buffer)
        }
    }

    companion object {
        const val TAG = "FastVK LongPoll"
    }
}

