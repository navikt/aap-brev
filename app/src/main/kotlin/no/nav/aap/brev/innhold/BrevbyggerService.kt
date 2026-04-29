package no.nav.aap.brev.innhold

import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.feil.valider
import no.nav.aap.brev.bestilling.Brevmal
import no.nav.aap.brev.bestilling.Brevmal.BlockChildren
import no.nav.aap.brev.bestilling.Brevmal.DelmalValg
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.math.BigDecimal
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

        val kategorier = utledKategorier(faktagrunnlag)
        val delmaler = utledValgteDelmaler(brevmal)
        val faktagrunnlagMedVerdi = utledFaktagrunnlagMedVerdi(faktagrunnlag, bestilling.språk)
        val valg = utledValg(brevmal, kategorier)
        val betingetTekst = utledBetingetTekst(brevmal, kategorier)

        val brevdata = Brevdata(
            delmaler = delmaler,
            faktagrunnlag = faktagrunnlagMedVerdi,
            tabeller = utledTabellerMedVerdi(faktagrunnlag, bestilling.språk),
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
    ): List<Brevdata.Faktagrunnlag> {
        val faktagrunnlagTekst = faktagrunnlagService.faktagrunnlagTilTekst(faktagrunnlag, språk)

        return faktagrunnlagTekst.entries.map { faktagrunnlag ->
            Brevdata.Faktagrunnlag(
                faktagrunnlag.key.name,
                faktagrunnlag.value
            )
        }
    }

    private fun utledTabellerMedVerdi(
        faktagrunnlag: Set<Faktagrunnlag>,
        språk: Språk
    ): List<Brevdata.Tabell> {
        val faktagrunnlagTekst = faktagrunnlagService.faktagrunnlagTilTabeller(faktagrunnlag, språk)
        TODO()
    }

    private fun utledKategorier(faktagrunnlag: Set<Faktagrunnlag>): Set<KjentKategori> {
        return faktagrunnlag.flatMap { faktagrunnlag ->
            when (faktagrunnlag) {
                is Faktagrunnlag.TilkjentYtelse -> {
                    buildSet {
                        leggTilHvis(KjentKategori.HAR_BARNETILLEGG) {
                            (faktagrunnlag.barnetillegg ?: BigDecimal.ZERO) > BigDecimal.ZERO
                        }
                    }
                }

                is Faktagrunnlag.ForholdTilAndreYtelser -> {
                    buildSet {
                        leggTilHvis(KjentKategori.HAR_FRADRAG_ANDRE_YTELSER) { faktagrunnlag.fradragAndreYtelser.isNotEmpty() }
                        leggTilHvis(KjentKategori.HAR_REDUKSJON_ARBEIDSGIVER) { faktagrunnlag.reduksjonArbeidsgiver.isNotEmpty() }
                        leggTilHvis(KjentKategori.HAR_REFUSJONSKRAV_TJENESTEPENSJON) { faktagrunnlag.refusjonskravTjenestepensjon != null }
                        leggTilHvis(KjentKategori.HAR_SAMORDNING_ANDRE_YTELSER) { faktagrunnlag.samordningAndreYtelser.isNotEmpty() }
                        leggTilHvis(KjentKategori.HAR_SAMORDNING_BARNEPENSJON) { faktagrunnlag.samordningBarnepensjon.isNotEmpty() }
                        leggTilHvis(KjentKategori.HAR_SAMORDNING_UFØRE) { faktagrunnlag.samordningUføre.isNotEmpty() }
                        leggTilHvis(KjentKategori.HAR_SYKESTIPEND) { faktagrunnlag.sykestipend.isNotEmpty() }
                    }
                }

                else -> emptySet()
            }
        }.toSet()
    }

    private fun MutableSet<KjentKategori>.leggTilHvis(kategori: KjentKategori, predikat: () -> Boolean) {
        if (predikat()) {
            add(kategori)
        }
    }

    private fun utledValg(brevmal: Brevmal, kategorier: Set<KjentKategori>): List<Brevdata.Valg> {
        return brevmal.delmaler.flatMap { delmalValg ->
            delmalValg.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Valg>()
                .mapNotNull { valg ->
                    val forhåndsvalgt =
                        valg.valg.alternativer.filterIsInstance<Brevmal.ValgAlternativ.KategorisertTekst>()
                            .find { valgAlternativ ->
                                kategorier.map { it.name }.contains(valgAlternativ.kategori?.tekniskNavn)
                            } ?: return@mapNotNull null
                    Brevdata.Valg(id = valg.valg._id, forhåndsvalgt._key)
                }
        }
    }

    private fun utledBetingetTekst(brevmal: Brevmal, kategorier: Set<KjentKategori>): List<Brevdata.BetingetTekst> {
        return brevmal.delmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.BetingetTekst>()
                .mapNotNull { betingetTekst ->
                    if (betingetTekst.kategorier.orEmpty().map { it.tekniskNavn }
                            .intersect(kategorier.map { it.name }.toSet())
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
            "Kan ikke automatisk ferdigstille brevbestilling"

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
    }

    fun validerFerdigstilling(brevbestillingReferanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val brevmal = bestilling.brevmal?.tilBrevmal()
        val brevdata = bestilling.brevdata

        checkNotNull(brevmal)
        checkNotNull(brevdata)

        val feilmelding =
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}. Validering av brevinnhold feilet"

        val valgteDelmaler =
            brevmal.delmaler.filter { delmal -> brevdata.delmaler.map { it.id }.contains(delmal.delmal._id) }

        val manglendeDelmaler = brevmal.delmaler.filter { it.obligatorisk }
            .filterNot { delmal -> brevdata.delmaler.map { it.id }.contains(delmal.delmal._id) }

        valider(manglendeDelmaler.isEmpty()) {
            "$feilmelding: Mangler obligatoriske delmaler med id ${manglendeDelmaler.joinToString(separator = ",") { it.delmal._id }}."
        }

        valgteDelmaler.forEach { delmalValg ->
            val manglendeFritekster = delmalValg.delmal.teksteditor
                .filterIsInstance<Brevmal.TeksteditorElement.Fritekst>()
                .filterNot { brevdata.fritekster.map { it.key }.contains(it._key) }
            valider(manglendeFritekster.isEmpty()) {
                "$feilmelding: Mangler fritekst(er) med key ${
                    manglendeFritekster.joinToString(
                        transform = { it._key },
                        separator = ","
                    )
                } for delmal med id ${delmalValg.delmal._id}."
            }
        }
        validerFaktagrunnlag(brevmal.delmaler, brevdata, feilmelding)

        val manglendeValg =
            valgteDelmaler
                .flatMap { it.delmal.teksteditor.filterIsInstance<Brevmal.TeksteditorElement.Valg>() }
                .filter { it.obligatorisk }
                .filterNot { valg ->
                    brevdata.valg.map { it.id }.contains(valg.valg._id)
                }
        valider(manglendeValg.isEmpty()) {
            "$feilmelding: Obligatorisk(e) valg med id ${
                manglendeValg.joinToString(
                    separator = ",",
                    transform = { it.valg._id })
            } er ikke valgt."
        }
    }

    private fun validerFaktagrunnlag(
        delmaler: List<DelmalValg>,
        brevdata: Brevdata?,
        feilmelding: String
    ) {
        val påkrevdeFaktagrunnlag = finnAllePåkrevdeFaktagrunnlag(delmaler, brevdata)
        val faktagrunnlagData = brevdata?.faktagrunnlag ?: emptyList()
        val manglendeFaktagrunnlag = påkrevdeFaktagrunnlag.filterNot { påkrevdFaktagrunnlag ->
            faktagrunnlagData.map { it.tekniskNavn }.contains(påkrevdFaktagrunnlag)
        }

        valider(manglendeFaktagrunnlag.isEmpty()) {
            "$feilmelding: Mangler faktagrunnlag for ${manglendeFaktagrunnlag.joinToString(separator = ",")}."
        }
    }

    private fun finnAllePåkrevdeFaktagrunnlag(delmaler: List<DelmalValg>, brevdata: Brevdata?): List<String> {
        val valgteDelmaler = brevdata?.delmaler ?: emptyList()
        val relevanteDelmaler =
            delmaler.filter { delmal -> valgteDelmaler.map { it.id }.contains(delmal.delmal._id) }
        return relevanteDelmaler.flatMap { valgtDelmal ->
            valgtDelmal.delmal.teksteditor.flatMap { teksteditorElement ->
                when (teksteditorElement) {
                    is Brevmal.TeksteditorElement.Block -> {
                        filtrerFaktagrunnlag(teksteditorElement)
                    }

                    is Brevmal.TeksteditorElement.BetingetTekst -> {
                        teksteditorElement.tekst.teksteditor.flatMap { filtrerFaktagrunnlag(it) }
                    }

                    is Brevmal.TeksteditorElement.Valg -> {
                        val valgData = brevdata?.valg?.find { it.id == teksteditorElement.valg._id }
                        if (valgData != null) {
                            teksteditorElement.valg.alternativer
                                .filterIsInstance<Brevmal.ValgAlternativ.KategorisertTekst>()
                                .find { it._key == valgData.key }
                                ?.tekst?.teksteditor
                                ?.flatMap { filtrerFaktagrunnlag(it) } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }

                    is Brevmal.TeksteditorElement.Fritekst -> {
                        emptyList()
                    }
                }
            }
        }
    }

    private fun filtrerFaktagrunnlag(block: Brevmal.TeksteditorElement.Block): List<String> {
        return block.children.filterIsInstance<BlockChildren.Faktagrunnlag>()
            .map { it.tekniskNavn }
    }
}
