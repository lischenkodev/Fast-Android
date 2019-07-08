package ru.melodin.fast.util

import androidx.collection.ArrayMap
import androidx.collection.SparseArrayCompat
import org.jetbrains.annotations.Contract
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

object ArrayUtil {

    private const val VALUE_NOT_FOUND = -1

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (array[i] == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: CharArray, value: Char, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (array[i] == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (array[i] == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (array[i] == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (array[i] == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @JvmOverloads
    fun linearSearch(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (java.lang.Float.compare(array[i], value) == 0) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @JvmOverloads
    fun linearSearch(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            if (java.lang.Double.compare(array[i], value) == 0) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    @Contract(pure = true)
    @JvmOverloads
    fun linearSearch(array: Array<Any>, value: Any, start: Int = 0, end: Int = array.size): Int {
        for (i in start until end) {
            val o = array[i]
            if (o == value) {
                return i
            }
        }
        return VALUE_NOT_FOUND
    }

    fun <T> toString(array: ArrayList<T>): String? {
        if (isEmpty(array)) return null

        val builder = StringBuilder()
        builder.append(array[0].toString())

        for (i in 1 until array.size) {
            builder.append(',')
            builder.append(array[i].toString())
        }

        return builder.toString()
    }

    fun toString(vararg array: Int): String? {
        if (isEmpty(array)) return null

        val buffer = StringBuilder(array.size * 12)
        buffer.append(array[0])
        for (i in 1 until array.size) {
            buffer.append(',')
            buffer.append(array[i])
        }
        return buffer.toString()
    }

    fun <E> singletonList(`object`: E): ArrayList<E> {
        val list = ArrayList<E>(1)
        list.add(`object`)

        return list
    }

    @Contract("null -> true")
    fun isEmpty(array: SparseArrayCompat<*>?): Boolean {
        return array == null || array.isEmpty
    }

    @Contract("null -> true")
    fun isEmpty(array: JSONArray?): Boolean {
        return array == null || array.length() == 0
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: ByteArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: CharArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: ShortArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: IntArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: LongArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: FloatArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: DoubleArray?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(array: Array<Any>?): Boolean {
        return array == null || array.isEmpty()
    }

    @Contract(value = "null -> true", pure = true)
    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    fun isEmpty(`object`: JSONObject?): Boolean {
        return `object` == null || `object`.length() == 0
    }

    fun isEmpty(array: ArrayMap<*, *>?): Boolean {
        return array == null || array.isEmpty
    }

    fun toString(ids: Collection<Int>): String? {
        val builder = StringBuilder()
        val list = mutableListOf(ids)

        builder.append(list[0])
        list.forEach {
            builder.append(',')
            builder.append(it.toString())
        }

        return builder.toString()
    }

    fun isEmpty(array: Array<File>?): Boolean {
        return array == null || array.isEmpty()

    }
}
