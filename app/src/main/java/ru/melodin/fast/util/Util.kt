package ru.melodin.fast.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import ru.melodin.fast.R
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.io.BytesOutputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object Util {

    var dateFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        // 15:57
        //SimpleDateFormat dateMonthFormatter = new SimpleDateFormat("d MMM", Locale.getDefault());
        //SimpleDateFormat dateYearFormatter = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
        //SimpleDateFormat dateFullFormatter = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
    }

    private fun restart(activity: Activity, extras: Intent?, anim: Boolean) {
        val intent = Intent(activity, activity.javaClass)
        if (extras != null)
            intent.putExtras(extras)

        activity.startActivity(intent)
        activity.finish()

        if (anim)
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun restart(activity: Activity, anim: Boolean) {
        restart(activity, null, anim)
    }

    fun copyText(text: String) {
        val cm = AppGlobal.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(null, text))
    }

    fun parseSize(sizeInBytes: Long): String {
        val unit: Long = 1024
        if (sizeInBytes < unit) return "$sizeInBytes B"
        val exp = (Math.log(sizeInBytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1] + ""// + ("i");
        return String.format(Locale.US, "%.1f %sB", sizeInBytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun serialize(source: Any?): ByteArray? {
        try {
            val bos = BytesOutputStream()
            val out = ObjectOutputStream(bos)

            out.writeObject(source)
            out.close()
            return bos.byteArray
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    fun deserialize(source: ByteArray?): Any? {
        if (ArrayUtil.isEmpty(source)) {
            return null
        }

        try {
            val bis = ByteArrayInputStream(source)
            val `in` = ObjectInputStream(bis)

            val o = `in`.readObject()

            `in`.close()
            return o
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun dp(px: Float): Float {
        return px / AppGlobal.context.resources.displayMetrics.density
    }

    fun px(dp: Float): Float {
        return dp * AppGlobal.context.resources.displayMetrics.density
    }

    fun hasConnection(): Boolean {
        val cm = AppGlobal.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            network != null && capabilities != null
        } else {
            val networkInfo = cm.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    @Throws(Exception::class)
    fun saveFileByUrl(link: String): String {
        val url = URL(link)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.doOutput = false
        urlConnection.connect()

        val directory = File(Environment.getExternalStorageDirectory().toString() + "/VK")

        if (!directory.exists()) directory.mkdir()

        val name = link.substring(link.lastIndexOf("/") + 1)

        var file = File(directory, name)

        var i = 1
        while (file.exists()) {
            var fileName = link.substring(link.lastIndexOf("/") + 1)

            val dotIndex = fileName.lastIndexOf(".")

            var newName = fileName.substring(0, dotIndex)
            val ext = fileName.substring(dotIndex)
            newName += "-$i"

            fileName = newName + ext

            file = File(directory, fileName)
            i++
        }

        val fileOutput = FileOutputStream(file)
        val inputStream = urlConnection.inputStream

        val buffer = ByteArray(1024)
        var bufferLength: Int

        inputStream.read(buffer)

        while (inputStream.read(buffer) > 0) {

        }

        do {
            bufferLength = inputStream.read(buffer)
            if (bufferLength <= 0) break

            fileOutput.write(buffer, 0, bufferLength)
        } while (true)

        fileOutput.close()

        return directory.absolutePath
    }
}