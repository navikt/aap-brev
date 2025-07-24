package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigstillJournalpostSteg(
    val journalføringService: JournalføringService,
    val journalpostRepository: JournalpostRepository
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigstillJournalpostSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("FerdigstillJournalpostSteg")
        val journalposter = journalpostRepository.hentAlleFor(kontekst.referanse)
        journalposter.forEach {
            journalføringService.ferdigstillJournalpost(it)
        }
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): FerdigstillJournalpostSteg {
            return FerdigstillJournalpostSteg(
                JournalføringService.konstruer(connection),
                JournalpostRepositoryImpl(connection)
            )
        }
    }
}
