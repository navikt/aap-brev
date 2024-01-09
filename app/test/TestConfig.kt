

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket

object TestConfig {
    val postgres: PostgresConfig = PostgresConfig(
        host = "localhost",
        port = "5432",
        database = "test_db",
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
        driver = "org.h2.Driver",
        cluster = "test",
    )

    val sanity: SanityConfig = SanityConfig(
        token = "123",
        host = "localhost:${PortUtil.getFreePort()}",
    )
}

object PortUtil {
    private val mutex = Mutex()
    private val reserved = mutableSetOf<Int>()

    fun getFreePort(): Int {
        val port = ServerSocket(0).use(ServerSocket::getLocalPort)

        runBlocking { 
            reserve(port) 
        }

        return port
    }

    private suspend fun reserve(port: Int) {
        mutex.withLock {
            if (port in reserved) return reserve(getFreePort())
            else reserved.add(port)
        }
    }
}

