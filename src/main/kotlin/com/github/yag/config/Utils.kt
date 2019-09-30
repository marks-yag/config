package com.github.yag.config

import com.google.common.primitives.Primitives
import java.lang.reflect.Field
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL

internal fun getDeclaredFields(type: Class<*>, fields: MutableCollection<Field> = ArrayList()): Collection<Field> {
    if (type != Object::class.java) {
        fields.addAll(type.declaredFields)
        type.superclass?.let {
            getDeclaredFields(it, fields)
        }
    }
    return fields
}

internal fun isSimpleType(fieldType: Class<*>) =
    fieldType == java.lang.String::class.java ||
            fieldType.isPrimitive ||
            Primitives.isWrapperType(fieldType) ||
            fieldType.isEnum ||
            fieldType == URI::class.java ||
            fieldType == URL::class.java ||
            fieldType == InetSocketAddress::class.java

internal fun isCollectionType(fieldType: Class<*>) = Collection::class.java.isAssignableFrom(fieldType)

internal fun isMapType(fieldType: Class<*>) = Map::class.java.isAssignableFrom(fieldType)

fun getEnumValue(type: Class<*>, enumValue: String): Any {
    val enumClz = type.enumConstants as Array<Enum<*>>
    return enumClz.first { it.name == enumValue }
}