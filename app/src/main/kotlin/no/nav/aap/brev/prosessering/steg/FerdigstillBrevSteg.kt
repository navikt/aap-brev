package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.FerdigstillService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigstillBrevSteg(private val ferdigstillService: FerdigstillService) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigstillBrevSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("Starter ferdigstilling av brev.")
        ferdigstillService.validerFerdigstilling(kontekst.referanse)
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): FerdigstillBrevSteg {
            return FerdigstillBrevSteg(FerdigstillService.konstruer(connection))
        }
    }
}
