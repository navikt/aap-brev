import org.junit.jupiter.api.BeforeEach

abstract class H2 {
    init {
        Hikari.init(TestConfig.postgres)
    }

    @BeforeEach
    fun clear(): Unit = Hikari.transaction {
        it.prepareStatement("TRUNCATE TABLE draft").execute()
    }

    fun count(): Int = Hikari.transaction {
        it.prepareStatement("SELECT COUNT(*) FROM draft")
            .executeQuery()
            .map { rs -> rs.getInt(1) }
            .first()
    }
}

