package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.DBConnection

class FaktagrunnlagService(
    private val hentFagtagrunnlagGateway: HentFagtagrunnlagGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): FaktagrunnlagService {
            return FaktagrunnlagService(
                BehandlingsflytGateway(),
                BrevbestillingRepositoryImpl(connection),
            )
        }
    }

    fun hentOgFyllInnFaktagrunnlag(brevbestillingReferanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brev = checkNotNull(bestilling.brev)
        val faktagrunnlagTyper = brev.finnFaktagrunnlag().mapNotNull { mapFaktagrunnlag(it) }.toSet()

        if (faktagrunnlagTyper.isEmpty()) {
            return
        }

        val faktagrunnlag = hentFagtagrunnlagGateway.hent(bestilling.behandlingReferanse, faktagrunnlagTyper)

        if (faktagrunnlag.isEmpty()) {
            return
        }

        val oppdatertBrev = fyllInnFaktagrunnlag(brev, faktagrunnlag)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, oppdatertBrev)
    }

    private fun fyllInnFaktagrunnlag(brev: Brev, faktagrunnlag: Set<Faktagrunnlag>): Brev =
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




    private fun mapBlokkInnholdTilTekst(blokkInnhold: BlokkInnhold, faktagrunnlag: Set<Faktagrunnlag>): BlokkInnhold =
        when (blokkInnhold) {
            is BlokkInnhold.FormattertTekst -> blokkInnhold
            is BlokkInnhold.Faktagrunnlag ->
                mapFaktagrunnlag(blokkInnhold)
                    ?.let { finnFaktagrunnlag(faktagrunnlag, it) }
                    ?.let { faktagrunnlagTilFormatertTekst(it, blokkInnhold) }
                    ?: blokkInnhold
        }

    private fun mapFaktagrunnlag(blokkInnhold: BlokkInnhold.Faktagrunnlag): FaktagrunnlagType? =
        FaktagrunnlagType.entries.find { it.name == blokkInnhold.tekniskNavn.uppercase() }

    private fun finnFaktagrunnlag(
        faktagrunnlag: Set<Faktagrunnlag>,
        faktagrunnlagType: FaktagrunnlagType
    ): Faktagrunnlag? =
        faktagrunnlag.find { it.type == faktagrunnlagType }

    private fun faktagrunnlagTilFormatertTekst(
        faktagrunnlag: Faktagrunnlag,
        blokkInnhold: BlokkInnhold.Faktagrunnlag
    ): BlokkInnhold.FormattertTekst {
        return when (faktagrunnlag) {
            is Faktagrunnlag.Testverdi ->
                BlokkInnhold.FormattertTekst(
                    id = blokkInnhold.id,
                    tekst = faktagrunnlag.testString,
                    formattering = emptyList(),
                )
        }
    }
}
