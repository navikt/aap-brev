package no.nav.aap.brev.kontrakt

class KanDistribuereBrevReponse (
    mottakereDistStatus: List<MottakerDistStatus>
)

data class MottakerDistStatus(
    val mottaker: MottakerDto,
    val kanDistribuere: Boolean
)
