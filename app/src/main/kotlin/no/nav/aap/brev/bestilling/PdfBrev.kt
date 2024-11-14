package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.BlokkType
import no.nav.aap.brev.kontrakt.Formattering
import java.time.LocalDate

data class PdfBrev(
    val mottaker: Mottaker,
    val saksnummer: Saksnummer,
    val dato: LocalDate,
    val overskrift: String?,
    val tekstbolker: List<Tekstbolk>,
)

data class Mottaker(
    val navn: String,
    val ident: String,
)

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

