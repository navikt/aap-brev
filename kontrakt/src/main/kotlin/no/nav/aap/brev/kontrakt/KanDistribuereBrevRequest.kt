package no.nav.aap.brev.kontrakt

public data class KanDistribuereBrevRequest (
    val brukerIdent: String,
    val mottakerIdentListe: List<String> = emptyList(),
)

public data class KanDistribuereBrevV2Request (
    val mottakerId: String
)
