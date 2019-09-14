package com.github.yag.config

import com.github.yag.crypto.AESCrypto
import java.lang.NumberFormatException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurationTest {

    @Test
    fun testString() {
        mapOf(
                "username" to "mark",
                "password" to AESCrypto("foo").encryptUTFToBase64("123")
        ).config(StringConfig::class).let {
            assertEquals("mark", it.username)
            assertEquals("123", it.password)
        }

        val config = StringConfig()
        export(StringConfig::class).let { result ->
            exportAsProperties(result, System.out)
            assertEquals(2, result.size)
            result["username"].let {
                assertNotNull(it)
                assertEquals(config.username, it.value)
                assertTrue(it.annotation.required)
            }

            result["password"].let {
                assertNotNull(it)
                assertEquals("", it.value)
                assertTrue(it.annotation.required)
            }
        }
    }

    @Test
    fun testNumber() {
        repeat(5) { loop ->
            mapOf(
                    "port" to "${loop + 80}",
                    "timeoutMs" to "${loop + 1000}",
                    "percent" to "${loop + 0.8}"
            ).config(NumberConfig::class).let {
                assertEquals(loop + 80, it.port)
                assertEquals(loop + 1000L, it.timeoutMs)
                assertEquals(loop + 0.8, it.percent)
            }

            val config = NumberConfig()
            export(NumberConfig::class).let { result ->
                exportAsProperties(result, System.out)
                result["port"].let {
                    assertNotNull(it)
                    assertEquals(config.port, it.value)
                    assertTrue(it.annotation.required)
                }

                result["timeoutMs"].let {
                    assertNotNull(it)
                    assertEquals(config.timeoutMs, it.value)
                    assertTrue(it.annotation.required)
                }

                result["percent"].let {
                    assertNotNull(it)
                    assertEquals(config.percent, it.value)
                    assertTrue(it.annotation.required)
                }
            }
        }
    }

    @Test(expected = NumberFormatException::class)
    fun testIllegalNumber() {
        mapOf("port" to "foo").config(NumberConfig::class)
    }

    @Test
    fun testBoolean() {
        mapOf("auth" to "true").config(BooleanConfig::class).let {
            assertTrue(it.auth)
        }
        mapOf("auth" to "false").config(BooleanConfig::class).let {
            assertFalse(it.auth)
        }

        val config = BooleanConfig()
        export(BooleanConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["auth"].let {
                assertNotNull(it)
                assertEquals(config.auth, it.value)
                assertTrue(it.annotation.required)
            }
        }
    }

    @Test
    fun testEnum() {
        Mode.values().forEach { mode ->
            mapOf(
                    "mode" to mode.toString()
            ).config(EnumConfig::class).let {
                assertEquals(mode, it.mode)
            }
        }

        val config = EnumConfig()
        export(EnumConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["mode"].let {
                assertNotNull(it)
                assertEquals(config.mode, it.value)
                assertTrue(it.annotation.required)
            }
        }
    }

    @Test
    fun testEndpoint() {
        mapOf(
            "address" to "127.0.0.1:9527",
            "url" to "http://127.0.0.1:9527",
            "uri" to "mailto:yag@github.com"
        ).config(EndpointConfig::class).let {
            assertEquals(InetSocketAddress("127.0.0.1", 9527), it.address)
            assertEquals(URL("http://127.0.0.1:9527"), it.url)
            assertEquals(URI("mailto:yag@github.com"), it.uri)
        }

        val config = EndpointConfig()
        export(EndpointConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["address"].let {
                assertNotNull(it)
                assertTrue(it.annotation.required)
            }
            result["url"].let {
                assertNotNull(it)
                assertTrue(it.annotation.required)
            }
            result["uri"].let {
                assertNotNull(it)
                assertTrue(it.annotation.required)
            }
        }
    }

    @Test
    fun testCollection() {
        mapOf(
                "options" to Options.values().joinToString(",")
        ).config(CollectionConfig::class).let {
            assertEquals(Options.values().toSet(), it.options)
        }

        val config = CollectionConfig()
        export(CollectionConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["options"].let {
                assertNotNull(it)
                assertEquals(config.options, it.value)
                assertTrue(it.annotation.required)
            }
        }
    }

    @Test
    fun testMap() {
        mapOf(
                "options.mark" to Options.Encryption.toString(),
                "options.guile" to setOf(Options.Compression, Options.Indexing).joinToString(",")
        ).config(MapConfig::class).let {
            assertEquals(2, it.options.size)
            assertEquals(setOf(Options.Encryption), it.options["mark"])
            assertEquals(setOf(Options.Compression, Options.Indexing), it.options["guile"])
        }

        val config = MapConfig()
        export(MapConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["options.test"].let {
                assertNotNull(it)
                val set = config.options["test"]
                assertNotNull(set)
                assertEquals(set, it.value)
                assertFalse(it.annotation.required)
            }

            result["options.prod"].let {
                assertNotNull(it)
                val set = config.options["prod"]
                assertNotNull(set)
                assertEquals(set, it.value)
                assertFalse(it.annotation.required)
            }
        }
    }

    @Test
    fun testNest() {
        mapOf(
                "enum.mode" to Mode.CLUSTER.toString(),
                "bool" to "true",
                "bool.auth" to "false"
        ).config(NestConfig::class).let { nest ->
            nest.enum.let {
                assertEquals(Mode.CLUSTER, it.mode)
            }
            nest.bool.let {
                assertNotNull(it)
                assertFalse(it.auth)
            }
        }

        mapOf(
                "enum.mode" to Mode.CLUSTER.toString(),
                "bool.auth" to "false"
        ).config(NestConfig::class).let { nest ->
            assertNull(nest.bool)
        }

        val config = NestConfig()
        export(NestConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["enum.mode"].let {
                assertNotNull(it)
                assertEquals(config.enum.mode, it.value)
                assertTrue(it.annotation.required)
                assertTrue(it.required)
            }

            result["bool.auth"].let {
                assertNotNull(it)
                assertEquals(false, it.value)
                assertTrue(it.annotation.required)
                assertFalse(it.required)
            }
        }
    }

    @Test
    fun testDefaultName() {
        mapOf(
            "hello" to "foo",
            "hello-world" to "bar"
        ).config(DefaultNameConfig::class).let { result ->
            assertEquals("foo", result.hello)
            assertEquals("bar", result.helloWorld)
        }

        val config = DefaultNameConfig()
        export(DefaultNameConfig::class).let { result ->
            exportAsProperties(result, System.out)
            result["hello"].let {
                assertNotNull(it)
            }
            result["hello-world"].let {
                assertNotNull(it)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMissing() {
        Properties().config(StringConfig::class)
    }

    @Test
    fun testValueToText() {
        assertEquals("foo", valueToText("foo"))
        assertEquals("false", valueToText(false))
        assertEquals("1", valueToText(1))
        assertEquals("1.2", valueToText(1.2))
        assertEquals("SINGLE", valueToText(Mode.SINGLE))
        assertEquals("127.0.0.1:9527", valueToText(InetSocketAddress("127.0.0.1", 9527)))
        assertEquals("http://127.0.0.1:9527", valueToText(URL("http://127.0.0.1:9527")))
        assertEquals("mailto:yag@github.com", valueToText(URI("mailto:yag@github.com")))
        assertEquals("1,2,3", valueToText(listOf(1, 2, 3)))
    }
}