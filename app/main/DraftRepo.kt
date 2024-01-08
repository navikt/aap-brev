import java.sql.Connection
import java.sql.Timestamp
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

object DraftRepo {

    fun insert(id: UUID, raw: ByteArray) = 
        Hikari.transaction { con ->
            con.prepareStatement("INSERT INTO draft (id, created, udpated, raw) VALUES (?, ?, ?, ?)").apply {
                setObject(1, id)
                setObject(2, raw)
                setObject(3, Timestamp.from(Instant.now()))
                setObject(4, Timestamp.from(Instant.now()))
            }.execute()
        }

    fun selectAll(): List<Draft> =
        Hikari.transaction { con ->
            con.prepareStatement("SELECT * FROM draft")
                .executeQuery()
                .map(Draft::fromResultSet)
        }

    fun selectById(id: UUID): Draft? =
        Hikari.transaction { con ->
            con.prepareStatement("SELECT * FROM draft where id = ?")
                .apply { setObject(1, id)}
                .executeQuery()
                .map(Draft::fromResultSet)
                .singleOrNull()
        }

    fun delete(id: UUID) =
        Hikari.transaction { con ->
            con.prepareStatement("DELETE FROM draft WHERE id = ?")
                .apply { setObject(1, id) }
                .execute()
        }
}

private fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence { while (next()) yield(block(this@map)) }.toList()

private fun ResultSet.getUUID(columnLabel: String): UUID = 
    UUID.fromString(this.getString(columnLabel))

data class Draft(
    val id: UUID,
    val raw: ByteArray,
    val created: Instant = Instant.now(),
    val update: Instant = Instant.now(),
) {
    companion object {
        fun fromResultSet(rs: ResultSet): Draft =
            Draft(
                id = rs.getUUID("id"),
                raw = rs.getBytes("raw"),
                created = rs.getTimestamp("created").toInstant(),
                update = rs.getTimestamp("updated").toInstant(),
            )
    }
}

