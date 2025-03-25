package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Formattering

data class PdfBrev(
    val mottaker: Mottaker,
    val saksnummer: String,
    val dato: String,
    val overskrift: String?,
    val tekstbolker: List<Tekstbolk>,
    val signaturer: List<Signatur>,
    val automatisk: Boolean,
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

    data class Signatur(val navn: String, val enhet: String)

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