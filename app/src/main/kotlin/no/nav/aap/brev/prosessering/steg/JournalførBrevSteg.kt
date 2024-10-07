package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBrevSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(JournalførBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("JournalførBrevSteg")
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return JournalførBrevSteg()
        }
    }
}
