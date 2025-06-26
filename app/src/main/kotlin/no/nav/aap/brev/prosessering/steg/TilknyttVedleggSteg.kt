package no.nav.aap.brev.prosessering.steg

import no.nav.aap.brev.bestilling.JournalpostRepository
import no.nav.aap.brev.bestilling.JournalpostRepositoryImpl
import no.nav.aap.brev.journalføring.JournalføringService
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class TilknyttVedleggSteg(
    val journalføringService: JournalføringService,
    val journalpostRepository: JournalpostRepository
) : Steg.Utfører {
    private val log = LoggerFactory.getLogger(TilknyttVedleggSteg::class.java)
    override fun utfør(kontekst: Steg.Kontekst): Steg.Resultat {
        log.info("TilknyttVedleggSteg")
        val journalposter = journalpostRepository.hentAlleFor(kontekst.referanse)

        // TODO: Psas på at denne er idempotent, eller opprett jobber
        journalposter.forEach {
            // Denne er idempotent, men kan være nyttig å opprette jobber i stedet og sjekke status i neste steg?
            journalføringService.tilknyttVedlegg(kontekst.referanse, it.journalpostId)
        }
        return Steg.Resultat.FULLFØRT
    }

    companion object : Steg {
        override fun konstruer(connection: DBConnection): TilknyttVedleggSteg {
            return TilknyttVedleggSteg(
                JournalføringService.konstruer(connection),
                JournalpostRepositoryImpl(connection)
            )
        }
    }
}