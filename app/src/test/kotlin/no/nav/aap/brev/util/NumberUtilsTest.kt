package no.nav.aap.brev.util

import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.NumberUtils.formater
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NumberUtilsTest {
    @Test
    fun `formaterer`() {
        assertThat(BigDecimal("123456.00").formater(Språk.NB)).isEqualTo("123 456")
        assertThat(BigDecimal("123456.237").formater(Språk.NB)).isEqualTo("123 456,24")
        assertThat(BigDecimal("123456.237").formater(Språk.NN)).isEqualTo("123 456,24")
        assertThat(BigDecimal("123456.237").formater(Språk.EN)).isEqualTo("123,456.24")
    }
}
