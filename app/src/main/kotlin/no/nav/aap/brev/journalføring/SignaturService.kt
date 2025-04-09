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
import no.nav.aap.brev.organisasjon.EnhetsType
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
        return if (personinfo.harStrengtFortroligAdresse || sorterbareSignaturer.isEmpty()) {
            emptyList()
        } else {
            val sorterteSignaturer = sorterbareSignaturer.sortedBy { it.sorteringsnøkkel }

            val ansattInfoMedRolle: List<AnsattInfoMedRolle> = sorterteSignaturer.map {
                it to ansattInfoGateway.hentAnsattInfo(it.navIdent)
            }.map { (signatur, ansattInfo) ->
                AnsattInfoMedRolle(ansattInfo, signatur.rolle)
            }

            val enheter = enhetGateway.hentEnheter(ansattInfoMedRolle.map { it.ansattInfo.enhetsnummer })

            ansattInfoMedRolle.map { ansattInfoMedRolle ->
                val ansattEnhet = enheter.single { it.enhetsNummer == ansattInfoMedRolle.ansattInfo.enhetsnummer }
                val valgtEnhet =
                    if (ansattEnhet.type == EnhetsType.LOKAL && ansattInfoMedRolle.rolle == Rolle.KVALITETSSIKRER) {
                        enhetGateway.hentOverordnetFylkesenhet(ansattEnhet.enhetsNummer)
                    } else {
                        ansattEnhet
                    }

                Signatur(
                    navn = ansattInfoMedRolle.ansattInfo.navn,
                    enhet = if (brukEnhetsTypeNavn(brevtype)) valgtEnhet.enhetstypeNavn else valgtEnhet.navn
                )
            }
        }
    }

    private fun brukEnhetsTypeNavn(brevtype: Brevtype): Boolean {
        return when (brevtype) {
            Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT, Brevtype.FORVALTNINGSMELDING -> {
                true
            }

            Brevtype.VEDTAK_ENDRING, Brevtype.VARSEL_OM_BESTILLING, Brevtype.AVSLAG, Brevtype.INNVILGELSE -> {
                false
            }
        }
    }

    private data class AnsattInfoMedRolle(val ansattInfo: AnsattInfo, val rolle: Rolle?)
}
