package no.nav.aap.brev.bestilling

import no.nav.aap.brev.arkivoppslag.ArkivoppslagGateway
import no.nav.aap.brev.arkivoppslag.SafGateway
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører.Companion.BESTILLING_REFERANSE_PARAMETER_NAVN
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

class BrevbestillingService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val jobbRepository: FlytJobbRepository,
    private val arkivoppslagGateway: ArkivoppslagGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                jobbRepository = FlytJobbRepository(connection),
                arkivoppslagGateway = SafGateway(),
            )
        }
    }

    private val log = LoggerFactory.getLogger(BrevbestillingService::class.java)

    fun opprettBestilling(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): BrevbestillingReferanse {

        // TODO:
//        validerBestilling(saksnummer, vedlegg)

        val referanse = brevbestillingRepository.opprettBestilling(
            saksnummer = saksnummer,
            behandlingReferanse = behandlingReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg.map {
                Vedlegg(
                    journalpostId = JournalpostId(it.journalpostId),
                    dokumentInfoId = DokumentInfoId(it.dokumentInfoId)
                )
            }.toSet(),
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

    private fun validerBestilling(saksnummer: Saksnummer, vedlegg: Set<Vedlegg>) {
        if (vedlegg.isNotEmpty()) {
            vedlegg.forEach { (journalpostId, dokumentInfoId) ->
                val journalpost = arkivoppslagGateway.hentJournalpost(JournalpostId(journalpostId))
                val sak = journalpost.sak
                val feilmelding =
                    "Kan ikke legge ved dokument, dokumentInfoId=$dokumentInfoId fra journalpostId=$journalpostId i bestilling for sak ${saksnummer.nummer}"

                check(
                    sak.fagsakId == saksnummer.nummer &&
                            sak.fagsaksystem == "KELVIN" &&
                            sak.sakstype == "FAGSAK" &&
                            sak.tema == "AAP"
                ) {
                    "$feilmelding: Ulik sak."
                }

                check(journalpost.brukerHarTilgang) {
                    "$feilmelding: Bruker har ikke tilgang til dokumentet."
                }

                check(journalpost.journalstatus == "FERDIGSTILT" || journalpost.journalstatus == "EKSPEDERT") {
                    "$feilmelding: Feil status ${journalpost.journalstatus}."
                }

                val dokument = journalpost.dokumenter.find { it.dokumentInfoId == dokumentInfoId }
                checkNotNull(dokument) {
                    "$feilmelding: Fant ikke dokument i journalpost."
                }

                check(dokument.dokumentvarianter.find { it.brukerHarTilgang } != null) {
                    "$feilmelding: Bruker har ikke tilgang til dokumentet."
                }
            }
        }
    }
}
