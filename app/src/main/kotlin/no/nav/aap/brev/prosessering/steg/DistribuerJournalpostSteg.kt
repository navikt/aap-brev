package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.bestilling.MottakerRepository
import no.nav.aap.brev.bestilling.MottakerRepositoryImpl
import no.nav.aap.brev.distribusjon.DistribusjonService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class DistribuerJournalpostSteg(
    val distribusjonService: DistribusjonService,
    val journalpostRepository: JournalpostRepository,
    val mottakerRepository: MottakerRepository
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(DistribuerJournalpostSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("DistribuerJournalpostSteg")
        val mottakere = mottakerRepository.hentMottakere(kontekst.referanse)
        val journalposter = journalpostRepository.hentAlleFor(kontekst.referanse)
        
        // TODO: Forbedre disse, flytt ut
        check(journalposter.all { it.ferdigstilt }) {
            "Feiltilstand: Det finnes journalposter for bestillingen som ikke er journalført."
        }
        check(mottakere.map { it.id }.toSet() == journalposter.map { it.mottaker.id }.toSet()) {
            "Journalposter og mottakere samsvarer ikke"
        }
        
        journalposter.forEach {
            distribusjonService.distribuerBrev(it)

        }
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): DistribuerJournalpostSteg {
            return DistribuerJournalpostSteg(
                DistribusjonService.konstruer(connection), JournalpostRepositoryImpl(connection),
                MottakerRepositoryImpl(connection)
            )
        }
    }
}
