package no.nav.aap.brev.kontrakt

data class KanDistribuereBrevRequest (
    val saksnummer: String,
    val brukerIdent: String,
    val mottakerIdentListe: List<String> = emptyList(),
)
