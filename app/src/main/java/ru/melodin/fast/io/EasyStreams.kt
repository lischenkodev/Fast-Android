package ru.melodin.fast.io

import java.io.*
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object EasyStreams {

    private const val BUFFER_SIZE = 8192
    private const val CHAR_BUFFER_SIZE = 4096

    @Throws(IOException::class)
    @JvmOverloads
    fun read(from: InputStream, encoding: Charset = Charsets.UTF_8): String {
        return read(InputStreamReader(from, encoding))
    }

    @Throws(IOException::class)
    fun read(from: Reader): String {
        val builder = StringWriter(CHAR_BUFFER_SIZE)
        try {
            copy(from, builder)
            return builder.toString()
        } finally {
            close(from)
        }
    }

    @Throws(IOException::class)
    fun readBytes(from: InputStream): ByteArray {
        val output = ByteArrayOutputStream(Math.max(from.available(), BUFFER_SIZE))
        try {
            copy(from, output)
        } finally {
            close(from)
        }
        return output.toByteArray()
    }

    @Throws(IOException::class)
    fun write(from: ByteArray, to: OutputStream) {
        try {
            to.write(from)
            to.flush()
        } finally {
            close(to)
        }
    }

    @Throws(IOException::class)
    fun write(from: String, to: OutputStream) {
        write(from, OutputStreamWriter(to, Charsets.UTF_8))
    }

    @Throws(IOException::class)
    fun write(from: CharArray, to: Writer) {
        try {
            to.write(from)
            to.flush()
        } finally {
            close(to)
        }
    }

    @Throws(IOException::class)
    fun write(from: String, to: Writer) {
        try {
            to.write(from)
            to.flush()
        } finally {
            close(to)
        }
    }

    @Throws(IOException::class)
    fun copy(from: Reader, to: Writer): Long {
        val buffer = CharArray(CHAR_BUFFER_SIZE)
        var read: Int
        var total: Long = 0

        do {
            read = from.read(buffer)

            if (read == -1) break

            to.write(buffer, 0, read)
            total += read.toLong()
        } while (true)

        return total
    }

    @Throws(IOException::class)
    fun copy(from: InputStream, to: OutputStream): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        var total: Long = 0

        do {
            read = from.read(buffer)

            if (read == -1) break

            to.write(buffer, 0, read)
            total += read.toLong()
        } while (true)

        return total
    }

    @JvmOverloads
    fun buffer(input: InputStream, size: Int = BUFFER_SIZE): BufferedInputStream {
        return input as? BufferedInputStream ?: BufferedInputStream(input, size)
    }

    @JvmOverloads
    fun buffer(output: OutputStream, size: Int = BUFFER_SIZE): BufferedOutputStream {
        return output as? BufferedOutputStream ?: BufferedOutputStream(output, size)
    }

    @JvmOverloads
    fun buffer(input: Reader, size: Int = CHAR_BUFFER_SIZE): BufferedReader {
        return input as? BufferedReader ?: BufferedReader(input, size)
    }

    @JvmOverloads
    fun buffer(output: Writer, size: Int = CHAR_BUFFER_SIZE): BufferedWriter {
        return output as? BufferedWriter ?: BufferedWriter(output, size)
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun gzip(input: InputStream, size: Int = BUFFER_SIZE): GZIPInputStream {
        return input as? GZIPInputStream ?: GZIPInputStream(input, size)
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun gzip(input: OutputStream, size: Int = BUFFER_SIZE): GZIPOutputStream {
        return input as? GZIPOutputStream ?: GZIPOutputStream(input, size)
    }

    private fun close(c: Closeable?): Boolean {
        if (c != null) {
            try {
                c.close()
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return false
    }

}
