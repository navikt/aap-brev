package no.nav.aap.brev.innhold

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class HentFaktagrunnlagService(
    private val hentFagtagrunnlagGateway: HentFagtagrunnlagGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): HentFaktagrunnlagService {
            return HentFaktagrunnlagService(
                BehandlingsflytGateway(),
                BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun hentFaktagrunnlag(brevbestillingReferanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brev = checkNotNull(bestilling.brev)
        val behandlingReferanse = bestilling.behandlingReferanse
        val faktagrunnlagTyper = finnFaktagrunnlag(brev).mapNotNull { mapFaktagrunnlag(it) }.toSet()

        val faktagrunnlag = hentFagtagrunnlagGateway.hent(behandlingReferanse, faktagrunnlagTyper)

        val oppdatertBrev = erstattFaktagrunnlag(brev, faktagrunnlag)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, oppdatertBrev)
    }

    private fun erstattFaktagrunnlag(brev: Brev, foo: Set<Faktagrunnlag>): Brev {
        TODO("Not yet implemented")
    }

    private fun mapFaktagrunnlag(faktagrunnlag: BlokkInnhold.Faktagrunnlag): FaktagrunnlagType? =
        FaktagrunnlagType.entries.find { it.name == faktagrunnlag.tekniskNavn.uppercase() }

    private fun finnFaktagrunnlag(brev: Brev): List<BlokkInnhold.Faktagrunnlag> =
        brev.tekstbolker
            .flatMap { it.innhold }
            .flatMap { it.blokker }
            .flatMap { it.innhold }
            .filterIsInstance<BlokkInnhold.Faktagrunnlag>()

}

// TODO Flytt alt under til Behandlingsflyt-kontrakt
const val FAKTAGRUNNLAG_TYPE_STARTDATO = "STARTDATO"

enum class FaktagrunnlagType(@JsonValue val verdi: String) {
    STARTDATO(FAKTAGRUNNLAG_TYPE_STARTDATO)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
sealed class Faktagrunnlag(val type: FaktagrunnlagType) {
    @JsonTypeName(FAKTAGRUNNLAG_TYPE_STARTDATO)
    data class Startdato(
        val dato: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.STARTDATO)

}
