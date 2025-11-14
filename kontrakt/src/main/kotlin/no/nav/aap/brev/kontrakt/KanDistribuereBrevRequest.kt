package no.nav.aap.brev.kontrakt

data class KanDistribuereBrevRequest (
    val brukerIdent: String,
    val mottakerIdentListe: List<String> = emptyList(),
)
