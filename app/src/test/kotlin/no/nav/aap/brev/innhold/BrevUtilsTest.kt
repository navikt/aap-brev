package no.nav.aap.brev.innhold

import no.nav.aap.brev.test.fakes.brev
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrevUtilsTest {
    @Test
    fun `har faktagrunnlag`() {
        assertTrue(brev(listOf("faktagrunnlag")).harFaktagrunnlag())
    }

    @Test
    fun `har ikke faktagrunnlag`() {
        assertFalse(brev(medFaktagrunnlag = emptyList()).harFaktagrunnlag())
    }
}