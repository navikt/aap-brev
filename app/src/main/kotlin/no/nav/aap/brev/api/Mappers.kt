package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.kontrakt.BrevbestillingResponse

fun Brevbestilling.tilResponse(): BrevbestillingResponse =
    BrevbestillingResponse(
        referanse = referanse.referanse,
        brev = brev,
        opprettet = opprettet,
        oppdatert = oppdatert,
        behandlingReferanse = behandlingReferanse.referanse,
        brevtype = brevtype,
        språk = språk,
    )