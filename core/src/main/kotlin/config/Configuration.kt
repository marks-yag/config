package config

import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.set
import kotlin.reflect.KClass

class Configuration @JvmOverloads constructor(
    private val properties: NestedKeyValueStore,
    private val simpleObjects: SimpleObjects = SimpleObjects()
) {

    private fun <T : Any> refresh(obj: T) {
        LOG.debug("Refresh: {}.", obj)
        val initMethod = getDeclaredMethods(obj.javaClass).singleOrNull {
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

            if (annotation != null) {
                val config = annotation.config.let {
                    it.ifEmpty {
                        toLowerHyphen(field.name)
                    }
                }

                require(config.isNotEmpty())

                val result = parseField(fieldType, genericFieldType, config, fieldValue)
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
        fieldValue: Any?
    ): Any? {
        LOG.debug("Field type: {}, generic field type: {}, config: {}", fieldType, genericFieldType, config)
        var result: Any? = fieldValue
        when {
            isCollectionType(fieldType) -> {
                result = parseCollection(
                    genericFieldType,
                    config,
                    fieldType,
                    (result
                        ?: kotlin.run { getCollection(fieldType as Class<MutableCollection<Any>>) }) as MutableCollection<Any>
                )
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
                result = try {
                    parse(fieldType, config, fieldValue)
                } catch (e: Exception) {
                    throw IllegalStateException("Parse $config failed.", e)
                }
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
        LOG.debug(
            "Parse collection, generic field type: {}, config: {}, field type: {}.",
            genericFieldType,
            config,
            fieldType
        )
        val elementType = (genericFieldType as ParameterizedType).actualTypeArguments[0] as Class<*>

        return properties.readCollection(config)?.let { collection ->
            result.addAll(collection.map {
                if (simpleObjects.isSimple(elementType)) {
                    simpleObjects.parse(elementType, it)
                } else {
                    val type = properties.getSubStore(config).getValue(it)?.run {
                        if (startsWith("@")) {
                            getSubType(elementType, removePrefix("@"))
                        } else {
                            Class.forName(this)
                        }
                    } ?: elementType
                    getSubConfig(config).getSubConfig(it).get(type)
                }
            })
            result
        }
    }

    private fun getSubType(type: Class<*>, id: String): Class<*> {
        val subTypes = type.getAnnotation(SubTypes::class.java)
        checkNotNull(subTypes) {
            "A SubTypes annotation was expected for $type."
        }
        return (subTypes.values.firstOrNull {
            it.id == id
        } ?: throw IllegalStateException("No sub type id $id found for $type")).value.java
    }

    private fun parse(
        fieldType: Class<*>,
        config: String,
        fieldValue: Any?
    ): Any? {
        if (simpleObjects.isSimple(fieldType)) {
            return properties.getValue(config)?.let { simpleObjects.parse(fieldType, it) }
        } else {
            val implClass = properties.getValue(config)

            return if (implClass != null || fieldValue != null) {
                val configuration = getSubConfig(config)
                val type = if (implClass != null && implClass.startsWith("@")) {
                    getSubType(fieldType, implClass.removePrefix("@"))
                } else if (implClass.isNullOrBlank()) {
                    fieldType
                } else {
                    Class.forName(implClass)
                }

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
                    val type = if (value.isNullOrBlank()) valueType else Class.forName(value)
                    map[simpleObjects.parse(keyClass, key)] = getSubConfig(key).get(type)
                } else {
                    // With parameterized type
                    val valueType = valueType as ParameterizedType
                    val rawType = valueType.rawType as Class<MutableCollection<Any>>

                    val collection = parseField(rawType, valueType, key, null) as MutableCollection<Any>
                    map[simpleObjects.parse(keyClass, key)] = collection
                }
            }
        }
    }

    private fun getCollection(type: Class<MutableCollection<Any>>): MutableCollection<Any> {
        val modifier = type.modifiers
        return if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier)) {
            when (type) {
                List::class.java -> ArrayList()
                Set::class.java -> HashSet()
                else -> throw IllegalArgumentException("Unsupported value type: ${type.name}")
            }
        } else {
            type.getConstructor().newInstance()
        }
    }

    private fun getMap(type: Class<MutableMap<Any, Any>>): MutableMap<Any, Any> {
        val modifier = type.modifiers
        return if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier)) {
            when (type) {
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
    return toStringMap().config(clazz)
}

fun <T : Any> Properties.config(clazz: KClass<T>): T {
    return this.config(clazz.java)
}

fun <T : Any> Map<String, String>.config(clazz: Class<T>): T {
    return Configuration(PropertiesKeyValueStore(this)).get(clazz)
}

fun <T : Any> Map<String, String>.config(clazz: KClass<T>): T {
    return this.config(clazz.java)
}
