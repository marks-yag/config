package com.github.yag.config

import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.reflect.KClass

class SimpleObjectParser {

    private val parsers = HashMap<Class<*>, Parser<*>>()

    init {
        register(String::class.java) { it }
        registerKClass(Int::class) { it.toInt() }
        registerKClass(Long::class) { it.toLong() }
        registerKClass(Float::class) { it.toFloat() }
        registerKClass(Double::class) { it.toDouble() }
        registerKClass(Short::class) { it.toShort() }
        registerKClass(Byte::class) { it.toByte() }
        registerKClass(Boolean::class) { it.toBoolean() }

        register(InetSocketAddress::class.java) { it.split(":").run { InetSocketAddress(this[0], this[1].toInt()) } }
        register(URL::class.java) { URL(it) }
        register(URI::class.java) { URI(it) }
        register(File::class.java) { File(it) }
        register(Path::class.java) { Paths.get(it) }
        register(Duration::class.java) { Duration.parse(it) }
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

    private fun <T: Any> registerKClass(type: KClass<T>, parser: Parser<T>) {
        register(type.javaObjectType, parser)
        register(type.java, parser)
    }

    fun <T> register(type: Class<T>, parser: Parser<T>) {
        parsers[type] = parser
    }

}
