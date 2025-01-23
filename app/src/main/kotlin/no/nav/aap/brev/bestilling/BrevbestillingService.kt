package no.nav.aap.brev.bestilling

import no.nav.aap.brev.arkivoppslag.ArkivoppslagGateway
import no.nav.aap.brev.arkivoppslag.SafGateway
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.finnFaktagrunnlag
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører
import no.nav.aap.brev.prosessering.ProsesserBrevbestillingJobbUtfører.Companion.BESTILLING_REFERANSE_PARAMETER_NAVN
import no.nav.aap.brev.prosessering.ProsesseringStatus
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
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): OpprettBrevbestillingResultat {
        val eksisterendeBestilling = brevbestillingRepository.hent(unikReferanse)
        if (eksisterendeBestilling != null) {
            if (erDuplikatBestilling(
                    eksisterendeBestilling = eksisterendeBestilling,
                    saksnummer = saksnummer,
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = unikReferanse,
                    brevtype = brevtype,
                    språk = språk,
                    vedlegg = vedlegg,
                )
            ) {
                log.info("Fant eksisterende bestilling med referanse = ${eksisterendeBestilling.referanse.referanse}")
                return OpprettBrevbestillingResultat(
                    id = eksisterendeBestilling.id,
                    referanse = eksisterendeBestilling.referanse,
                    alleredeOpprettet = true
                )
            } else {
                throw IllegalStateException("Bestilling med unikReferanse=${unikReferanse.referanse} finnnes allerede, men er ikke samme bestilling.")
            }
        }

        validerBestilling(saksnummer, vedlegg)

        val bestilling = brevbestillingRepository.opprettBestilling(
            saksnummer = saksnummer,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg,
        )

        leggTilJobb(bestilling)

        log.info("Bestilling opprettet for ${bestilling.referanse.referanse}")
        return OpprettBrevbestillingResultat(
            id = bestilling.id,
            referanse = bestilling.referanse,
            alleredeOpprettet = false
        )
    }

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return brevbestillingRepository.hent(referanse)
    }

    fun oppdaterBrev(referanse: BrevbestillingReferanse, oppdatertBrev: Brev) {
        brevbestillingRepository.oppdaterBrev(referanse, oppdatertBrev)
    }

    fun ferdigstill(referanse: BrevbestillingReferanse) {
        val bestilling = hent(referanse)

        if (erBestillingAlleredeFerdigstilt(bestilling)) {
            log.warn("Forsøkte å ferdigstille allerede ferdigstilt bestilling.")
            return
        }

        validerFerdigstilling(bestilling)

        leggTilJobb(bestilling)
    }

    private fun validerBestilling(saksnummer: Saksnummer, vedlegg: Set<Vedlegg>) {
        if (vedlegg.isNotEmpty()) {
            vedlegg.forEach { (journalpostId, dokumentInfoId) ->
                val journalpost = arkivoppslagGateway.hentJournalpost(journalpostId)
                val sak = journalpost.sak
                val feilmelding =
                    "Kan ikke legge ved dokument, dokumentInfoId=${dokumentInfoId.id} fra journalpostId=${journalpostId.id} i bestilling for sak ${saksnummer.nummer}"

                valider(
                    sak.fagsakId == saksnummer.nummer &&
                            sak.fagsaksystem == "KELVIN" &&
                            sak.sakstype == "FAGSAK" &&
                            sak.tema == "AAP"
                ) {
                    "$feilmelding: Ulik sak."
                }

                valider(journalpost.brukerHarTilgang) {
                    "$feilmelding: Bruker har ikke tilgang til journalpost."
                }

                valider(journalpost.journalstatus == "FERDIGSTILT" || journalpost.journalstatus == "EKSPEDERT") {
                    "$feilmelding: Feil status ${journalpost.journalstatus}."
                }

                val dokument = journalpost.dokumenter.find { it.dokumentInfoId == dokumentInfoId }
                valider(dokument != null) {
                    "$feilmelding: Fant ikke dokument i journalpost."
                }

                valider(dokument?.dokumentvarianter?.find { it.brukerHarTilgang } != null) {
                    "$feilmelding: Bruker har ikke tilgang til dokumentet."
                }
            }
        }
    }

    private fun erBestillingAlleredeFerdigstilt(bestilling: Brevbestilling): Boolean {
        return bestilling.prosesseringStatus != null &&
                bestilling.prosesseringStatus >= ProsesseringStatus.BREV_FERDIGSTILT
    }

    private fun validerFerdigstilling(bestilling: Brevbestilling) {
        checkNotNull(bestilling.brev)
        checkNotNull(bestilling.prosesseringStatus)

        val feilmelding =
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse}"

        valider(bestilling.prosesseringStatus == ProsesseringStatus.BREVBESTILLING_LØST) {
            "$feilmelding: Bestillingen er i feil status for ferdigstilling, prosesseringStatus=${bestilling.prosesseringStatus}"
        }

        val faktagrunnlag = bestilling.brev.finnFaktagrunnlag()
        valider(faktagrunnlag.isNotEmpty()) {
            val faktagrunnlagString = faktagrunnlag.joinToString(separator = ",", transform = { it.tekniskNavn })
            "$feilmelding: Brevet mangler utfylling av faktagrunnlag med teknisk navn: $faktagrunnlagString."
        }
    }

    private fun valider(value: Boolean, lazyMessage: () -> String) {
        if (!value) {
            val message = lazyMessage()
            throw ValideringsfeilException(message)
        }
    }

    private fun erDuplikatBestilling(
        eksisterendeBestilling: Brevbestilling,
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Boolean {
        return eksisterendeBestilling.saksnummer == saksnummer &&
                eksisterendeBestilling.behandlingReferanse == behandlingReferanse &&
                eksisterendeBestilling.unikReferanse == unikReferanse &&
                eksisterendeBestilling.brevtype == brevtype &&
                eksisterendeBestilling.språk == språk &&
                eksisterendeBestilling.vedlegg.containsAll(vedlegg) &&
                vedlegg.containsAll(eksisterendeBestilling.vedlegg)
    }

    private fun leggTilJobb(bestilling: Brevbestilling) {
        val jobb =
            JobbInput(ProsesserBrevbestillingJobbUtfører)
                .medCallId()
                .forSak(bestilling.id.id)
                .medParameter(BESTILLING_REFERANSE_PARAMETER_NAVN, bestilling.referanse.referanse.toString())

        jobbRepository.leggTil(jobb)
    }

}
