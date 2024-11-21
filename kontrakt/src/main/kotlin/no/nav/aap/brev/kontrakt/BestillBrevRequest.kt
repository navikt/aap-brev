package no.nav.aap.brev.kontrakt

import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.util.*

data class BestillBrevRequest(
    val saksnummer: String,
    val behandlingReferanse: UUID,
    val brevtype: Brevtype,
    val sprak: Språk,
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return null
    }

    override fun hentBehandlingsreferanse(): String {
        return behandlingReferanse.toString()
    }
}
