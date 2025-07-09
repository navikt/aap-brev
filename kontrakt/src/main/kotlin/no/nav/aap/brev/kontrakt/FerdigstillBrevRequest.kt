package no.nav.aap.brev.kontrakt

import java.util.UUID

data class FerdigstillBrevRequest(
    val referanse: UUID,
    val signaturer: List<SignaturGrunnlag>?,
    /**
     * Mottakere er en liste over de som skal motta brevet.
     * Hvis listen er tom, vil brevet bli sendt til bruker, 
     * ellers til de som er oppgitt i listen.
     */
    val mottakere: List<MottakerDto> = emptyList()
)
