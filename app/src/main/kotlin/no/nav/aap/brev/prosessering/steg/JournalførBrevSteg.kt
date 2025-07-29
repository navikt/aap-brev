package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.MottakerRepository
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBrevSteg(val journalføringService: JournalføringService, val mottakerRepository: MottakerRepository) :
    Steg.Utfører {
    private val log = LoggerFactory.getLogger(JournalførBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("JournalførBrevSteg")
        val mottakere = mottakerRepository.hentMottakere(kontekst.referanse)
        require(mottakere.size >= 1) {
            "Det må være minst én mottaker for å kunne journalføre brevbestilling."
        }
        journalføringService.journalførBrevbestilling(kontekst.referanse, mottakere)
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): JournalførBrevSteg {
            return JournalførBrevSteg(
                JournalføringService.konstruer(connection),
                MottakerRepositoryImpl(connection)
            )
        }
    }
}
