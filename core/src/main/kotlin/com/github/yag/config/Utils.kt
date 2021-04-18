package com.github.yag.config

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.ArrayList

internal fun getDeclaredFields(type: Class<*>, fields: MutableCollection<Field> = ArrayList()): Collection<Field> {
    if (type != Object::class.java) {
        fields.addAll(type.declaredFields)
        type.superclass?.let {
            getDeclaredFields(it, fields)
        }
    }
    return fields
}

internal fun getDeclaredMethods(type: Class<*>, methods: MutableCollection<Method> = ArrayList()): Collection<Method> {
    if (type != Object::class.java) {
        methods.addAll(type.declaredMethods)
        type.superclass?.let {
            getDeclaredMethods(it, methods)
        }
    }
    return methods
}

internal fun isCollectionType(fieldType: Class<*>) = Collection::class.java.isAssignableFrom(fieldType)

internal fun isMapType(fieldType: Class<*>) = Map::class.java.isAssignableFrom(fieldType)

fun getEnumValue(type: Class<*>, enumValue: String): Enum<*> {
    return java.lang.Enum.valueOf(type as Class<Enum<*>>, enumValue)
}

fun Properties.toStringMap() : Map<String, String> {
    return TreeMap<String, String>().also { map ->
        stringPropertyNames().forEach { key ->
            map[key] = getProperty(key)
        }
    }
}

fun toLowerHyphen(str: String) : String {
    return str.map {
        if (it.isUpperCase()) {
            "-" + it.toLowerCase()
        } else {
            it
        }
    }.joinToString("").removePrefix("_")
}
