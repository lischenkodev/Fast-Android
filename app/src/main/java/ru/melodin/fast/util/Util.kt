package ru.melodin.fast.util

import android.content.ClipData
import android.os.Build
import android.os.Environment
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.io.BytesOutputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

object Util {

    var dateFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    var timeFormatter: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    var dayOfWeekFormatter: DateFormat = SimpleDateFormat("EEE", Locale.getDefault())
    var shortDateFormatter: DateFormat = DateFormat.getDateInstance(DateFormat.SHORT)

    fun formatShortTimestamp(ts: Long): String {
        val thenCal = GregorianCalendar()
        thenCal.timeInMillis = ts
        val nowCal = GregorianCalendar()
        nowCal.timeInMillis = System.currentTimeMillis()

        val f = if (thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
            && thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
            && thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)
        ) {
            timeFormatter
        } else if (thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
            && thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
            && nowCal.get(Calendar.DAY_OF_MONTH) - thenCal.get(Calendar.DAY_OF_MONTH) < 7
        ) {
            dayOfWeekFormatter
        } else {
            shortDateFormatter
        }
        return f.format(thenCal.time)
    }

    fun copyText(text: String) {
        AppGlobal.clipService.setPrimaryClip(ClipData.newPlainText(null, text))
    }

    fun parseSize(sizeInBytes: Long): String {
        val unit: Long = 1024
        if (sizeInBytes < unit) return "$sizeInBytes B"
        val exp = (ln(sizeInBytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1] + ""
        return String.format(
            Locale.US,
            "%.1f %sB",
            sizeInBytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
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
        return px / AppGlobal.res.displayMetrics.density
    }

    fun px(dp: Float): Float {
        return dp * AppGlobal.res.displayMetrics.density
    }

    fun hasConnection(): Boolean {
        val cm = AppGlobal.connectionService
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            network != null && capabilities != null
        } else {
            val networkInfo = cm.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
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

        do {
            bufferLength = inputStream.read(buffer)
            if (bufferLength <= 0) break

            fileOutput.write(buffer, 0, bufferLength)
        } while (true)

        fileOutput.close()

        return directory.absolutePath
    }
}