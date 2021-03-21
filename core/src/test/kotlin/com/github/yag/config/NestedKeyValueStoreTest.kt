package com.github.yag.config

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue


class NestedKeyValueStoreTest {

    @TestTemplate
    @ExtendWith(EmptyNestedKeyValueStoreContextProvider::class)
    fun testEmpty(store: NestedKeyValueStore) {
        assertTrue(store.getEntries().isEmpty())
        assertNull(store.getValue("foo"))
        assertTrue(store.getSubStore("foo").getEntries().isEmpty())
    }

    @TestTemplate
    @ExtendWith(NormalNestedKeyValueStoreContextProvider::class)
    fun testGetEntries(store: NestedKeyValueStore) {
        store.getEntries().let {
            assertEquals(2, it.size)
            assertTrue {
                it.contains("options" to "guile,mark")
            }
            assertTrue {
                it.contains("tag" to "prod,test")
            }
        }
    }

    @TestTemplate
    @ExtendWith(NormalNestedKeyValueStoreContextProvider::class)
    fun testGetValue(store: NestedKeyValueStore) {
        assertEquals("guile,mark", store.getValue("options"))
        assertEquals("prod,test", store.getValue("tag"))

        assertFailsWith<IllegalArgumentException> {
            store.getValue("options.guile")
        }
    }

    @TestTemplate
    @ExtendWith(NormalNestedKeyValueStoreContextProvider::class)
    fun testSubStore(store: NestedKeyValueStore) {
        store.getSubStore("options").let {
            it.getEntries().let {
                assertEquals(2, it.size)
            }
        }

        assertFailsWith<IllegalArgumentException> {
            store.getSubStore("options.guile")
        }
    }

    class EmptyNestedKeyValueStoreContextProvider : TestTemplateInvocationContextProvider {

        override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
            return listOf(
                ConfigLoader.load(Format.INI),
                //ConfigLoader.load(Format.TOML)
            ).map { invocationContext(it) }.stream()
        }

        override fun supportsTestTemplate(context: ExtensionContext): Boolean {
            return true
        }

    }

    class NormalNestedKeyValueStoreContextProvider : TestTemplateInvocationContextProvider {

        override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
            return listOf(
                ConfigLoader.load(Format.INI, "normal.ini"),
                //ConfigLoader.load(Format.TOML)
            ).map { invocationContext(it) }.stream()
        }

        override fun supportsTestTemplate(context: ExtensionContext): Boolean {
            return true
        }

    }

}

fun invocationContext(parameter: NestedKeyValueStore): TestTemplateInvocationContext {
    return object : TestTemplateInvocationContext {
        override fun getDisplayName(invocationIndex: Int): String {
            return parameter.javaClass.toString()
        }

        override fun getAdditionalExtensions(): MutableList<Extension> {
            return mutableListOf(
                object : ParameterResolver {
                    override fun supportsParameter(
                        parameterContext: ParameterContext,
                        extensionContext: ExtensionContext
                    ): Boolean {
                        return parameterContext.parameter.type == NestedKeyValueStore::class.java
                    }

                    override fun resolveParameter(
                        parameterContext: ParameterContext,
                        extensionContext: ExtensionContext
                    ): Any {
                        return parameter
                    }
                }
            )
        }
    }
}
