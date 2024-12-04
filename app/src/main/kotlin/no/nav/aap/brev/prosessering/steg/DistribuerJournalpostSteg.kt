package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.distribusjon.DistribusjonService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class DistribuerJournalpostSteg(val distribusjonService: DistribusjonService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(DistribuerJournalpostSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("DistribuerJournalpostSteg")
        distribusjonService.distribuerBrev(kontekst.referanse)
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return DistribuerJournalpostSteg(DistribusjonService.konstruer(connection))
        }
    }
}
