package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.LøsBrevbestillingService
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.steg.Steg.Resultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class LøsBrevbestillingSteg(
    private val løsBrevbestillingService: LøsBrevbestillingService,
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(LøsBrevbestillingSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Resultat {
        log.info("LøsBrevbestillingSteg")

        val status = løsBrevbestillingService.løsBestilling(kontekst.referanse)

        return if (status == Status.FERDIGSTILT) {
            Resultat.FULLFØRT
        } else {
            Resultat.STOPP
        }
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): LøsBrevbestillingSteg {
            return LøsBrevbestillingSteg(
                løsBrevbestillingService = LøsBrevbestillingService.konstruer(connection),
            )
        }
    }
}
