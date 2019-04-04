package com.github.yag.config

import com.github.yag.crypto.AESCrypto
import com.google.common.base.CaseFormat
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.Properties
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class Configuration @JvmOverloads constructor(private val properties: Map<String, String>, private val base: String = "") {

    constructor(properties: Properties, base: String = "") : this(HashMap<String, String>().apply {
        properties.stringPropertyNames().forEach {
            put(it, properties.getProperty(it))
        }
    }, base)

    fun <T : Any> refresh(obj: T) {
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

                if (!config.isEmpty()) {
                    val value = properties[config]

                    if (isPlainType(fieldType)) {
                        checkRequired(value, annotation)
                        value?.let {
                            field.set(obj, parse(fieldType, it, encrypted))
                        }
                    } else if (isCollectionType(fieldType)) {
                        checkRequired(value, annotation)
                        if (fieldValue != null) {
                            if (value != null) {
                                parseCollection(genericFieldType as ParameterizedType, value, fieldValue)
                            }
                        } else {
                            throw IllegalArgumentException("Collection $config can not be null.")
                        }
                    } else if (isMapType(fieldType)) {
                        if (fieldType != null) {
                            withPrefix("$config.").refreshMap(genericFieldType, fieldValue as MutableMap<Any, Any>)
                        } else {
                            throw IllegalArgumentException("Map $config can not be null.")
                        }
                    } else {
                        if ((value == null && annotation.required) || value?.toBoolean() == true) {
                            val configuration = withPrefix("$config.")
                            if (fieldValue == null) {
                                field.set(obj, configuration.get(fieldType))
                            } else {
                                configuration.refresh(fieldValue)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkRequired(value: Any?, annotation: Value) {
        if (annotation.required && value == null) {
            throw IllegalArgumentException("$base${annotation.config} is required.")
        }
    }

    private fun <T> parse(fieldType: Class<T>, value: String, encrypted: Encrypted? = null) : T {
        return (if (fieldType.isEnum) {
            getEnumValue(fieldType, value)
        } else {
            when (fieldType) {
                String::class.java -> {
                    if (encrypted != null) {
                        AESCrypto(encrypted.key).decryptBase64ToUTF(value)
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
                    throw IllegalArgumentException("Can not parse $fieldType.")
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

    private fun extract(prefix: String, properties: Map<String, String>) : HashMap<String, String> {
        val newProps = HashMap<String, String>()
        properties.filter {
            (it.key).startsWith(prefix)
        }.forEach {
            newProps[it.key.substring(prefix.length)] = it.value
        }
        return newProps
    }

    private fun withPrefix(prefix: String): Configuration {
        return Configuration(extract(prefix, properties), base + prefix)
    }

    private fun refreshMap(genericType: Type, config: MutableMap<Any, Any>) {
        config.clear()
        if (genericType is ParameterizedType) {
            val typeArguments = genericType.actualTypeArguments
            properties.forEach { key, value ->
                val keyClass = typeArguments[0] as Class<*>
                val valueType = typeArguments[1]

                if (valueType is Class<*>) {
                    if (isPlainType(valueType)) {
                        config[parse(keyClass, key)] = parse(valueType, value)
                    } else {
                        val prefix = key.substringBefore(".")
                        config[parse(keyClass, prefix)] = withPrefix("$prefix.").get(valueType)
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

    fun <T: Any> get(clazz: Class<T>) : T {
        return clazz.newInstance().also {
            refresh(it)
        }
    }
}

fun <T: Any> Properties.config(clazz: Class<T>): T {
    return Configuration(this).get(clazz)
}

fun <T: Any> Properties.config(clazz: KClass<T>) : T {
    return this.config(clazz.java)
}

fun <T: Any> Map<String, String>.config(clazz: Class<T>) : T {
    return Configuration(this).get(clazz)
}

fun <T: Any> Map<String, String>.config(clazz: KClass<T>) : T {
    return this.config(clazz.java)
}