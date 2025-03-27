package no.nav.aap.brev.kontrakt

import java.util.UUID

data class FerdigstillBrevRequest(
    val referanse: UUID,
    val signaturer: List<SignaturGrunnlag>?
)
