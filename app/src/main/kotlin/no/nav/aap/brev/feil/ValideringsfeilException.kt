package no.nav.aap.brev.feil

@Deprecated(
    message = "Bruk heller en variant av ApiException (UgyldigForespørselException, Internfeil, etc.)",
    replaceWith = ReplaceWith("no.nav.aap.komponenter.httpklient.exception.ApiException")
)
class ValideringsfeilException(message: String): IllegalStateException(message)