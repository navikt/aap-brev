package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Formattering
import java.time.LocalDate

data class PdfBrev(
    val mottaker: Mottaker,
    val saksnummer: String,
    val dato: LocalDate,
    val overskrift: String?,
    val tekstbolker: List<Tekstbolk>,
    val enhet: String,
    val saksbehandler: String,
) {
    data class Mottaker(
        val navn: String,
        val ident: String,
        val identType: IdentType
    ) {
        enum class IdentType {
            FNR, HPRNR
        }
    }

    data class Tekstbolk(
        val overskrift: String?,
        val innhold: List<Innhold>,
    )

    data class Innhold(
        val overskrift: String?,
        val blokker: List<Blokk>,
    )

    data class Blokk(
        val innhold: List<FormattertTekst>,
        val type: BlokkType,
    )

    data class FormattertTekst(
        val tekst: String,
        val formattering: List<Formattering>,
    )
}