package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.organisasjon.AnsattInfo
import no.nav.aap.brev.organisasjon.AnsattInfoDevGateway
import no.nav.aap.brev.organisasjon.AnsattInfoGateway
import no.nav.aap.brev.organisasjon.EnhetGateway
import no.nav.aap.brev.organisasjon.NomInfoGateway
import no.nav.aap.brev.organisasjon.NorgGateway
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode

class SignaturService(
    val ansattInfoGateway: AnsattInfoGateway,
    val enhetGateway: EnhetGateway,
) {

    companion object {
        fun konstruer(): SignaturService {
            return SignaturService(
                ansattInfoGateway = if (Miljø.er() == MiljøKode.DEV) AnsattInfoDevGateway() else NomInfoGateway(),
                enhetGateway = NorgGateway()
            )
        }
    }

    fun signaturer(
        sorterbareSignaturer: List<SorterbarSignatur>,
        brevtype: Brevtype,
        personinfo: Personinfo
    ): List<Signatur> {
        return if (personinfo.harStrengtFortroligAdresse) {
            emptyList()
        } else {

            val sorterteSignaturer = sorterbareSignaturer.sortedBy { it.sorteringsnøkkel }
            val ansattInfoListe = sorterteSignaturer.map {
                it to ansattInfoGateway.hentAnsattInfo(it.navIdent)
            }.map { (signatur, ansattInfo) ->
                if (signatur.rolle == Rolle.KVALITETSSIKRER && ansattInfo.enhetsnummer != "0393") {
                    ansattInfo.copy(enhetsnummer = ansattInfo.enhetsnummer.dropLast(2) + "00")
                } else {
                    ansattInfo
                }
            }

            val enheter = enhetGateway.hentEnhetsnavn(ansattInfoListe.map { it.enhetsnummer })
            val brukEnhetsTypeNavn = when (brevtype) {
                Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT, Brevtype.FORVALTNINGSMELDING -> {
                    true
                }

                Brevtype.VEDTAK_ENDRING, Brevtype.VARSEL_OM_BESTILLING, Brevtype.AVSLAG, Brevtype.INNVILGELSE -> {
                    false
                }
            }
            ansattInfoListe.map { ansattInfo ->
                val enhet = enheter.single { it.enhetsNummer == ansattInfo.enhetsnummer }
                Signatur(
                    navn = ansattInfo.navn,
                    enhet = if (brukEnhetsTypeNavn) enhet.enhetstypeNavn else enhet.navn
                )
            }
        }
    }
}
