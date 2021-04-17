package com.github.yag.config

import java.net.InetSocketAddress
import java.net.URI
import java.net.URL

class SimpleObjectParser {

    private val parsers = HashMap<Class<*>, Parser<*>>()

    init {
        register(String::class.java) { it }
        register(Int::class.java) { it.toInt() }
        register(Long::class.java) { it.toLong() }
        register(Float::class.java) { it.toFloat() }
        register(Double::class.java) { it.toDouble() }
        register(Short::class.java) { it.toShort() }
        register(Byte::class.java) { it.toByte() }
        register(Boolean::class.java) { it.toBoolean() }
        register(InetSocketAddress::class.java) { it.split(":").run { InetSocketAddress(this[0], this[1].toInt()) } }
        register(URL::class.java) { URL(it) }
        register(URI::class.java) { URI(it) }
    }

    fun <T : Any> parse(
        type: Class<T>,
        value: String,
    ): T {
        return (if (type.isEnum) {
            getEnumValue(type, value)
        } else {
            val parser = parsers[type]
            requireNotNull(parser) {
                "Unsupported type: $type"
            }
            parser.parse(value)
        }) as T
    }

    fun <T> register(type: Class<T>, parser: Parser<T>) {
        parsers[type] = parser
    }

}
