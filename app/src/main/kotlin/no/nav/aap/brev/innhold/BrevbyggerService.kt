package no.nav.aap.brev.innhold

import Brevdata
import Brevdata.FaktagrunnlagMedVerdi
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.feil.valider
import no.nav.aap.brev.kontrakt.Brevmal
import no.nav.aap.brev.kontrakt.Brevmal.DelmalValg
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.collections.filterIsInstance
import kotlin.collections.joinToString

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

    private fun utledFaktagrunnlagMedVerdi(faktagrunnlag: Set<Faktagrunnlag>, språk: Språk): List<FaktagrunnlagMedVerdi> {
        val faktagrunnlagTekst = faktagrunnlagService.faktagrunnlagTilTekst(faktagrunnlag, språk)

        return faktagrunnlagTekst.entries.map { faktagrunnlag ->
            FaktagrunnlagMedVerdi(
                faktagrunnlag.key.name,
                faktagrunnlag.value
            )
        }
    }

    private fun utledPeriodetekster(brevmal: Brevmal, periodiserteFaktagrunnlag: List<Any>): List<Brevdata.Periodetekst> {
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

    fun validerAutomatiskFerdigstilling(brevbestillingReferanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brevmal = checkNotNull(bestilling.brevmal).tilBrevmal()
        val brevdata = bestilling.brevdata
        val feilmelding =
            "Kan ikke automatisk ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}"

        valider(brevmal.kanSendesAutomatisk) {
            "$feilmelding: Brevmal er ikke konfigurert til at brevet kan sendes automatisk."
        }

        valider(brevmal.delmaler.all { it.obligatorisk }) {
            "$feilmelding: Det er delmaler som ikke er obligatorisk."
        }

        validerFaktagrunnlag(brevmal.delmaler, brevdata, feilmelding)

        val alleTeksteditorElementer = brevmal.delmaler.flatMap { it.delmal.teksteditor }

        valider(alleTeksteditorElementer.filterIsInstance<Brevmal.TeksteditorElement.Valg>().isEmpty()) {
            "$feilmelding: Det er delmaler som inneholder valg."
        }

        valider(alleTeksteditorElementer.filterIsInstance<Brevmal.TeksteditorElement.Fritekst>().isEmpty()) {
            "$feilmelding: Det er delmaler som inneholder fritekst."
        }

        valider(alleTeksteditorElementer.filterIsInstance<Brevmal.TeksteditorElement.BetingetTekst>().isEmpty()) {
            "$feilmelding: Det er delmaler som inneholder betinget tekst."
        }

        valider(alleTeksteditorElementer.filterIsInstance<Brevmal.TeksteditorElement.BetingetTekst>().isEmpty()) {
            "$feilmelding: Det er delmaler som inneholder betinget tekst."
        }
    }

    fun validerFerdigstilling(brevbestillingReferanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brevmal = bestilling.brevmal?.tilBrevmal()
        val brevdata = bestilling.brevdata

        checkNotNull(brevmal)
        checkNotNull(brevdata)

        val feilmelding =
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}"

        val valgteDelmaler =
            brevmal.delmaler.filter { delmal -> brevdata.delmaler.map { it.id }.contains(delmal.delmal._id) }

        val manglendeDelmaler = brevmal.delmaler.filter { it.obligatorisk }
            .filterNot { delmal -> brevdata.delmaler.map { it.id }.contains(delmal.delmal._id) }

        valider(manglendeDelmaler.isEmpty()) {
            "$feilmelding: Mangler obligatoriske delmaler med id ${manglendeDelmaler.joinToString(separator = ",") { it.delmal._id }}"
        }

        valgteDelmaler.forEach { delmalValg ->
            val manglendeFritekster = delmalValg.delmal.teksteditor
                .filterIsInstance<Brevmal.TeksteditorElement.Fritekst>()
                .filterNot { brevdata.fritekster.map { it.key }.contains(it._key) }
            valider(manglendeFritekster.isEmpty()) {
                "$feilmelding: Mangler fritekst(er) ${manglendeFritekster.joinToString(separator = ",")} med key for delmale med id ${delmalValg.delmal._id}"
            }
        }
        validerFaktagrunnlag(valgteDelmaler, brevdata, feilmelding)

        val manglendeValg =
            valgteDelmaler
                .flatMap { it.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Valg>() }
                .filter { it.obligatorisk }
                .filterNot { valg ->
                    brevdata.valg.map { it.id }.contains(valg.valg._id)
                }
        valider(manglendeValg.isEmpty()) {
            "$feilmelding: Obligatorisk valg er ikke valgt"
        }
    }

    private fun validerFaktagrunnlag(
        delmaler: List<DelmalValg>,
        brevdata: Brevdata?,
        feilmelding: String
    ) {
        val påkrevdeFaktagrunnlag = finnAllePåkrevdeFaktagrunnlag(delmaler)
        val faktagrunnlagData = brevdata?.faktagrunnlag ?: emptyList()
        val manglendeFaktagrunnlag = påkrevdeFaktagrunnlag.filterNot { påkrevdFaktagrunnlag ->
            faktagrunnlagData.map { it.tekniskNavn }.contains(påkrevdFaktagrunnlag)
        }

        valider(manglendeFaktagrunnlag.isEmpty()) {
            "$feilmelding: Mangler faktagrunnlag for ${manglendeFaktagrunnlag.joinToString(separator = ",")}"
        }
    }

    private fun finnAllePåkrevdeFaktagrunnlag(delmaler: List<DelmalValg>): List<String> {
        return delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.flatMap { teksteditorElement ->
                when (teksteditorElement) {
                    is Brevmal.TeksteditorElement.Block -> {
                        filtrerFaktagrunnlag(teksteditorElement)
                    }

                    is Brevmal.TeksteditorElement.BetingetTekst -> {
                        teksteditorElement.tekst.teksteditor.flatMap { filtrerFaktagrunnlag(it) }
                    }

                    is Brevmal.TeksteditorElement.Valg -> {
                        teksteditorElement.valg.alternativer.filterIsInstance<Brevmal.ValgAlternativ.KategorisertTekst>()
                            .flatMap { it.tekst.teksteditor }
                            .flatMap { filtrerFaktagrunnlag(it) }
                    }

                    is Brevmal.TeksteditorElement.Periodetekst -> {
                        emptyList() // har faktagrunnlag men flere verdier for samme faktagrunnlag
                    }

                    is Brevmal.TeksteditorElement.Fritekst -> {
                        emptyList()
                    }
                }
            }
        }
    }

    private fun filtrerFaktagrunnlag(block: Brevmal.TeksteditorElement.Block): List<String> {
        return block.children.filterIsInstance<Brevmal.BlockChildren.Faktagrunnlag>()
            .map { it.tekniskNavn }
    }
}
