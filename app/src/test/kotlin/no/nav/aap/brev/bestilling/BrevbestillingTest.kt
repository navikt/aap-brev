package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.harFaktagrunnlag
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.prosessering.ProsesseringStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class BrevbestillingTest : IntegrationTest() {

    @Test
    fun `oppretter bestilling, henter innhold, fyller inn faktagrunnlag og oppdaterer status`() {
        val resultat = opprettBrevbestilling(
            brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            ferdigstillAutomatisk = false,
            faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
        )

        assertThat(resultat.brevbestilling.brev).isNotNull
        assertThat(resultat.brevbestilling.brev?.harFaktagrunnlag()).isFalse
        assertThat(resultat.brevbestilling.prosesseringStatus).isEqualTo(ProsesseringStatus.BREVBESTILLING_LØST)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
        assertThat(resultat.alleredeOpprettet).isFalse
    }

    @Test
    fun `legger til jobb for automatisk ferdigstilling dersom brev kan sendes automatisk`() {
        val resultat = opprettBrevbestilling(
            brevtype = Brevtype.VARSEL_OM_BESTILLING,
            ferdigstillAutomatisk = true,
        )

        assertThat(resultat.brevbestilling.prosesseringStatus).isEqualTo(ProsesseringStatus.BREVBESTILLING_LØST)
        assertAntallJobber(resultat.brevbestilling.referanse, 1)
    }

    @Test
    fun `ferdigstiller ikke ved bestilling som ikke skal ferdigstilles automatisk`() {
        val resultat = opprettBrevbestilling(
            brevtype = Brevtype.VARSEL_OM_BESTILLING,
            ferdigstillAutomatisk = false,
        )

        assertThat(resultat.brevbestilling.prosesseringStatus).isEqualTo(ProsesseringStatus.BREVBESTILLING_LØST)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
    }

    @Test
    fun `feiler ved bestilling bestill som skal ferdigstilles automatisk dersom brevet ikke kan sendes automatisk`() {
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brev automatisk"
        )
    }

    @Test
    fun `gjør ingenting dersom bestillingen allerede er opprettet`() {
        val resultat1 = opprettBrevbestilling()

        val resultat2 = opprettBrevbestilling(
                    saksnummer = resultat1.brevbestilling.saksnummer,
                    brukerIdent = checkNotNull(resultat1.brevbestilling.brukerIdent),
                    behandlingReferanse = resultat1.brevbestilling.behandlingReferanse,
                    unikReferanse = resultat1.brevbestilling.unikReferanse,
                    brevtype = resultat1.brevbestilling.brevtype,
                    språk = resultat1.brevbestilling.språk,
                    vedlegg = resultat1.brevbestilling.vedlegg,
        )

        assertThat(resultat1.alleredeOpprettet).isFalse
        assertThat(resultat2.alleredeOpprettet).isTrue
        assertThat(resultat1.brevbestilling).isEqualTo(resultat2.brevbestilling)
    }
}