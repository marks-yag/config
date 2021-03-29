package com.github.yag.config

interface NestedKeyValueStore {

    fun getSubStore(key: String) : NestedKeyValueStore

    /**
     * T should be simple type
     */
    fun <T: Any> getValue(key: String, type: Class<T>, encryptedKey: String? = null) : T?

    /**
     * T should be simple type
     */
    fun readCollection(key: String) : Collection<String>?

    fun getFullKey(key: String) : String

    fun getEntries() : Set<String>

    fun getValue(key: String) : String? {
        return getValue(key, String::class.java)
    }

}
