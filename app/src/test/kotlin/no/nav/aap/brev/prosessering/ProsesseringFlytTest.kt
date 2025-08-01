package no.nav.aap.brev.prosessering

import no.nav.aap.brev.prosessering.steg.DistribuerJournalpostSteg
import no.nav.aap.brev.prosessering.steg.FerdigSteg
import no.nav.aap.brev.prosessering.steg.FerdigstillBrevSteg
import no.nav.aap.brev.prosessering.steg.JournalførBrevSteg
import no.nav.aap.brev.prosessering.steg.StarterSteg
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class ProsesseringFlytTest {

    @Test
    fun `builder kaster feilmelding dersom ingen steg`() {
        assertThrows<IllegalStateException> {
            ProsesseringFlyt.Builder().build()
        }
    }

    @Test
    fun `builder feiler dersom man har samme steg flere ganger`() {
        assertThrows<IllegalArgumentException> {
            ProsesseringFlyt.Builder()
                .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
                .med(steg = StarterSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
                .build()
        }
    }

    @Test
    fun `builder feiler dersom man har samme utfall for to steg`() {
        assertThrows<IllegalArgumentException> {
            ProsesseringFlyt.Builder()
                .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
                .med(steg = FerdigSteg, utfall = ProsesseringStatus.STARTET)
                .build()
        }
    }

    @Test
    fun `fra status gir resterende steg`() {
        val flyt = ProsesseringFlyt.Builder()
            .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
            .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
            .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
            .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
            .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
            .build()

        assertThat(
            flyt.fraStatus(ProsesseringStatus.BREV_FERDIGSTILT)
        ).isEqualTo(
            listOf(
                JournalførBrevSteg,
                DistribuerJournalpostSteg,
                FerdigSteg,
            )
        )
    }

    @Test
    fun `fra status der status er null gir alle steg`() {
        val flyt = ProsesseringFlyt.Builder()
            .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
            .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
            .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
            .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
            .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
            .build()

        assertThat(
            flyt.fraStatus(null)
        ).isEqualTo(
            listOf(
                StarterSteg,
                FerdigstillBrevSteg,
                JournalførBrevSteg,
                DistribuerJournalpostSteg,
                FerdigSteg,
            )
        )
    }

    @Test
    fun `fra status der status avbrutt gir ingen steg`() {
        val flyt = ProsesseringFlyt.Builder()
            .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
            .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
            .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
            .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
            .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
            .build()

        assertThat(
            flyt.fraStatus(ProsesseringStatus.AVBRUTT)
        ).isEmpty()
    }

    @Test
    fun `steg til utfall gir definerte utfall for steg`() {
        val flyt = ProsesseringFlyt.Builder()
            .med(steg = StarterSteg, utfall = ProsesseringStatus.STARTET)
            .med(steg = FerdigstillBrevSteg, utfall = ProsesseringStatus.BREV_FERDIGSTILT)
            .med(steg = JournalførBrevSteg, utfall = ProsesseringStatus.JOURNALFORT)
            .med(steg = DistribuerJournalpostSteg, utfall = ProsesseringStatus.DISTRIBUERT)
            .med(steg = FerdigSteg, utfall = ProsesseringStatus.FERDIG)
            .build()

        assertThat(flyt.utfall(StarterSteg)).isEqualTo(ProsesseringStatus.STARTET)
        assertThat(flyt.utfall(FerdigstillBrevSteg)).isEqualTo(ProsesseringStatus.BREV_FERDIGSTILT)
        assertThat(flyt.utfall(JournalførBrevSteg)).isEqualTo(ProsesseringStatus.JOURNALFORT)
        assertThat(flyt.utfall(DistribuerJournalpostSteg)).isEqualTo(ProsesseringStatus.DISTRIBUERT)
        assertThat(flyt.utfall(FerdigSteg)).isEqualTo(ProsesseringStatus.FERDIG)
    }
}
