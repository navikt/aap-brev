package no.nav.aap.brev.kontrakt

class KanDistribuereBrevRequest (
    val brukerIdent: String,
    val mottakerIdentListe: List<String> = emptyList(),
)
