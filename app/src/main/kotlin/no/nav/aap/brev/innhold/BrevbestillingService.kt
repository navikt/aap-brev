package no.nav.aap.brev.innhold

import no.nav.aap.brev.BrevbestillingRepository
import no.nav.aap.brev.BrevbestillingRepositoryImpl
import no.nav.aap.brev.domene.BehandlingReferanse
import no.nav.aap.brev.domene.Brev
import no.nav.aap.brev.domene.Brevbestilling
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.domene.Brevtype
import no.nav.aap.brev.domene.Språk
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class BrevbestillingService(
    private val brevinnholdGateway: BrevinnholdGateway,
    private val brevbestillingRepository: BrevbestillingRepository
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                brevinnholdGateway = SanityBrevinnholdGateway(),
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
            )
        }
    }

    private val log = LoggerFactory.getLogger(BrevbestillingService::class.java)

    fun behandleBrevbestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ): BrevbestillingReferanse {
        log.info("Henter brevinnhold for behandlingReferanse=$behandlingReferanse  brevtype=$brevtype språk=$språk")
        val brev = brevinnholdGateway.hentBrevmal(brevtype, språk)

        return brevbestillingRepository.opprettBestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = brevtype,
            sprak = språk,
            brev = brev,
        )
    }

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return brevbestillingRepository.hent(referanse)
    }

    fun oppdaterBrev(referanse: BrevbestillingReferanse, oppdatertBrev: Brev) {
        brevbestillingRepository.oppdaterBrev(referanse, oppdatertBrev)
    }
}
