package no.nav.aap.brev.bestilling

import no.nav.aap.brev.IntegrationTest
import no.nav.aap.brev.feil.ValideringsfeilException
import no.nav.aap.brev.innhold.harFaktagrunnlag
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.test.randomUnikReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.booleanArrayOf

class BrevbestillingTest : IntegrationTest() {

    @Test
    fun `oppretter bestilling v2`() {
        val resultat = opprettBrevbestilling(brukV3 = false, ferdigstillAutomatisk = false)
        assertThat(resultat.brevbestilling.brev).isNotNull
        assertThat(resultat.brevbestilling.brevmal).isNull()
        assertThat(resultat.brevbestilling.brevdata).isNull()
        assertThat(resultat.brevbestilling.status).isEqualTo(Status.UNDER_ARBEID)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
        assertThat(resultat.alleredeOpprettet).isFalse
    }

    @Test
    fun `oppretter bestilling v3`() {
        val resultat = opprettBrevbestilling(brukV3 = true, ferdigstillAutomatisk = false)
        assertThat(resultat.brevbestilling.brev).isNull()
        assertThat(resultat.brevbestilling.brevmal).isNotNull
        assertThat(resultat.brevbestilling.brevdata).isNotNull
        assertThat(resultat.brevbestilling.status).isEqualTo(Status.UNDER_ARBEID)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
        assertThat(resultat.alleredeOpprettet).isFalse
    }

    @Test
    fun `oppretter bestilling, henter innhold, fyller inn faktagrunnlag og oppdaterer status v2`() {
        val resultat = opprettBrevbestilling(
            brukV3 = false,
            brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            ferdigstillAutomatisk = false,
            faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
        )

        assertThat(resultat.brevbestilling.brev).isNotNull
        assertThat(resultat.brevbestilling.brev?.harFaktagrunnlag()).isFalse
        assertThat(resultat.brevbestilling.status).isEqualTo(Status.UNDER_ARBEID)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
        assertThat(resultat.alleredeOpprettet).isFalse
    }

    @Test
    fun `oppretter bestilling, henter innhold, lagrer initielle brevdata og oppdaterer status v3`() {
        val resultat = opprettBrevbestilling(
            brukV3 = true,
            brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            ferdigstillAutomatisk = false,
            faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
        )

        assertThat(resultat.brevbestilling.brevmal).isNotNull
        assertThat(resultat.brevbestilling.brevdata).isNotNull
        assertThat(resultat.brevbestilling.status).isEqualTo(Status.UNDER_ARBEID)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
        assertThat(resultat.alleredeOpprettet).isFalse
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `setter status ferdigstilt og legger til jobb for automatisk ferdigstilling dersom brev kan sendes automatisk`(
        brukV3: Boolean
    ) {
        val resultat = opprettBrevbestilling(
            brukV3 = brukV3,
            brevtype = Brevtype.VARSEL_OM_BESTILLING,
            ferdigstillAutomatisk = true
        )

        assertThat(resultat.brevbestilling.status).isEqualTo(Status.FERDIGSTILT)
        assertAntallJobber(resultat.brevbestilling.referanse, 1)
        dataSource.transaction { connection ->
            val mottakerRepository = MottakerRepositoryImpl(connection)
            val mottakere = mottakerRepository.hentMottakere(resultat.brevbestilling.id)
            assertThat(mottakere).hasSize(1)
            assertThat(mottakere).allSatisfy {
                assertThat(it.ident).isEqualTo(resultat.brevbestilling.brukerIdent)
                assertThat(it.identType).isEqualTo(IdentType.FNR)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `ferdigstiller ikke ved bestilling som ikke skal ferdigstilles automatisk`(brukV3: Boolean) {
        val resultat = opprettBrevbestilling(
            brukV3 = brukV3,
            brevtype = Brevtype.VARSEL_OM_BESTILLING,
            ferdigstillAutomatisk = false,
        )

        assertThat(resultat.brevbestilling.status).isEqualTo(Status.UNDER_ARBEID)
        assertAntallJobber(resultat.brevbestilling.referanse, 0)
    }

    @Test
    fun `feiler ved bestilling som skal ferdigstilles automatisk dersom brevet ikke kan sendes automatisk v2`() {
        val unikReferanse: UnikReferanse = randomUnikReferanse()
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brukV3 = false,
                brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
                unikReferanse = unikReferanse,
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brev automatisk"
        )
        dataSource.transaction { connection ->
            val bestilling = BrevbestillingRepositoryImpl(connection).hent(unikReferanse)
            assertThat(bestilling).isNull()
        }
    }

    @Test
    fun `feiler ved bestilling som skal ferdigstilles automatisk dersom brevet ikke kan sendes automatisk v3`() {
        val unikReferanse: UnikReferanse = randomUnikReferanse()
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brukV3 = true,
                brevtype = Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
                unikReferanse = unikReferanse,
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke automatisk ferdigstille brevbestilling: Brevmal er ikke konfigurert til at brevet kan sendes automatisk."
        )
        dataSource.transaction { connection ->
            val bestilling = BrevbestillingRepositoryImpl(connection).hent(unikReferanse)
            assertThat(bestilling).isNull()
        }
    }

    @Test // Kan slettes med v2, dekkes av BrevbyggerServiceTest
    fun `feiler ved bestilling som skal ferdigstilles automatisk dersom brevet kan sendes automatisk men har faktagrunnlag`() {
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brevtype = Brevtype.KLAGE_AVVIST,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brev automatisk"
        )
    }

    @Test // Kan slettes med v2, dekkes av BrevbyggerServiceTest
    fun `feiler ved bestilling som skal ferdigstilles automatisk dersom brevet kan sendes automatisk men er ikke fullstendig`() {
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brevtype = Brevtype.KLAGE_TRUKKET,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brev automatisk"
        )
    }

    @Test // Kan slettes med v2, dekkes av BrevbyggerServiceTest
    fun `feiler ved bestilling som skal ferdigstilles automatisk dersom brevet kan sendes automatisk men innhold kan redigeres`() {
        val exception = assertThrows<ValideringsfeilException> {
            opprettBrevbestilling(
                brevtype = Brevtype.KLAGE_OPPRETTHOLDELSE,
                ferdigstillAutomatisk = true,
                faktagrunnlag = setOf(Faktagrunnlag.FristDato11_7(frist = LocalDate.now())),
            )
        }
        assertThat(exception.message).isEqualTo(
            "Kan ikke ferdigstille brev automatisk"
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `gjør ingenting dersom bestillingen allerede er opprettet`(brukV3: Boolean) {
        val resultat1 = opprettBrevbestilling(
            brukV3 = brukV3,
            ferdigstillAutomatisk = false
        )

        val resultat2 = opprettBrevbestilling(
            brukV3 = brukV3,
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
