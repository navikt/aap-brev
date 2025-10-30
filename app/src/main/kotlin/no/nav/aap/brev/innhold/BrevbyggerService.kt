package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.bestilling.Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection

class BrevbyggerService(
    val brevbestillingRepository: BrevbestillingRepository,
    val faktagrunnlagService: FaktagrunnlagService
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbyggerService {
            return BrevbyggerService(
                BrevbestillingRepositoryImpl(connection),
                FaktagrunnlagService.konstruer(connection)
            )
        }
    }

    fun lagreInitiellBrevdata(brevbestillingReferanse: BrevbestillingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brevmal = checkNotNull(bestilling.brevmal?.tilBrevmal())

        val delmaler = utledValgteDelmaler(brevmal)
        val faktagrunnlagMedVerdi = utledFaktagrunnlagMedVerdi(faktagrunnlag, bestilling.språk)
        val periodetekster =
            utledPeriodetekster(brevmal, emptyList()) // TODO periodiserte faktagrunnlag fra faktagrunnlag
        val valg = utledValg(brevmal, relevanteKategorier = emptyList()) // TODO relevante kategorier fra faktagrunnlag
        val betingetTekst = utledBetingetTekst(
            brevmal,
            relevanteKategorier = emptyList()
        )  // TODO relevante kategorier fra faktagrunnlag

        val brevdata = Brevdata(
            delmaler = delmaler,
            faktagrunnlag = faktagrunnlagMedVerdi,
            periodetekster = periodetekster,
            valg = valg,
            betingetTekst = betingetTekst,
            fritekster = emptyList()
        )

        brevbestillingRepository.oppdaterBrevdata(bestilling.id, brevdata)
    }

    private fun utledValgteDelmaler(brevmal: Brevmal): List<Brevdata.Delmal> {
        return brevmal.delmaler.filter { it.obligatorisk }.map { Brevdata.Delmal(it.delmal._id) }
    }

    private fun utledFaktagrunnlagMedVerdi(
        faktagrunnlag: Set<Faktagrunnlag>,
        språk: Språk
    ): List<FaktagrunnlagMedVerdi> {
        val faktagrunnlagTekst = faktagrunnlagService.faktagrunnlagTilTekst(faktagrunnlag, språk)

        return faktagrunnlagTekst.entries.map { faktagrunnlag ->
            FaktagrunnlagMedVerdi(
                faktagrunnlag.key.name,
                faktagrunnlag.value
            )
        }
    }

    private fun utledPeriodetekster(
        brevmal: Brevmal,
        periodiserteFaktagrunnlag: List<Any>
    ): List<Brevdata.Periodetekst> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Periodetekst>()
                .flatMap { teksteditorElement ->
                    /** TODO
                     * - Hent ut alle faktagrunnlag i teksteditorElement.periodetekst
                     * - det må eksistere faktagrunnlag for fom- og/eller tom-dato
                     * - Sjekk om det finnes periodiserte faktagrunnlag som er relevant
                     * - returner teksteditorElement.periodetekst._id og alle faktagrunnlag med verdi
                     */
                    emptyList()
                }
        }
    }

    private fun utledValg(brevmal: Brevmal, relevanteKategorier: List<String>): List<Brevdata.Valg> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Valg>()
                .mapNotNull { valg ->
                    val forhåndsvalgt =
                        valg.valg.alternativer.filterIsInstance<Brevmal.ValgAlternativ.KategorisertTekst>()
                            .find { valgAlternativ ->
                                relevanteKategorier.contains(valgAlternativ.kategori?.tekniskNavn)
                            } ?: return@mapNotNull null
                    Brevdata.Valg(id = valg.valg._id, forhåndsvalgt._key, null)
                }
        }
    }

    private fun utledBetingetTekst(brevmal: Brevmal, relevanteKategorier: List<String>): List<Brevdata.BetingetTekst> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.BetingetTekst>()
                .mapNotNull { betingetTekst ->
                    if (betingetTekst.kategorier.map { it.tekniskNavn }.intersect(relevanteKategorier.toSet())
                            .isNotEmpty()
                    ) {
                        Brevdata.BetingetTekst(betingetTekst.tekst._id)
                    } else {
                        null
                    }
                }
        }
    }
}
