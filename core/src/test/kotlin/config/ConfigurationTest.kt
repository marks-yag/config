package config

import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.*
import kotlin.test.*

class ConfigurationTest {

    @Test
    fun testString() {
        mapOf(
            "username" to "mark",
            "password" to ""
        ).config(StringConfig::class).let {
            assertEquals("mark", it.username)
            assertEquals("", it.password)
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
        }
    }

    @Test
    fun testIllegalNumber() {
        val cause = assertFailsWith<IllegalStateException> {
            mapOf("port" to "foo").config(NumberConfig::class)
        }.cause
        assertNotNull(cause)
        assertEquals(NumberFormatException::class.java, cause.javaClass)
    }

    @Test
    fun testBoolean() {
        mapOf("auth" to "true").config(BooleanConfig::class).let {
            assertTrue(it.auth)
        }
        mapOf("auth" to "false").config(BooleanConfig::class).let {
            assertFalse(it.auth)
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
    }

    @Test
    fun testSubType() {
        mapOf(
            "store" to "@local",
            "store.local-addr" to "foo"
        ).config(SubTypeConfig::class).let {
            it.store.let {
                assertTrue(it is LocalStore)
                assertEquals("foo", it.localAddr)
            }
        }
    }

    @Test
    fun testCollection() {
        mapOf(
            "options" to Options.values().joinToString(","),
            "list" to "one,two,three",
            "list.one.auth" to "true",
            "list.two.auth" to "false",
            "list.three.auth" to "true",
            "stores" to "hot,cold",
            "stores.hot" to LocalStore::class.java.name,
            "stores.hot.local-addr" to "foo",
            "stores.cold" to "@remote",
            "stores.cold.remote-addr" to "bar"
        ).config(CollectionConfig::class).let {
            assertEquals(Options.values().toSet(), it.options)
            assertTrue(it.list[0].auth)
            assertFalse(it.list[1].auth)
            assertTrue(it.list[2].auth)

            assertEquals(2, it.stores.size)
            it.stores.first().let {
                assertTrue(it is LocalStore)
                assertEquals("foo", it.localAddr)
            }
            it.stores.last().let {
                assertTrue(it is RemoteStore)
                assertEquals("bar", it.remoteAddr)
            }
        }
    }

    @Test
    fun testMap() {
        val map = mapOf(
            "options.mark" to Options.Encryption.toString(),
            "options.guile" to setOf(Options.Compression, Options.Indexing).joinToString(","),
            "map.first" to "",
            "map.second" to "",
            "map.first.auth" to "true",
            "map.second.auth" to "false",
            "stores.hot" to LocalStore::class.java.name,
            "stores.cold" to RemoteStore::class.java.name,
            "stores.hot.local-addr" to "foo",
            "stores.cold.remote-addr" to "bar"
        )

        val config = map.config(MapConfig::class)
        config.let {
            assertEquals(2, it.options.size)
            assertEquals(setOf(Options.Encryption), it.options["mark"])
            assertEquals(setOf(Options.Compression, Options.Indexing), it.options["guile"])
            assertTrue(it.map["first"]!!.auth)
            assertFalse(it.map["second"]!!.auth)

            assertEquals(2, it.stores.size)
            it.stores["hot"]!!.let {
                assertTrue(it is LocalStore)
                assertEquals("foo", it.localAddr)
            }
            it.stores["cold"]!!.let {
                assertTrue(it is RemoteStore)
                assertEquals("bar", it.remoteAddr)
            }
        }
    }

    @Test
    fun testNest() {
        mapOf(
            "enum.mode" to Mode.SINGLE.toString(),
            "bool" to "",
            "bool.auth" to "false"
        ).config(NestConfig::class).let { nest ->
            nest.enum.let {
                assertEquals(Mode.SINGLE, it.mode)
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
    }

    @Test
    fun testMissing() {
        try {
            Properties().config(StringConfig::class)
            fail("Check missing failed.")
        } catch (e: IllegalArgumentException) {
            assertEquals("username is required.", e.message)
        }
    }

    @Test
    fun testToml() {
        val store = ConfigLoader.load(Format.TOML, "demo.toml")
        val demo = Configuration(store).get(Demo::class.java)
        verify(demo)
    }

    @Test
    fun testIni() {
        val store = ConfigLoader.load(Format.INI, "demo.ini")
        val demo = Configuration(store).get(Demo::class.java)
        verify(demo)
    }

    private fun verify(demo: Demo) {
        assertEquals("TOML Example", demo.title)
        assertEquals("Tom Preston-Werner", demo.owner.name)
        assertEquals("1979-05-27T07:32:01-08:00", demo.owner.dob)
        assertTrue(demo.database.enabled)
        assertEquals(listOf(8000, 8001, 8002), demo.database.ports)
        assertEquals(79.5, demo.database.tempTargets.cpu)
        assertEquals(72.0, demo.database.tempTargets.case)

        assertEquals(2, demo.servers.size)
        val alpha = demo.servers["alpha"]
        assertNotNull(alpha)
        assertEquals("10.0.0.1", alpha.ip)
        assertEquals("frontend", alpha.role)
        val beta = demo.servers["beta"]
        assertNotNull(beta)
        assertEquals("10.0.0.2", beta.ip)
        assertEquals("backend", beta.role)
    }

}
