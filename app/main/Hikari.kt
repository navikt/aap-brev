import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import javax.sql.DataSource

internal object Hikari {
    private lateinit var datasource: DataSource

    fun init(config: PostgresConfig) {
        datasource = createAndMigrate(config)
    }

    fun <T> transaction(nested: Connection? = null, block: (Connection) -> T): T {
        return when (nested) {
            null -> datasource.connection.use { inTransaction(it, block) }
            else -> inTransaction(nested, block)
        }
    }

    private fun <T> inTransaction(connection: Connection, block: (Connection) -> T) =
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
                .locations("")
                .load()
                .migrate()
        }
}


