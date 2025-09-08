package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import org.slf4j.LoggerFactory

class DistribusjonService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val distribusjonGateway: DistribusjonGateway,
    private val adresseGateway: AdresseGateway,
    private val distribusjonskanalGateway: DistribusjonskanalGateway
) {
    private val log = LoggerFactory.getLogger(DistribusjonService::class.java)

    companion object {
        fun konstruer(connection: DBConnection): DistribusjonService {
            return DistribusjonService(
                BrevbestillingRepositoryImpl(connection),
                JournalpostRepositoryImpl(connection),
                DokdistfordelingGateway(),
                RegoppslagGateway(),
                DokdistkanalGateway()
            )
        }
    }

    fun kanBrevDistribueresTilBruker(personIndent: String): Boolean {
        return (hentDistribusjonskanal(personIndent) != Distribusjonskanal.PRINT) || (hentPostadresse(personIndent) != null)
    }

    fun hentPostadresse(personIdent: String): HentPostadresseResponse? {
        return adresseGateway.hentPostadresse(personIdent)
    }

    fun hentDistribusjonskanal(personIdent: String): Distribusjonskanal? {
        return distribusjonskanalGateway.bestemDistribusjonskanal(personIdent)
    }

    fun distribuerBrev(referanse: BrevbestillingReferanse) {
        val brevbestilling = brevbestillingRepository.hent(referanse)
        val journalposter = journalpostRepository.hentAlleFor(referanse)

        check(journalposter.isNotEmpty()) {
            "Kan ikke distribuere en bestilling som ikke er journalført."
        }

        check(journalposter.all { it.ferdigstilt }) {
            "Feiltilstand: Det finnes journalposter for bestillingen som ikke er ferdigstilt."
        }

        journalposter
            .filter { it.distribusjonBestillingId == null }
            .forEach { journalpost ->
                val mottaker = journalpost.mottaker
                val ident = mottaker.ident
                val kanDistribuere = (mottaker.identType != IdentType.FNR) || (ident != null && kanBrevDistribueresTilBruker(ident))

                // TODO Fjerne logging og feature toggle etter verifisering i prod
                log.info("Kan distribuere brev til bruker: ${kanDistribuere}")
                if (Miljø.erProd() || kanDistribuere) {
                    val distribusjonBestillingId = distribusjonGateway.distribuerJournalpost(
                        journalpost.journalpostId,
                        brevbestilling.brevtype,
                        journalpost.mottaker
                    )
                    // TODO Midlertidig for bakoverkompabilitet
                    if (journalpost.mottaker.ident == brevbestilling.brukerIdent) {
                        brevbestillingRepository.lagreDistribusjonBestilling(brevbestilling.id, distribusjonBestillingId)
                    }
                    journalpostRepository.lagreDistribusjonBestilling(journalpost.journalpostId, distribusjonBestillingId)
                }
            }
    }
}
