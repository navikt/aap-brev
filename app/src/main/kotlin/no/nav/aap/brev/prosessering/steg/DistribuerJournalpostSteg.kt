package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class DistribuerJournalpostSteg() : Steg.Utfører {
    private val log = LoggerFactory.getLogger(DistribuerJournalpostSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("DistribuerJournalpostSteg")
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return DistribuerJournalpostSteg()
        }
    }
}
