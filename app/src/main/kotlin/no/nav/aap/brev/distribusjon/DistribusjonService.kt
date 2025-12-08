package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.OpprettetJournalpost
import no.nav.aap.komponenter.dbconnect.DBConnection
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

    fun kanBrevDistribueresTilBruker(brukerId: String, mottakerId: String): Boolean {
        return (hentDistribusjonskanal(brukerId, mottakerId) != Distribusjonskanal.PRINT) || (hentPostadresse(mottakerId)?.adresse != null)
    }

    fun hentPostadresse(personIdent: String): HentPostadresseResponse? {
        return adresseGateway.hentPostadresse(personIdent)
    }

    fun hentDistribusjonskanal(brukerId: String, mottakerId: String): Distribusjonskanal? {
        return distribusjonskanalGateway.bestemDistribusjonskanal(brukerId, mottakerId)
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
                if (kanDistribuere(brevbestilling, journalpost)) {
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
                } else {
                    log.info("Kan ikke distribuere brev til bruker pga. manglende adresseinformasjon. Fortsetter uten distribusjon.")
                }
            }
    }

    fun kanDistribuere(brevbestilling: Brevbestilling, journalpost: OpprettetJournalpost): Boolean {
        val brukerIdent = brevbestilling.brukerIdent
        val mottaker = journalpost.mottaker
        val mottakerIdent = mottaker.ident
        val kanDistribuere = (mottaker.identType != IdentType.FNR) || (brukerIdent != null && mottakerIdent != null && kanBrevDistribueresTilBruker(
            brukerIdent,
            mottakerIdent
        ))

        return kanDistribuere
    }
}
