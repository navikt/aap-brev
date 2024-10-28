package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class LøsBrevbestillingSteg  : Steg.Utfører {
    private val log = LoggerFactory.getLogger(LøsBrevbestillingSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("LøsBrevbestillingSteg")
        // TODO løs brevbestilling mot behandlingsflyt
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return LøsBrevbestillingSteg()
        }
    }
}
