package no.nav.aap.brev.innhold

import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.BlokkInnhold.FormattertTekst
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.util.formaterFullLengde
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
        val faktagrunnlagTyper = brev.kjenteFaktagrunnlag().map { it.tilFaktagrunnlagType() }.toSet()

        if (faktagrunnlagTyper.isEmpty()) {
            return
        }

        val faktagrunnlag = hentFagtagrunnlagGateway.hent(bestilling.behandlingReferanse, faktagrunnlagTyper)

        if (faktagrunnlag.isEmpty()) {
            return
        }

        val faktagrunnlagTekst = faktagrunnlagTilTekst(faktagrunnlag, bestilling.språk)
        val oppdatertBrev = brev.endreBlokkInnhold { erstattFaktagrunnlagMedTekst(it, faktagrunnlagTekst) }

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, oppdatertBrev)
    }

    private fun erstattFaktagrunnlagMedTekst(
        blokkInnhold: BlokkInnhold,
        faktagrunnlagTekst: Map<KjentFaktagrunnlag, String>,
    ): BlokkInnhold =
        when (blokkInnhold) {
            is BlokkInnhold.FormattertTekst -> blokkInnhold
            is BlokkInnhold.Faktagrunnlag ->
                blokkInnhold.kjentFaktagrunnlag()?.let { faktagrunnlagTekst[it] }
                    ?.let { blokkInnhold.tilFormattertTekst(it) }
                    ?: blokkInnhold
        }

    private fun faktagrunnlagTilTekst(faktagrunnlag: Set<Faktagrunnlag>, språk: Språk): Map<KjentFaktagrunnlag, String> {
        return buildMap {
            faktagrunnlag.forEach {
                when (it) {
                    is Faktagrunnlag.Testverdi ->
                        put(KjentFaktagrunnlag.TESTVERDI, it.testString)

                    is Faktagrunnlag.FristDato11_7 ->
                        put(KjentFaktagrunnlag.FRIST_DATO_11_7, it.frist.formaterFullLengde(språk))

                    is Faktagrunnlag.GrunnlagBeregning -> {
                        val sortert = it.inntekterPerÅr.sortedBy { it.år }
                        sortert.getOrNull(0)?.also {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_INNTEKT, it.inntekt.toString())
                        }
                        sortert.getOrNull(1)?.also {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_INNTEKT, it.inntekt.toString())
                        }
                        sortert.getOrNull(2)?.also {
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL, it.år.toString())
                            put(KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT, it.inntekt.toString())
                        }
                    }
                }
            }
        }
    }

    fun KjentFaktagrunnlag.tilFaktagrunnlagType(): FaktagrunnlagType = when (this) {
        KjentFaktagrunnlag.TESTVERDI -> FaktagrunnlagType.TESTVERDI
        KjentFaktagrunnlag.FRIST_DATO_11_7 -> FaktagrunnlagType.FRIST_DATO_11_7
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_AARSTALL,
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_AARSTALL,
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_AARSTALL,
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_1_INNTEKT,
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_2_INNTEKT,
        KjentFaktagrunnlag.GRUNNLAG_BEREGNING_AAR_3_INNTEKT -> FaktagrunnlagType.GRUNNLAG_BEREGNING
    }

    private fun BlokkInnhold.Faktagrunnlag.tilFormattertTekst(tekst: String): FormattertTekst {
        return FormattertTekst(
            id = id,
            tekst = tekst,
            formattering = emptyList()
        )
    }
}
