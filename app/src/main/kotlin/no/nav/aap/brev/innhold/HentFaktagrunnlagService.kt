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
import java.time.format.DateTimeFormatter

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

    private fun erstattFaktagrunnlag(brev: Brev, faktagrunnlag: Set<Faktagrunnlag>): Brev =
        brev.copy(tekstbolker = brev.tekstbolker.map { tekstbolk ->
            tekstbolk.copy(innhold = tekstbolk.innhold.map { innhold ->
                innhold.copy(blokker = innhold.blokker.map { blokk ->
                    blokk.copy(innhold = blokk.innhold.map { blokkInnhold ->
                        mapBlokkInnholdTilTekst(
                            blokkInnhold,
                            faktagrunnlag
                        )
                    })
                })
            })
        })


    private fun finnFaktagrunnlag(brev: Brev): List<BlokkInnhold.Faktagrunnlag> =
        brev.tekstbolker
            .flatMap { it.innhold }
            .flatMap { it.blokker }
            .flatMap { it.innhold }
            .filterIsInstance<BlokkInnhold.Faktagrunnlag>()

    private fun mapBlokkInnholdTilTekst(blokkInnhold: BlokkInnhold, faktagrunnlag: Set<Faktagrunnlag>): BlokkInnhold =
        when (blokkInnhold) {
            is BlokkInnhold.FormattertTekst -> blokkInnhold
            is BlokkInnhold.Faktagrunnlag ->
                mapFaktagrunnlag(blokkInnhold)
                    ?.let { finnFaktagrunnlad(faktagrunnlag, it) }
                    ?.let { faktagrunnlagTilFormatertTekst(it, blokkInnhold) }
                    ?: blokkInnhold
        }

    private fun mapFaktagrunnlag(blokkInnhold: BlokkInnhold.Faktagrunnlag): FaktagrunnlagType? =
        FaktagrunnlagType.entries.find { it.name == blokkInnhold.tekniskNavn.uppercase() }

    private fun finnFaktagrunnlad(
        faktagrunnlag: Set<Faktagrunnlag>,
        faktagrunnlagType: FaktagrunnlagType
    ): Faktagrunnlag? =
        faktagrunnlag.find { it.type == faktagrunnlagType }

    private fun faktagrunnlagTilFormatertTekst(
        faktagrunnlag: Faktagrunnlag,
        blokkInnhold: BlokkInnhold.Faktagrunnlag
    ): BlokkInnhold.FormattertTekst {
        return when (faktagrunnlag) {
            is Faktagrunnlag.Startdato ->
                BlokkInnhold.FormattertTekst(
                    id = blokkInnhold.id,
                    tekst = faktagrunnlag.dato.format(DateTimeFormatter.ofPattern("dd.MM.YYYY")),
                    formattering = emptyList(),
                )
        }
    }
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
