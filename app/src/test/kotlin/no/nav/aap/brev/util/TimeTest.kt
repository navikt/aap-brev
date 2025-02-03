package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TimeTest {

    @Test
    fun `formaterer dato i full lenge riktig`() {
        val dato = LocalDate.of(2025, 2, 7)
        assertEquals(dato.formaterFullLengde(Språk.NB), "7. februar 2025")
        assertEquals(dato.formaterFullLengde(Språk.NN), "7. februar 2025")
        assertEquals(dato.formaterFullLengde(Språk.EN), "February 7, 2025")
    }
}
