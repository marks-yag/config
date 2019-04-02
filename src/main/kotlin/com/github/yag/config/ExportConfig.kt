package com.github.yag.config

import java.io.OutputStream
import java.io.PrintStream
import java.lang.IllegalArgumentException
import java.util.TreeMap
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

internal fun export(clazz: Class<*>, prefix: String, map: MutableMap<String, Item>, instance: Any = clazz.newInstance()) {
    getDeclaredFields(clazz).forEach { field ->
        field.isAccessible = true
        val fieldType = field.type
        val fieldValue = field.get(instance) ?: (fieldType.constructors.singleOrNull { it.parameterCount == 0 }?.newInstance() ?: "")
        val annotation = field.getAnnotation(Value::class.java)

        if (annotation != null) {
            val configName = prefix + annotation.config
            if (!map.contains(configName)) {
                when {
                    isPlainType(fieldType) || fieldValue is Collection<*> -> {
                        map[configName] = Item(fieldValue, annotation)
                    }
                    fieldValue is Map<*, *> -> {
                        fieldValue.forEach { any, u ->
                            map["$configName.$any"] = Item(u!!, annotation)
                        }
                    }
                    else -> {
                        export(fieldType, "$configName.", map, fieldValue)
                    }
                }
            } else {
                throw IllegalArgumentException("Duplicated configuration item: $configName")
            }
        }
    }
}

internal fun exportAsProperties(map: Map<String, Item>, out: OutputStream) {
    val ps = PrintStream(out)
    map.forEach { key, value ->
        exportDesc(value.annotation, ps)
        if (!value.annotation.required) {
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

internal fun <T : Any> valueToText(value: T): String {
    return when(value) {
        is Collection<*> -> {
            (value as Collection<*>).joinToString(",") {
                escape(it.toString())
            }
        }
        else -> {
            escape(value.toString())
        }
    }
}

internal data class Item(val value: Any, val annotation: Value)