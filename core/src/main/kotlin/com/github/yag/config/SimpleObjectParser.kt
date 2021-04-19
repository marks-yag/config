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
        registerParser(String::class.java) { it }

        registerParserKClass(Int::class) { it.toInt() }
        registerParserKClass(Long::class) { it.toLong() }
        registerParserKClass(Float::class) { it.toFloat() }
        registerParserKClass(Double::class) { it.toDouble() }
        registerParserKClass(Short::class) { it.toShort() }
        registerParserKClass(Byte::class) { it.toByte() }
        registerParserKClass(Boolean::class) { it.toBoolean() }

        registerParser(InetSocketAddress::class.java) { it.split(":").run { InetSocketAddress(this[0], this[1].toInt()) } }
        registerParser(URL::class.java) { URL(it) }
        registerParser(URI::class.java) { URI(it) }
        registerParser(File::class.java) { File(it) }
        registerParser(Path::class.java) { Paths.get(it) }
        registerParser(Duration::class.java) { Duration.parse(it) }
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

    private fun <T: Any> registerParserKClass(type: KClass<T>, parser: Parser<T>) {
        registerParser(type.javaObjectType, parser)
        registerParser(type.java, parser)
    }

    fun <T> registerParser(type: Class<T>, parser: Parser<T>) {
        parsers[type] = parser
    }

}
