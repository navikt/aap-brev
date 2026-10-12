package no.nav.aap.brev.kontrakt

public data class OppdaterMottakereRequest(
    val mottaker: MottakerDto,
    val kopimottaker: MottakerDto? = null,
)
