package no.nav.aap.brev.bestilling

data class OpprettBrevbestillingResultat(
    val brevbestilling: Brevbestilling,
    val alleredeOpprettet: Boolean
)