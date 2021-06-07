package config

interface NestedKeyValueStore {

    fun getSubStore(key: String) : NestedKeyValueStore

    fun getValue(key: String) : String?

    fun readCollection(key: String) : Collection<String>?

    fun getFullKey(key: String) : String

    fun getEntries() : Set<String>

}
