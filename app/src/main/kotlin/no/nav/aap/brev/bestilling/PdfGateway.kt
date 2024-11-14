package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.Brev
import java.time.LocalDate

interface PdfGateway {
    fun genererPdf(
        personinfo: Personinfo,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate
    ): Pdf
}
