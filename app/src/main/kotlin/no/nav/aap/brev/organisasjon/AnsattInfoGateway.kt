package no.nav.aap.brev.organisasjon


interface AnsattInfoGateway {
    fun hentAnsattInfo(navIdent: String): AnsattInfo
}
