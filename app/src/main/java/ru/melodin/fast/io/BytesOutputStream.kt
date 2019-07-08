package ru.melodin.fast.io

import java.io.ByteArrayOutputStream

class BytesOutputStream : ByteArrayOutputStream {

    val byteArray: ByteArray
        get() = buf

    constructor() : super(8192)

    constructor(size: Int) : super(size)
}
