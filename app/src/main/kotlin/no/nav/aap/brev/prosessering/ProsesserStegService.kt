package no.nav.aap.brev.prosessering

import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.prosessering.steg.DistribuerJournalpostSteg
import no.nav.aap.brev.prosessering.steg.FerdigSteg
import no.nav.aap.brev.prosessering.steg.FerdigstillBrevSteg
import no.nav.aap.brev.prosessering.steg.HentFaktagrunnlagSteg
import no.nav.aap.brev.prosessering.steg.HentInnholdSteg
import no.nav.aap.brev.prosessering.steg.JournalførBrevSteg
import no.nav.aap.brev.prosessering.steg.LøsBrevbestillingSteg
import no.nav.aap.brev.prosessering.steg.StarterSteg
import no.nav.aap.brev.prosessering.steg.Steg
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class ProsesserStegService(
    private val connection: DBConnection
) {

    companion object {
        fun konstruer(connection: DBConnection): ProsesserStegService {
            return ProsesserStegService(connection)
        }
    }

    private val log = LoggerFactory.getLogger(ProsesserStegService::class.java)
    private val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

    private val flyt = ProsesseringFlyt.Builder()
        .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
        .med(steg = HentInnholdSteg, utfall = ProsesseringStatus.INNHOLD_HENTET)
        .med(steg = HentFaktagrunnlagSteg, utfall = ProsesseringStatus.FAKTAGRUNNLAG_HENTET)
        .med(steg = LøsBrevbestillingSteg, utfall = ProsesseringStatus.BREVBESTILLING_LØST)
        .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
        .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
        .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
        .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
        .build()

    fun prosesserBestilling(referanse: BrevbestillingReferanse) {

        val bestilling = brevbestillingRepository.hent(referanse)
        val stegene = flyt.fraStatus(bestilling.prosesseringStatus)

        if (stegene.isEmpty()) {
            log.warn("Forsøkte å prosessere bestilling uten flere steg og status ${bestilling.prosesseringStatus}.")
            return
        }

        stegene.forEach { steg ->
            val stegResultat = steg.konstruer(connection).utfør(Steg.Kontekst(referanse))

            if (stegResultat == Steg.Resultat.STOPP) {
                return
            }

            brevbestillingRepository.oppdaterProsesseringStatus(referanse, flyt.utfall(steg))

            connection.markerSavepoint()
        }
    }
}
