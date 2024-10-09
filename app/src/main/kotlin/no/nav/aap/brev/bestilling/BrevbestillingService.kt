package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører.Companion.BESTILLING_REFERANSE_PARAMETER_NAVN
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

class BrevbestillingService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val jobbRepository: FlytJobbRepository,
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                jobbRepository = FlytJobbRepository(connection),
            )
        }
    }

    private val log = LoggerFactory.getLogger(BrevbestillingService::class.java)

    fun opprettBestilling(
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
    ): BrevbestillingReferanse {

        val referanse = brevbestillingRepository.opprettBestilling(
            behandlingReferanse = behandlingReferanse,
            brevtype = brevtype,
            språk = språk,
        )

        val jobb =
            JobbInput(ProsesserBrevbestillingJobbUtfører)
                .medCallId()
                .medParameter(BESTILLING_REFERANSE_PARAMETER_NAVN, referanse.referanse.toString())

        jobbRepository.leggTil(jobb)

        return referanse
    }

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return brevbestillingRepository.hent(referanse)
    }

    fun oppdaterBrev(referanse: BrevbestillingReferanse, oppdatertBrev: Brev) {
        brevbestillingRepository.oppdaterBrev(referanse, oppdatertBrev)
    }
}
