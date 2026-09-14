package no.nav.aap.brev.kontrakt

public data class HentSignaturerRequest(
    val brukerIdent: String,
    val brevtype: Brevtype,
    val signaturGrunnlag: List<SignaturGrunnlag>
)
