package no.nav.aap.brev.bestilling

data class OpprettBrevbestillingResultat(
    val id: BrevbestillingId,
    val referanse: BrevbestillingReferanse,
    val alleredeOpprettet: Boolean
)