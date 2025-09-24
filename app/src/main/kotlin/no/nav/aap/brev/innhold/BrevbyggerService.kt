package no.nav.aap.brev.innhold

import Brevdata
import Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.collections.filterIsInstance

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

    /**
     * TODO koble på denne ved bestilling
     */
    fun lagreInitiellBrevdata(brevbestillingReferanse: BrevbestillingReferanse, faktagrunnlag: Set<Faktagrunnlag>) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brevmal = checkNotNull(bestilling.deserialisertBrevmal())

        val delmaler = utledValgteDelmaler(brevmal)
        val faktagrunnlagMedVerdi = utledFaktagrunnlagMedVerdi(faktagrunnlag, bestilling.språk)
        val periodetekster =
            utledPeriodetekster(brevmal, emptyList()) // TODO periodiserte faktagrunnlag fra faktagrunnlag
        val valg = utledValg(brevmal, relevanteGrupper = emptyList()) // TODO relevante grupper fra faktagrunnlag
        val betingetTekst = utledBetingetTekst(brevmal)

        val brevdata = Brevdata(
            delmaler = delmaler,
            faktagrunnlag = faktagrunnlagMedVerdi,
            periodetekster = periodetekster,
            valg = valg,
            betingetTekst = betingetTekst,
        )

        brevbestillingRepository.oppdaterBrevdata(bestilling.id, brevdata)
    }

    fun utledValgteDelmaler(brevmal: Brevmal): List<Brevdata.Delmal> {
        return brevmal.delmaler.filter { it.obligatorisk }.map { Brevdata.Delmal(it.delmal._id) }
    }

    fun utledFaktagrunnlagMedVerdi(faktagrunnlag: Set<Faktagrunnlag>, språk: Språk): List<FaktagrunnlagMedVerdi> {
        val faktagrunnlagTekst = faktagrunnlagService.faktagrunnlagTilTekst(faktagrunnlag, språk)

        return faktagrunnlagTekst.entries.map { faktagrunnlag ->
            FaktagrunnlagMedVerdi(
                faktagrunnlag.key.name,
                faktagrunnlag.value
            )
        }
    }

    fun utledPeriodetekster(brevmal: Brevmal, periodiserteFaktagrunnlag: List<Any>): List<Brevdata.Periodetekst> {
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

    fun utledValg(brevmal: Brevmal, relevanteGrupper: List<String>): List<Brevdata.Valg> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Valg>()
                .mapNotNull { valg ->
                    val forhåndsvalgt =
                        valg.valg.valg.filterIsInstance<Brevmal.ValgAlternativ.GruppertTekst>().find { valgAlternativ ->
                            relevanteGrupper.contains(valgAlternativ.gruppe?.tekniskNavn)
                        } ?: return@mapNotNull null

                    Brevdata.Valg(id = valg.valg._id, forhåndsvalgt.tekst._id, null)
                }
        }
    }

    fun utledBetingetTekst(brevmal: Brevmal): List<Brevdata.BetingetTekst> {
        return emptyList() // TODO
    }

    fun kanFerdigstillesAutomatisk(brevbestillingReferanse: BrevbestillingReferanse): Boolean {
        /** TODO
         * Forslag til foreløpig logikk når vi kun lagrer enkle faktagrunnlag:
         * - Legg til felt på mal: kanSendesAutomatisk, må være true
         * - Ingen delmaler som ikke er obligatorisk
         * - Brevmal inneholder ikke:
         *      - periodetekster
         *      - valg
         *      - betinget tekst
         */
        return false
    }

    // TODO kan brukes i validering ved ferdigstilling
    fun finnAllePåkrevdeFaktagrunnlag(brevmal: Brevmal): List<String> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.flatMap { teksteditorElement ->
                when (teksteditorElement) {
                    is Brevmal.TeksteditorElement.Block -> {
                        filtrerFaktagrunnlag(teksteditorElement)
                    }

                    is Brevmal.TeksteditorElement.BetingetTekst -> {
                        teksteditorElement.tekst.teksteditor.flatMap { filtrerFaktagrunnlag(it) }
                    }

                    is Brevmal.TeksteditorElement.Valg -> {
                        teksteditorElement.valg.valg.filterIsInstance<Brevmal.ValgAlternativ.GruppertTekst>()
                            .flatMap { it.tekst.teksteditor }
                            .flatMap { filtrerFaktagrunnlag(it) }
                    }

                    is Brevmal.TeksteditorElement.Periodetekst -> {
                        emptyList() // har faktagrunnlag men flere verdier for samme faktagrunnlag
                    }
                }
            }
        }
    }

    private fun filtrerFaktagrunnlag(block: Brevmal.TeksteditorElement.Block): List<String> {
        return block.children.filterIsInstance<Brevmal.BlockChildren.Faktagrunnlag>()
            .map { it.tekniskNavn }.also { it.forEach { println(it) } }
    }
}