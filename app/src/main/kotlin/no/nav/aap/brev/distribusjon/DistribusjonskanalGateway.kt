package no.nav.aap.brev.distribusjon

interface DistribusjonskanalGateway {
    fun bestemDistribusjonskanal(brukerId: String, mottakerId: String): Distribusjonskanal?
}
