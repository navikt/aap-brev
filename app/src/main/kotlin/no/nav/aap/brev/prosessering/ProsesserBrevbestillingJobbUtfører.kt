package no.nav.aap.brev.prosessering

import no.nav.aap.brev.BrevbestillingRepositoryImpl
import no.nav.aap.brev.domene.BrevbestillingReferanse
import no.nav.aap.brev.innhold.SanityBrevinnholdGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.util.UUID

class ProsesserBrevbestillingJobbUtfører(
    private val prosesserStegService: ProsesserStegService,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val referanse = BrevbestillingReferanse(UUID.fromString(input.parameter(BESTILLING_REFERANSE_PARAMETER_NAVN)))

        prosesserStegService.prosesserBestilling(ProsesserStegService.Kontekst(referanse))
    }

    companion object : Jobb {

        const val BESTILLING_REFERANSE_PARAMETER_NAVN = "referanse"

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserBrevbestillingJobbUtfører(
                ProsesserStegService(
                    brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                    brevinnholdGateway = SanityBrevinnholdGateway(),
                )
            )
        }

        override fun type(): String {
            return "prosesserBrevbestilling"
        }

        override fun navn(): String {
            return "Prosesser brevbestilling"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å gjennomføre bestilling av brev"
        }
    }
}