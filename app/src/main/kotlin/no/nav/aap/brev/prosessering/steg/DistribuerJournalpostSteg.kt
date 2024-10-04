package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.prosessering.steg.StegUtfører.Kontekst
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class DistribuerJournalpostSteg() : StegUtfører {
    private val log = LoggerFactory.getLogger(DistribuerJournalpostSteg::class.java)
    override fun utfør(kontekst: Kontekst): StegResultat {
        log.info("DistribuerJournalpostSteg")
        return StegResultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): StegUtfører {
            return DistribuerJournalpostSteg()
        }
    }
}
