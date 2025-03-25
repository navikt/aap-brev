package no.nav.aap.brev.organisasjon

import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode

/**
 * NOM har ikke IDA-administrerte Z-identer i dev
 */
class AnsattInfoDevGateway : AnsattInfoGateway {
    override fun hentAnsattInfo(navIdent: String): AnsattInfo {
        check(Miljø.er() == MiljøKode.DEV) {
            "AnsattInfoDevGateway er kun til bruk i dev-miljø"
        }
        return AnsattInfo(
            navIdent = navIdent,
            navn = "F_$navIdent E_$navIdent",
            enhetsnummer = listOf("0313", "0314", "0315", "0316").random(),
        )
    }
}
