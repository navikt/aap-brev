import java.sql.Timestamp
import java.sql.ResultSet
import java.util.UUID
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object DraftRepo {

    fun insert(id: UUID, raw: ByteArray) = 
        Hikari.transaction { con ->
        val query = "INSERT INTO draft (id, raw, created, updated) VALUES (?, ?, ?, ?)"
            con.prepareStatement(query).apply {
                setObject(1, id)
                setObject(2, raw)
                setObject(3, Timestamp.valueOf(LocalDateTime.now()))
                setObject(4, Timestamp.valueOf(LocalDateTime.now()))
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

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence { while (next()) yield(block(this@map)) }.toList()

fun ResultSet.getUUID(columnLabel: String): UUID = 
    UUID.fromString(this.getString(columnLabel))

data class Draft(
    val id: UUID,
    val raw: ByteArray,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun fromResultSet(rs: ResultSet): Draft =
            Draft(
                id = rs.getUUID("id"),
                raw = rs.getBytes("raw"),
                created = rs.getTimestamp("created").toLocalDateTime(),
                updated = rs.getTimestamp("updated").toLocalDateTime(),
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Draft

        if (id != other.id) return false
        if (!raw.contentEquals(other.raw)) return false
        if (created != other.created) return false
        if (updated != other.updated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + raw.contentHashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + updated.hashCode()
        return result
    }

}

