package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBrevSteg(val journalføringService: JournalføringService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(JournalførBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("JournalførBrevSteg")
        try { // TODO midlertidig fortsetter vi ved feil slik at det ikke stopper behandlingsflyten
            journalføringService.genererBrevOgJournalfør(kontekst.referanse)
        } catch (e: Exception) {
            log.error("Feil under journalføring", e)
        }
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return JournalførBrevSteg(JournalføringService.konstruer(connection))
        }
    }
}
