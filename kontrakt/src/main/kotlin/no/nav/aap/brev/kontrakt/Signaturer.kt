package no.nav.aap.brev.kontrakt

data class SignaturGrunnlag(
    val navIdent: String,
    val rolle: Rolle?,
    val enhet: String?
)

data class Signatur(val navn: String, val enhet: String)

enum class Rolle {
    KVALITETSSIKRER,
    SAKSBEHANDLER_OPPFOLGING,
    BESLUTTER,
    SAKSBEHANDLER_NASJONAL,
}
