package no.nav.aap.brev.bestilling

import no.nav.aap.brev.arkivoppslag.ArkivoppslagGateway
import no.nav.aap.brev.arkivoppslag.SafGateway
import no.nav.aap.brev.exception.ValideringsfeilException
import no.nav.aap.brev.innhold.BrevinnholdService
import no.nav.aap.brev.innhold.FaktagrunnlagService
import no.nav.aap.brev.innhold.alleFaktagrunnlag
import no.nav.aap.brev.innhold.ikkeRedigerbartInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
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
    private val brevinnholdService: BrevinnholdService,
    private val faktagrunnlagService: FaktagrunnlagService,
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                jobbRepository = FlytJobbRepository(connection),
                arkivoppslagGateway = SafGateway(),
                brevinnholdService = BrevinnholdService.konstruer(connection),
                faktagrunnlagService = FaktagrunnlagService.konstruer(connection),
            )
        }
    }

    private val log = LoggerFactory.getLogger(BrevbestillingService::class.java)

    fun opprettBestillingV1(
        saksnummer: Saksnummer,
        brukerIdent: String?,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): OpprettBrevbestillingResultat {
        val resultat = opprettBestilling(
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg,
        )
        if (!resultat.alleredeOpprettet) {
            leggTilJobb(resultat.brevbestilling)
        }
        return resultat
    }

    fun opprettBestillingV2(
        saksnummer: Saksnummer,
        brukerIdent: String?,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        faktagrunnlag: Set<Faktagrunnlag>,
        vedlegg: Set<Vedlegg>,
        ferdigstillAutomatisk: Boolean,
    ): OpprettBrevbestillingResultat {
        val resultat = opprettBestilling(
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg,
        )

        if (resultat.alleredeOpprettet) {
            return resultat
        }

        val referanse = resultat.brevbestilling.referanse

        brevinnholdService.hentOgLagre(referanse)

        faktagrunnlagService.fyllInnFaktagrunnlag(referanse, faktagrunnlag)

        brevbestillingRepository.oppdaterProsesseringStatus(referanse, ProsesseringStatus.BREVBESTILLING_LØST)

        if (ferdigstillAutomatisk) {
            val brev = checkNotNull(brevbestillingRepository.hent(referanse).brev)
            if (brev.kanSendesAutomatisk ?: false) {
                leggTilJobb(resultat.brevbestilling)
            } else {
                throw ValideringsfeilException("Kan ikke ferdigstille brev automatisk")
            }
        }

        return OpprettBrevbestillingResultat(
            brevbestilling = brevbestillingRepository.hent(referanse),
            alleredeOpprettet = false
        )
    }

    private fun opprettBestilling(
        saksnummer: Saksnummer,
        brukerIdent: String?,
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
                    brukerIdent = brukerIdent,
                    behandlingReferanse = behandlingReferanse,
                    unikReferanse = unikReferanse,
                    brevtype = brevtype,
                    språk = språk,
                    vedlegg = vedlegg,
                )
            ) {
                log.info("Fant eksisterende bestilling med referanse=${eksisterendeBestilling.referanse.referanse}")
                return OpprettBrevbestillingResultat(
                    brevbestilling = eksisterendeBestilling,
                    alleredeOpprettet = true
                )
            } else {
                throw IllegalStateException("Bestilling med unikReferanse=${unikReferanse.referanse} finnnes allerede, men er ikke samme bestilling.")
            }
        }

        validerBestilling(saksnummer, vedlegg)

        val bestilling = brevbestillingRepository.opprettBestilling(
            saksnummer = saksnummer,
            brukerIdent = brukerIdent,
            behandlingReferanse = behandlingReferanse,
            unikReferanse = unikReferanse,
            brevtype = brevtype,
            språk = språk,
            vedlegg = vedlegg,
        )

        log.info("Bestilling opprettet med referanse=${bestilling.referanse.referanse}")
        return OpprettBrevbestillingResultat(
            brevbestilling = bestilling,
            alleredeOpprettet = false
        )
    }

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        return brevbestillingRepository.hent(referanse)
    }

    fun oppdaterBrev(referanse: BrevbestillingReferanse, oppdatertBrev: Brev) {
        validerOppdatering(referanse, oppdatertBrev)
        brevbestillingRepository.oppdaterBrev(referanse, oppdatertBrev)
    }

    fun ferdigstill(
        referanse: BrevbestillingReferanse,
        signaturer: List<SignaturGrunnlag>?
    ) {
        val bestilling = hent(referanse)

        if (erBestillingAlleredeFerdigstilt(bestilling)) {
            log.warn("Forsøkte å ferdigstille allerede ferdigstilt bestilling.")
            return
        }

        validerFerdigstilling(bestilling)

        if (signaturer != null) {
            brevbestillingRepository.lagreSignaturer(bestilling.id, signaturer)
        }

        leggTilJobb(bestilling)
    }

    fun avbryt(referanse: BrevbestillingReferanse) {
        val bestilling = hent(referanse)

        valider(kanBestillingAvbrytes(bestilling)) {
            "Kan ikke avbryte brevbestilling med status ${bestilling.prosesseringStatus}"
        }

        brevbestillingRepository.oppdaterProsesseringStatus(referanse, ProsesseringStatus.AVBRUTT)
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
                bestilling.prosesseringStatus >= ProsesseringStatus.BREV_FERDIGSTILT &&
                bestilling.prosesseringStatus != ProsesseringStatus.AVBRUTT
    }

    private fun kanBestillingAvbrytes(bestilling: Brevbestilling): Boolean {
        return bestilling.prosesseringStatus != null &&
                bestilling.prosesseringStatus == ProsesseringStatus.BREVBESTILLING_LØST
    }

    private fun validerFerdigstilling(bestilling: Brevbestilling) {
        checkNotNull(bestilling.brev)
        checkNotNull(bestilling.prosesseringStatus)

        val feilmelding =
            "Kan ikke ferdigstille brevbestilling med referanse=${bestilling.referanse.referanse}"

        valider(bestilling.prosesseringStatus == ProsesseringStatus.BREVBESTILLING_LØST) {
            "$feilmelding: Bestillingen er i feil status for ferdigstilling, prosesseringStatus=${bestilling.prosesseringStatus}"
        }

        val faktagrunnlag = bestilling.brev.alleFaktagrunnlag()
        valider(faktagrunnlag.isEmpty()) {
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
        brukerIdent: String?,
        behandlingReferanse: BehandlingReferanse,
        unikReferanse: UnikReferanse,
        brevtype: Brevtype,
        språk: Språk,
        vedlegg: Set<Vedlegg>,
    ): Boolean {
        return eksisterendeBestilling.saksnummer == saksnummer &&
                eksisterendeBestilling.brukerIdent == brukerIdent &&
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

    private fun validerOppdatering(referanse: BrevbestillingReferanse, oppdatertBrev: Brev) {
        val bestilling = brevbestillingRepository.hent(referanse)
        if (bestilling.prosesseringStatus != ProsesseringStatus.BREVBESTILLING_LØST) {
            throw ValideringsfeilException("Forsøkte å oppdatere brev i bestilling med prosesseringStatus=${bestilling.prosesseringStatus}")
        }
        checkNotNull(bestilling.brev)
        if (bestilling.brev.ikkeRedigerbartInnhold() != oppdatertBrev.ikkeRedigerbartInnhold()) {
//            throw ValideringsfeilException("Forsøkte å oppdatere deler av brevet som ikke er redigerbart")
            log.warn("Forsøkte å oppdatere deler av brevet som ikke er redigerbart") // TODO midlertidig bare logging for testing
        }
    }
}
