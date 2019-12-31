package com.github.yag.config

import com.google.common.base.CaseFormat
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.util.*
import kotlin.collections.Collection
import kotlin.collections.LinkedHashMap
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.contains
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.set
import kotlin.collections.singleOrNull
import kotlin.reflect.KClass

fun export(clazz: Class<*>, out: PrintStream) {
    val map = LinkedHashMap<String, Item>()
    export(clazz, "", map)
    exportAsProperties(map, out)
}

internal fun export(clazz: Class<*>): Map<String, Item> {
    return TreeMap<String, Item>().also {
        export(clazz, "", it)
    }
}

internal fun export(clazz: KClass<*>): Map<String, Item> {
    return export(clazz.java)
}

internal fun export(
    clazz: Class<*>,
    prefix: String,
    map: MutableMap<String, Item>,
    instance: Any = clazz.getDeclaredConstructor().newInstance(),
    required: Boolean = true
) {
    getDeclaredFields(clazz).forEach { field ->
        field.isAccessible = true
        val fieldType = field.type
        val fieldValue =
            field.get(instance) ?: (fieldType.constructors.singleOrNull { it.parameterCount == 0 }?.newInstance()
                ?: "") //TODO maybe let it null is better.
        val annotation = field.getAnnotation(Value::class.java)

        if (annotation != null) {
            val config = annotation.config.let {
                if (it.isEmpty()) {
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.name)
                } else {
                    it
                }
            }

            val configName = prefix + config
            if (!map.contains(configName)) {
                when {
                    isSimpleType(fieldType) || fieldValue is Collection<*> -> {
                        map[configName] = Item(fieldValue, annotation, required && annotation.required)
                    }
                    fieldValue is Map<*, *> -> {
                        fieldValue.forEach { (any, u) ->
                            map["$configName.$any"] = Item(u!!, annotation, required && annotation.required)
                        }
                    }
                    else -> {
                        export(fieldType, "$configName.", map, fieldValue, required && annotation.required)
                    }
                }
            } else {
                throw IllegalArgumentException("Duplicated configuration item: [$configName].")
            }
        }
    }
}

internal fun exportAsProperties(map: Map<String, Item>, out: OutputStream) {
    val ps = PrintStream(out)
    map.forEach { (key, value) ->
        exportDesc(value.annotation, ps)
        if (!value.required) {
            ps.print("#")
        }
        ps.println("$key=${valueToText(value.value)}")
        ps.println()
    }
}

internal fun exportDesc(annotation: Value, ps: PrintStream) {
    if (annotation.desc.isNotBlank()) {
        ps.println("#")
        annotation.desc.split("\n").forEach {
            ps.println("# $it")
        }
        ps.println("#")
    }
}

internal fun escape(value: String): String {
    return value.replace(" ", "\\ ")
}

internal fun <T : Any> valueToText(value: T?): String {
    return when {
        value == null -> ""
        value is InetSocketAddress -> "${value.hostString}:${value.port}"
        value is Collection<*> -> {
            (value as Collection<*>).joinToString(",") {
                valueToText(it)
            }
        }
        isSimpleType(value.javaClass) -> escape(value.toString())
        else  -> {
            value.javaClass.name
        }
    }
}

internal data class Item(val value: Any, val annotation: Value, val required: Boolean = annotation.required)