package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.innhold.BrevinnholdGateway
import no.nav.aap.brev.innhold.SanityBrevinnholdGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class HentInnholdSteg(
    private val brevinnholdGateway: BrevinnholdGateway,
    private val brevbestillingRepository: BrevbestillingRepository
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(HentInnholdSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("Henter brevinnhold for bestillingsreferanse=${kontekst.referanse}")

        val bestilling = brevbestillingRepository.hent(kontekst.referanse)
        val brev = brevinnholdGateway.hentBrevmal(bestilling.brevtype, bestilling.språk)

        brevbestillingRepository.oppdaterBrev(bestilling.referanse, brev)


        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): Steg.Utfører {
            return HentInnholdSteg(
                brevinnholdGateway = SanityBrevinnholdGateway(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
            )
        }
    }
}
