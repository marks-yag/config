package com.github.yag.config

import com.github.yag.crypto.AESCrypto
import com.github.yag.crypto.decodeBase64
import com.github.yag.crypto.toUtf8
import com.google.common.base.CaseFormat
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.log
import kotlin.reflect.KClass

class Configuration(private val properties: NestedKeyValueStore) {

    constructor(properties: Map<String, String>) : this(PropertiesKeyValueStore(properties))

    constructor(properties: Properties) : this(properties.toStringMap())

    private fun <T : Any> refresh(obj: T) {
        val initMethod = getDeclaredMethods(obj.javaClass).singleOrNull() {
            it.getAnnotationsByType(Init::class.java) != null
        }
        initMethod?.let {
            check(it.parameterCount == 0)
        }

        getDeclaredFields(obj.javaClass).forEach { field ->
            field.isAccessible = true

            val fieldType = field.type
            val genericFieldType = field.genericType

            val fieldValue: Any? = field.get(obj)
            val annotation: Value? = field.getAnnotation(Value::class.java)
            val encrypted: Encrypted? = field.getAnnotation(Encrypted::class.java)

            if (annotation != null) {
                val config = annotation.config.let {
                    if (it.isEmpty()) {
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.name)
                    } else {
                        it
                    }
                }

                if (config.isNotEmpty()) {
                    val value = properties.getValue(config)

                    if (isSimpleType(fieldType)) {
                        checkRequired(value, annotation)
                        value?.let {
                            field.set(obj, parse(fieldType, it, encrypted))
                        }
                    } else if (isCollectionType(fieldType)) {
                        checkRequired(value, annotation)
                        if (fieldValue != null) {
                            if (value != null) {
                                withPrefix(config).parseCollection(
                                    genericFieldType as ParameterizedType,
                                    value,
                                    fieldValue
                                )
                            }
                        } else {
                            throw IllegalArgumentException("Collection $config can not be null.")
                        }
                    } else if (isMapType(fieldType)) {
                        if (fieldType != null) {
                            withPrefix(config).refreshMap(genericFieldType, fieldValue as MutableMap<Any, Any>)
                        } else {
                            throw IllegalArgumentException("Map $config can not be null.")
                        }
                    } else {
                        if (value != null || fieldValue != null || annotation.required) {
                            val configuration = withPrefix(config)
                            val type = if (value.isNullOrBlank()) fieldType else Class.forName(value)
                            if (fieldValue == null) {
                                field.set(obj, configuration.get(type))
                            } else {
                                if (type.isAssignableFrom(fieldValue.javaClass)) {
                                    configuration.refresh(fieldValue)
                                } else {
                                    field.set(obj, configuration.get(type))
                                    //TODO an error?
                                }
                            }
                        }
                    }
                }
            }
        }

        initMethod?.invoke(obj)
    }

    private fun checkRequired(value: Any?, annotation: Value) {
        if (annotation.required && value == null) {
            throw IllegalArgumentException("${properties.getFullKey(annotation.config)} is required.")
        }
    }

    private fun <T : Any> parse(fieldType: Class<T>, value: String, encrypted: Encrypted? = null): T {
        return (if (fieldType.isEnum) {
            getEnumValue(fieldType, value)
        } else {
            when (fieldType) {
                String::class.java -> {
                    if (encrypted != null) {
                        AESCrypto(encrypted.key).decrypt(value.decodeBase64()).toUtf8()
                    } else {
                        value
                    }
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
                    val type = properties.getValue("$value")?.let {
                        Class.forName(it)
                    } ?: fieldType

                    require(fieldType.isAssignableFrom(type))

                    withPrefix(value).get(type)
                }
            }
        }) as T
    }

    private fun parseCollection(genericType: ParameterizedType, value: String, fieldValue: Any) {
        with(fieldValue as MutableCollection<Any>) {
            clear()
            value.split(",").forEach {
                add(parse(genericType.actualTypeArguments[0] as Class<*>, it))
            }
        }
    }

    private fun withPrefix(prefix: String): Configuration {
        return Configuration(properties.getSubStore(prefix))
    }

    private fun refreshMap(genericType: Type, config: MutableMap<Any, Any>) {
        config.clear()
        LOG.debug("Refresh map, type: {}.", genericType)
        if (genericType is ParameterizedType) {
            val typeArguments = genericType.actualTypeArguments
            properties.getEntries().forEach { (key, value) ->
                LOG.debug("Refresh map: {} -> {}.", key, value)
                val keyClass = typeArguments[0] as Class<*>
                val valueType = typeArguments[1]

                if (valueType is Class<*>) {
                    if (isSimpleType(valueType)) {
                        config[parse(keyClass, key)] = parse(valueType, value)
                    } else {
                        val prefix = key.substringBefore(".")
                        val type = if (value.isNotBlank()) Class.forName(value) else valueType
                        config[parse(keyClass, prefix)] = withPrefix(prefix).get(type)
                    }
                } else {
                    val valueType = valueType as ParameterizedType
                    val rawType = valueType.rawType as Class<*>

                    val collection: Collection<Any> = when {
                        List::class.java.isAssignableFrom(rawType) -> ArrayList()
                        Set::class.java.isAssignableFrom(rawType) -> HashSet()
                        else -> throw IllegalArgumentException("Unsupported collection type: ${rawType.name}")
                    }

                    parseCollection(valueType, value, collection)
                    config[parse(keyClass, key)] = collection
                }
            }
        }
    }

    fun <T : Any> get(clazz: Class<T>): T {
        return clazz.getConstructor().newInstance().also {
            refresh(it)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Configuration::class.java)
    }
}

fun <T : Any> Properties.config(clazz: Class<T>): T {
    return Configuration(this).get(clazz)
}

fun <T : Any> Properties.config(clazz: KClass<T>): T {
    return this.config(clazz.java)
}

fun <T : Any> Map<String, String>.config(clazz: Class<T>): T {
    return Configuration(this).get(clazz)
}

fun <T : Any> Map<String, String>.config(clazz: KClass<T>): T {
    return this.config(clazz.java)
}
