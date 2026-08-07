package no.nav.aap.brev.prosessering

import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.motor.JobbInput
import java.util.UUID

private const val BESTILLING_REFERANSE_KEY = "referanse"

fun JobbInput.medBestillingsreferanse(referanse: BrevbestillingReferanse): JobbInput =
    this.medParameter(BESTILLING_REFERANSE_KEY, referanse.referanse.toString())

fun JobbInput.bestillingsreferanseOrNull(): BrevbestillingReferanse? =
    this.optionalParameter(BESTILLING_REFERANSE_KEY)?.let { BrevbestillingReferanse(UUID.fromString(it)) }

fun JobbInput.bestillingsreferanse(): BrevbestillingReferanse =
    BrevbestillingReferanse(UUID.fromString(this.parameter(BESTILLING_REFERANSE_KEY)))
