package com.github.yag.config

import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

class SimpleObjectParser {

    private val parsers = HashMap<Class<*>, Parser<*>>()

    init {
        register(String::class.java) { it }
        register(Int::class) { it.toInt() }
        register(Long::class) { it.toLong() }
        register(Float::class) { it.toFloat() }
        register(Double::class) { it.toDouble() }
        register(Short::class) { it.toShort() }
        register(Byte::class) { it.toByte() }
        register(Boolean::class) { it.toBoolean() }

        register(InetSocketAddress::class.java) { it.split(":").run { InetSocketAddress(this[0], this[1].toInt()) } }
        register(URL::class.java) { URL(it) }
        register(URI::class.java) { URI(it) }
        register(File::class.java) { File(it) }
        register(Path::class.java) { Paths.get(it) }
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

    fun isSimple(type: Class<*>) : Boolean {
        return type.isEnum || parsers.containsKey(type)
    }

    private fun <T: Any> register(type: KClass<T>, parser: Parser<T>) {
        type.javaObjectType?.let { register(it, parser) }
        register(type.java, parser)
    }

    fun <T> register(type: Class<T>, parser: Parser<T>) {
        parsers[type] = parser
    }

}
