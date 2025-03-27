package no.nav.aap.brev.kontrakt

data class HentSignaturerRequest(
    val brukerIdent: String,
    val brevtype: Brevtype,
    val signaturGrunnlag: List<SignaturGrunnlag>
)
