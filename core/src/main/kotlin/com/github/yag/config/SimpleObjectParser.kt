package com.github.yag.config

import com.github.yag.crypto.AESCrypto
import com.github.yag.crypto.decodeBase64
import com.github.yag.crypto.toUtf8
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL

object SimpleObjectParser {

    fun <T : Any> parse(
        type: Class<T>,
        value: String,
        encryptedKey: String? = null,
    ): T {
        return (if (type.isEnum) {
            getEnumValue(type, value)
        } else {
            when (type) {
                String::class.java -> {
                    encryptedKey?.let {
                        AESCrypto(it).decrypt(value.decodeBase64()).toUtf8()
                    } ?: value
                }
                Int::class.java -> value.toInt()
                Long::class.java -> value.toLong()
                Float::class.java -> value.toFloat()
                Double::class.java -> value.toDouble()
                Short::class.java -> value.toShort()
                Byte::class.java -> value.toByte()
                Boolean::class.java -> value.toBoolean()
                InetSocketAddress::class.java -> value.split(":").let { InetSocketAddress(it[0], it[1].toInt()) }
                URI::class.java -> URI(value)
                URL::class.java -> URL(value)

                else -> {
                    throw IllegalArgumentException("Unsupported type: $type")
                }
            }
        }) as T
    }
}
