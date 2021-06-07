package config

import org.tomlj.TomlArray
import org.tomlj.TomlTable

class TomlKeyValueStore(private val table: TomlTable, private val base: String = "") : NestedKeyValueStore {

    override fun getSubStore(key: String): NestedKeyValueStore {
        val prefix = "${getFullKey(key)}."
        return TomlKeyValueStore(table.getTableOrEmpty(key), prefix)
    }

    override fun getValue(key: String): String? {
        require(!key.contains('.')) {
            key
        }
        return table.get(key)?.let {  value ->
            if (value is TomlArray) {
                Array(value.size()) {
                    value.get(it)
                }.joinToString(",")
            } else if (value is TomlTable) {
                null
            } else {
                value.toString()
            }
        }
    }

    override fun getFullKey(key: String): String {
        require(!key.contains('.')) {
            key
        }
        return "$base$key"
    }

    override fun readCollection(key: String): Collection<String>? {
        require(!key.contains('.')) {
            key
        }

        return table.getArray(key)?.let { array ->
            val size = array.size()
            return Array(size) {
                array.get(it).toString()
            }.toList()
        }
    }

    override fun getEntries(): Set<String> {
        return table.keySet()
    }
}
