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

    @Test
    fun `kan ferdigstilles automatisk dersom kan sendes automatisk, ikke har faktagrunnlag, ikke kan redigeres, og er fullsteindig`() {
        val brev = brev(
            kanSendesAutomatisk = true,
            medFaktagrunnlag = emptyList(),
            kanRedigeres = false,
            erFullstendig = true,
        )
        assertTrue(brev.kanFerdigstillesAutomatisk())
    }

    @Test
    fun `kan ikke ferdigstilles automatisk selv om det ikke har faktagrunnlag, ikke kan redigeres, og er fullsteindig, dersom det ikke kan sendes automatisk`() {
        val brev = brev(
            kanSendesAutomatisk = false,
            medFaktagrunnlag = emptyList(),
            kanRedigeres = false,
            erFullstendig = true,
        )
        assertFalse(brev.kanFerdigstillesAutomatisk())
    }

    @Test
    fun `kan ikke ferdigstilles automatisk dersom brevet har faktagrunnlag`() {
        val brev = brev(
            kanSendesAutomatisk = true,
            medFaktagrunnlag = listOf(KjentFaktagrunnlag.FRIST_DATO_11_7.name),
            kanRedigeres = false,
            erFullstendig = true,
        )
        assertFalse(brev.kanFerdigstillesAutomatisk())
    }

    @Test
    fun `kan ikke ferdigstilles automatisk dersom brevet kan redigeres`() {
        val brev = brev(
            kanSendesAutomatisk = true,
            medFaktagrunnlag = emptyList(),
            kanRedigeres = true,
            erFullstendig = true,
        )
        assertFalse(brev.kanFerdigstillesAutomatisk())
    }

    @Test
    fun `kan ikke ferdigstilles automatisk dersom brevet ikke er fullstendig`() {
        val brev = brev(
            kanSendesAutomatisk = true,
            medFaktagrunnlag = emptyList(),
            kanRedigeres = false,
            erFullstendig = false,
        )
        assertFalse(brev.kanFerdigstillesAutomatisk())
    }
}