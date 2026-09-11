package no.nav.aap.brev.kontrakt

public data class SignaturGrunnlag(
    val navIdent: String,
    val rolle: Rolle?,
    val enhet: String?
)

public data class Signatur(val navn: String, val enhet: String)

public enum class Rolle {
    KVALITETSSIKRER,
    SAKSBEHANDLER_OPPFOLGING,
    BESLUTTER,
    SAKSBEHANDLER_NASJONAL,
}
