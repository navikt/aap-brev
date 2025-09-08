package no.nav.aap.brev.distribusjon

interface DistribusjonskanalGateway {
    fun bestemDistribusjonskanal(personident: String): Distribusjonskanal?
}
