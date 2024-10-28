package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.BestillerGateway
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class LøsBrevbestillingSteg(val bestillerGateway: BestillerGateway) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(LøsBrevbestillingSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("LøsBrevbestillingSteg")

        // TODO: Sende riktig status i oppdaterBrevStatus - Er Status.FERDIGSTILT for å forhindre stopp i flyten
        bestillerGateway.oppdaterBrevStatus(kontekst.referanse, Status.FERDIGSTILT)
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return LøsBrevbestillingSteg(bestillerGateway = BehandlingsflytGateway())
        }
    }
}
