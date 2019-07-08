package ru.melodin.fast.io

import ru.melodin.fast.util.ArrayUtil
import java.io.*
import java.math.BigInteger

object FileStreams {

    private const val ONE_KB = 1024
    private const val ONE_MB = ONE_KB * 1024
    private const val ONE_GB = ONE_MB * 1024

    private const val ONE_TB = ONE_GB * 1024L
    private const val ONE_PB = ONE_TB * 1024L
    private const val ONE_EB = ONE_PB * 1024L

    private val ONE_ZB = BigInteger.valueOf(ONE_EB).multiply(BigInteger.valueOf(1024L))
    private val ONE_YB = ONE_ZB.multiply(BigInteger.valueOf(1024L))

    @Throws(IOException::class)
    fun read(from: File): String {
        return EasyStreams.read(reader(from))
    }


    @Throws(IOException::class)
    fun write(from: String, to: File) {
        EasyStreams.write(from, writer(to))
    }

    @Throws(IOException::class)
    fun write(from: ByteArray, to: File) {
        EasyStreams.write(from, FileOutputStream(to))
    }

    @Throws(IOException::class)
    fun append(from: ByteArray, to: File) {
        EasyStreams.write(from, FileOutputStream(to, true))
    }

    @Throws(IOException::class)
    fun append(from: CharArray, to: File) {
        EasyStreams.write(from, FileWriter(to, true))
    }

    @Throws(IOException::class)
    fun append(from: CharSequence, to: File) {
        EasyStreams.write(from as? String ?: from.toString(), FileWriter(to, true))
    }

    fun delete(dir: File) {
        if (dir.isDirectory) {
            val files = dir.listFiles()
            for (file in files!!) {
                delete(file)
            }
        } else {
            dir.delete()
        }
    }

    fun search(dir: File, name: String): File? {
        if (!dir.isDirectory) {
            throw IllegalArgumentException("dir can't be file.")
        }

        val files = dir.listFiles()
        if (ArrayUtil.isEmpty(files)) {
            return null
        }

        for (file in files!!) {
            if (file.isDirectory) {
                search(file, name)
            } else if (file.name.contains(name)) {
                return file
            }
        }

        return null
    }

    @Throws(FileNotFoundException::class)
    fun reader(from: File): Reader {
        return InputStreamReader(FileInputStream(from), Charsets.UTF_8)
    }

    @Throws(FileNotFoundException::class)
    fun writer(to: File): Writer {
        return OutputStreamWriter(FileOutputStream(to), Charsets.UTF_8)
    }
}
