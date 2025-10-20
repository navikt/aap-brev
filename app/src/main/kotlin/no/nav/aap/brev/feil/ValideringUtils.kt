package no.nav.aap.brev.feil

fun valider(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        val message = lazyMessage()
        throw ValideringsfeilException(message)
    }
}