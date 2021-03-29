package com.github.yag.config

import com.google.common.base.CaseFormat
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.set
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
            val encryptedKey: String? = encrypted?.key

            if (annotation != null) {
                val config = annotation.config.let {
                    if (it.isEmpty()) {
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.name)
                    } else {
                        it
                    }
                }

                require(config.isNotEmpty())

                var result = parseField(fieldType, genericFieldType, config, fieldValue, encryptedKey)
                checkRequired(result, annotation)

                result?.let { field.set(obj, result) }
            }
        }

        initMethod?.invoke(obj)
    }

    private fun parseField(
        fieldType: Class<*>,
        genericFieldType: Type,
        config: String,
        fieldValue: Any?,
        encryptedKey: String?
    ): Any? {
        LOG.debug("Field type: {}, generic field type: {}, config: {}", fieldType, genericFieldType, config)
        var result: Any? = fieldValue
        when {
            isCollectionType(fieldType) -> {
                result = parseCollection(genericFieldType, config, fieldType, (result?: kotlin.run { getCollection(fieldType as Class<MutableCollection<Any>>) }) as MutableCollection<Any>)
            }
            isMapType(fieldType) -> {
                if (result == null) {
                    result = getMap(fieldType as Class<MutableMap<Any, Any>>)
                }
                Configuration(properties.getSubStore(config)).refreshMap(
                    genericFieldType,
                    result as MutableMap<Any, Any>
                )
            }
            else -> {
                result = parse(fieldType, config, fieldValue, encryptedKey)
            }
        }
        return result
    }

    private fun parseCollection(
        genericFieldType: Type,
        config: String,
        fieldType: Class<*>,
        result: MutableCollection<Any>
    ): MutableCollection<Any>? {
        LOG.debug("Parse collection, generic field type: {}, config: {}, field type: {}.", genericFieldType, config, fieldType)
        val elementType = (genericFieldType as ParameterizedType).actualTypeArguments[0] as Class<*>

        return properties.readCollection(config)?.let { collection ->
            result.addAll(collection.map {
                if (isSimpleType(elementType)) {
                    SimpleObjectParser.parse(elementType, it)
                } else {
                    val type = properties.getSubStore(config).getValue(it)?.run {
                        Class.forName(this)
                    } ?: elementType
                    getSubConfig(config).getSubConfig(it).get(type)
                }
            })
            result
        }
    }

    private fun parse(
        fieldType: Class<*>,
        config: String,
        fieldValue: Any?,
        encryptedKey: String? = null
    ): Any? {
        if (isSimpleType(fieldType)) {
            return properties.getValue(config, fieldType, encryptedKey)
        } else {
            val implClass = properties.getValue(config, String::class.java)
            return if (implClass != null || fieldValue != null) {
                val configuration = getSubConfig(config)
                val type = if (implClass.isNullOrBlank()) fieldType else Class.forName(implClass)
                if (fieldValue == null) {
                    configuration.get(type)
                } else {
                    configuration.refresh(fieldValue)
                    fieldValue
                }
            } else {
                null
            }
        }
    }

    private fun checkRequired(value: Any?, annotation: Value) {
        if (annotation.required && value == null) {
            throw IllegalArgumentException("${properties.getFullKey(annotation.config)} is required.")
        }
    }

    private fun getSubConfig(key: String): Configuration {
        return Configuration(properties.getSubStore(key))
    }

    private fun refreshMap(genericType: Type, map: MutableMap<Any, Any>) {
        map.clear()
        LOG.debug("Refresh map, type: {}.", genericType)
        if (genericType is ParameterizedType) {
            val typeArguments = genericType.actualTypeArguments
            properties.getEntries().forEach { key ->
                LOG.debug("Refresh map: {}.", key)
                val keyClass = typeArguments[0] as Class<*>
                val valueType = typeArguments[1]

                if (valueType is Class<*>) {
                    // Without parameterized type
                    val value = properties.getValue(key)
                    checkNotNull(value)
                    val type = if (value.isNotBlank()) Class.forName(value) else valueType
                    map[SimpleObjectParser.parse(keyClass, key, null)] = getSubConfig(key).get(type)
                } else {
                    // With parameterized type
                    val valueType = valueType as ParameterizedType
                    val rawType = valueType.rawType as Class<MutableCollection<Any>>

                    val collection = parseField(rawType, valueType, key, null, null) as MutableCollection<Any>
                    map[SimpleObjectParser.parse(keyClass, key)] = collection
                }
            }
        }
    }

    private fun getCollection(type: Class<MutableCollection<Any>>) : MutableCollection<Any> {
        val modifier = type.modifiers
        return if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier)) {
            when(type) {
                List::class.java -> ArrayList()
                Set::class.java -> HashSet()
                else -> throw IllegalArgumentException("Unsupported value type: ${type.name}")
            }
        } else {
            type.getConstructor().newInstance()
        }
    }

    private fun getMap(type: Class<MutableMap<Any, Any>>) : MutableMap<Any, Any> {
        val modifier = type.modifiers
        return if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier)) {
            when(type) {
                Map::class.java -> HashMap()
                else -> throw IllegalArgumentException("Unsupported value type: ${type.name}")
            }
        } else {
            type.getConstructor().newInstance()
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
