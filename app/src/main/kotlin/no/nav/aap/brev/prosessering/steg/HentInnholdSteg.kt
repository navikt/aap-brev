package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.BrevbestillingRepository
import no.nav.aap.brev.BrevbestillingRepositoryImpl
import no.nav.aap.brev.innhold.BrevinnholdGateway
import no.nav.aap.brev.innhold.SanityBrevinnholdGateway
import no.nav.aap.brev.prosessering.steg.StegUtfører.Kontekst
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentInnholdSteg(
    private val brevinnholdGateway: BrevinnholdGateway,
    private val brevbestillingRepository: BrevbestillingRepository
) : StegUtfører {
    private val log = LoggerFactory.getLogger(HentInnholdSteg::class.java)
    override fun utfør(kontekst: Kontekst): StegResultat {
        log.info("Henter brevinnhold for bestillingsreferanse=${kontekst.referanse}")

        val bestilling = brevbestillingRepository.hent(kontekst.referanse)
        val brev = brevinnholdGateway.hentBrevmal(bestilling.brevtype, bestilling.språk)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)

        return StegResultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): StegUtfører {
            return HentInnholdSteg(
                brevinnholdGateway = SanityBrevinnholdGateway(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
            )
        }
    }
}
