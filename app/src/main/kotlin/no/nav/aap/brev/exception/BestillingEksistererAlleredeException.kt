package no.nav.aap.brev.exception

class BestillingEksistererAlleredeException(cause: Throwable) : RuntimeException("Bestilling eksisterer allerede.", cause)
