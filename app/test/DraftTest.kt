import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit
import java.util.*

class DraftTest : H2() {

    @Test
    fun `can insert draft`() {
        val id = UUID.randomUUID()
        DraftRepo.insert(id, "some swag draft".toByteArray())
        assertEquals(1, count())
    }

    @Test
    fun `can delete draft`() {
        val id = UUID.randomUUID()
        DraftRepo.insert(id, "some swag draft".toByteArray())
        assertEquals(1, count())
        DraftRepo.delete(id)
        assertEquals(0, count())
    }

    @Test
    fun `can get draft by id`() {
        val id = UUID.randomUUID()
        DraftRepo.insert(id, "some swag draft".toByteArray())
        val actual = DraftRepo.selectById(id)!!
        val expected = Draft(id, "some swag draft".toByteArray())
        assertEquals(expected.truncateSeconds(), actual.truncateSeconds())
    }

    @Test
    fun `can get all drafts`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        DraftRepo.insert(id1, "some swag draft".toByteArray())
        DraftRepo.insert(id2, "another awesome draft".toByteArray())

        assertEquals(2, count())
    }
}

private fun Draft.truncateSeconds(): Draft = copy(
    created = created.truncatedTo(ChronoUnit.SECONDS),
    updated = updated.truncatedTo(ChronoUnit.SECONDS),
)
