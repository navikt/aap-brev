package no.nav.aap.brev.kontrakt

data class KanDistribuereBrevReponse (
    val mottakereDistStatus: List<MottakerDistStatus>
)

data class MottakerDistStatus(
    val mottakerIdent: String,
    val kanDistribuere: Boolean
)
