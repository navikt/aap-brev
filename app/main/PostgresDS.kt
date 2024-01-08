import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

internal object Hikari {
    private lateinit var datasource: DataSource

    fun init(config: PostgresConfig) {
        datasource = createAndMigrate(config)
    }

    fun <T> transaction(block: (Connection) -> T): T {
        return datasource.connection.use { connection ->
            try {
                connection.autoCommit = false
                val result = block(connection)
                connection.commit()
                result
            } catch (e: Throwable) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun createAndMigrate(config: PostgresConfig): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.username
                password = config.password
                maximumPoolSize = 3
                minimumIdle = 1
                initializationFailTimeout = 5000
                idleTimeout = 10001
                connectionTimeout = 1000
                maxLifetime = 30001
                driverClassName = config.driver
            }
        ).apply {
            Flyway.configure()
                .dataSource(this)
                .locations("classpath")
                .load()
                .migrate()
        }
}


