package no.nav.aap.brev.kontrakt

public data class KanDistribuereBrevReponse (
    val mottakereDistStatus: List<MottakerDistStatus>
)

public data class MottakerDistStatus(
    val mottakerIdent: String,
    val kanDistribuere: Boolean
)
