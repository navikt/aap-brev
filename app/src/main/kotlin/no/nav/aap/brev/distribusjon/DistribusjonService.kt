package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class DistribusjonService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val distribusjonGateway: DistribusjonGateway,
    private val adresseGateway: AdresseGateway,
    private val distribusjonskanalGateway: DistribusjonKanalGateway
) {
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

    fun kanBrevDistribueres(personIndent: String): Boolean {
        val kanal = hentDistribusjonskanal(personIndent)
        return (kanal != Distribusjonskanal.PRINT) || (hentPostadresse(personIndent) != null)
    }

    fun hentPostadresse(personIdent: String): Postadresse? {
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
