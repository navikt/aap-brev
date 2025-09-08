package no.nav.aap.brev.distribusjon

interface DistribusjonKanalGateway {
    fun bestemDistribusjonskanal(personident: String): Distribusjonskanal?
}
