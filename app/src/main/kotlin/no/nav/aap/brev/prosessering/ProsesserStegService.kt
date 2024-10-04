package no.nav.aap.brev.prosessering

import no.nav.aap.brev.BrevbestillingRepositoryImpl
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.ProsesseringStatus
import no.nav.aap.brev.prosessering.steg.DistribuerJournalpostSteg
import no.nav.aap.brev.prosessering.steg.FerdigSteg
import no.nav.aap.brev.prosessering.steg.FerdigstillBrevSteg
import no.nav.aap.brev.prosessering.steg.HentFaktagrunnlagSteg
import no.nav.aap.brev.prosessering.steg.HentInnholdSteg
import no.nav.aap.brev.prosessering.steg.JournalførBrevSteg
import no.nav.aap.brev.prosessering.steg.StarterSteg
import no.nav.aap.brev.prosessering.steg.Steg
import no.nav.aap.brev.prosessering.steg.StegResultat
import no.nav.aap.brev.prosessering.steg.StegUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class ProsesserStegService(
    private val connection: DBConnection
) {
    private val log = LoggerFactory.getLogger(ProsesserStegService::class.java)
    private val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

    private val flyt = ProsesseringFlyt.Builder()
        .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
        .med(steg = HentInnholdSteg, utfall = ProsesseringStatus.INNHOLD_HENTET)
        .med(steg = HentFaktagrunnlagSteg, utfall = ProsesseringStatus.FAKTAGRUNNLAG_HENTET)
        .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
        .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
        .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
        .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
        .build()

    fun prosesserBestilling(referanse: BrevbestillingReferanse) {

        val bestilling = brevbestillingRepository.hent(referanse)

        prosesserTilStop(
            kontekst = StegUtfører.Kontekst(referanse),
            stegene = flyt.fraStatus(bestilling.prosesseringStatus),
        )
    }

    private fun prosesserTilStop(kontekst: StegUtfører.Kontekst, stegene: List<Steg>) {
        stegene.forEach { steg ->
            val stegResultat = steg.konstruer(connection).utfør(kontekst)

            if (stegResultat == StegResultat.STOPP) {
                return
            }

            brevbestillingRepository.oppdaterProsesseringStatus(kontekst.referanse, flyt.utfall(steg))

            connection.markerSavepoint()
        }
    }

}

class ProsesseringFlyt private constructor(
    private val rekkefølge: List<Steg>,
    private val stegTilUtfall: HashMap<Steg, ProsesseringStatus>,
    private val utfallTilSteg: HashMap<ProsesseringStatus, Steg>,
) {

    fun fraStatus(prosesseringStatus: ProsesseringStatus?): List<Steg> {
        if (prosesseringStatus == null) {
            return rekkefølge
        }
        val stegForUtfall = utfallTilSteg[prosesseringStatus]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert steg for status $prosesseringStatus")
        return rekkefølge.dropWhile { it != stegForUtfall }.drop(1)
    }

    fun utfall(steg: Steg): ProsesseringStatus {
        return stegTilUtfall[steg]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert utfall for steg $steg")
    }

    class Builder {
        private val rekkefølge = mutableListOf<Steg>()
        private val stegTilUtfall = mutableMapOf<Steg, ProsesseringStatus>()
        private val utfallTilSteg = mutableMapOf<ProsesseringStatus, Steg>()

        fun med(steg: Steg, utfall: ProsesseringStatus): Builder {
            if (rekkefølge.contains(steg)) {
                throw IllegalStateException("Steg $steg er allerede lagt til: $rekkefølge")
            }
            rekkefølge.add(steg)
            stegTilUtfall.put(steg, utfall)
            utfallTilSteg.put(utfall, steg)

            return this
        }

        fun build(): ProsesseringFlyt {
            if (rekkefølge.isEmpty()) {
                throw IllegalStateException("Ingen steg å prosessere.")
            }

            return ProsesseringFlyt(
                rekkefølge = rekkefølge,
                stegTilUtfall = HashMap(stegTilUtfall),
                utfallTilSteg = HashMap(utfallTilSteg),
            )
        }
    }
}
