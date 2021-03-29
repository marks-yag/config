package com.github.yag.config

class PropertiesKeyValueStore(private val map: Map<String, String>, private val base: String = "") : NestedKeyValueStore {

    override fun getSubStore(key: String): PropertiesKeyValueStore {
        val prefix = "${getFullKey(key)}."
        return PropertiesKeyValueStore(map, prefix)
    }

    override fun <T : Any> getValue(key: String, type: Class<T>, encryptedKey: String?): T? {
        require(!key.contains('.')) {
            key
        }
        val fullKey = getFullKey(key)
        return map[fullKey]?.let {  value ->
            SimpleObjectParser.parse(type, value, encryptedKey)
        }
    }

    override fun readCollection(key: String) : Collection<String>? {
        val value = getValue(key, String::class.java, null)
        return value?.split(",")
    }

    override fun getFullKey(key: String) : String {
        require(!key.contains('.')) {
            key
        }
        return "$base$key"
    }

    override fun getEntries(): Set<String> {
        return map.entries.filter {
            it.key.startsWith(base)
        }.map {
            it.key.removePrefix(base)
        }.filter {
            !it.contains('.')
        }.toSet()
    }
}
