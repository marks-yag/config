package com.github.yag.config

import com.google.common.base.CaseFormat
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.reflect.KClass

object ExportConfig {

    @JvmStatic
    @JvmOverloads
    fun export(clazz: Class<*>, instance: Any = clazz.getDeclaredConstructor().newInstance(), out: PrintStream) {
        val map = LinkedHashMap<String, Item>()
        export(clazz, "", map, instance)
        exportAsProperties(map, out)
    }

    @JvmStatic
    @JvmOverloads
    internal fun export(clazz: Class<*>, instance: Any = clazz.getDeclaredConstructor().newInstance()): Map<String, Item> {
        return TreeMap<String, Item>().also {
            export(clazz, "", it, instance)
        }
    }

    fun export(clazz: KClass<*>): Map<String, Item> {
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
            val rawFieldValue = field.get(instance)
            val fieldValue =
                rawFieldValue ?: (fieldType.constructors.singleOrNull { it.parameterCount == 0 }?.newInstance()
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
                            map["$configName"] = Item(fieldValue.keys, annotation, required && annotation.required)
                            fieldValue.forEach { (any, u) ->
                                map["$configName.$any"] = Item(u!!, annotation, required && annotation.required)
                            }
                        }
                        else -> {
                            export(fieldType, "$configName.", map, fieldValue, (required && annotation.required) || rawFieldValue != null)
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

    private fun exportDesc(annotation: Value, ps: PrintStream) {
        if (annotation.desc.isNotBlank()) {
            ps.println("#")
            annotation.desc.split("\n").forEach {
                ps.println("# $it")
            }
            ps.println("#")
        }
    }

    private fun escape(value: String): String {
        return value.replace(" ", "\\ ")
    }

    internal fun <T : Any> valueToText(value: T?): String {
        return when {
            value == null -> ""
            value is InetSocketAddress -> "${value.hostString}:${value.port}"
            value is Collection<*> -> {
                value.joinToString(",") {
                    valueToText(it)
                }
            }
            value is Enum<*> -> value.name
            isSimpleType(value.javaClass) -> escape(value.toString())
            else -> {
                value.javaClass.name
            }
        }
    }

}
