package no.nav.aap.brev.kontrakt

class KanDistribuereBrevReponse (
    mottakereDistStatus: List<MottakerDistStatus>
)

data class MottakerDistStatus(
    val mottakerIdent: String,
    val kanDistribuere: Boolean
)
