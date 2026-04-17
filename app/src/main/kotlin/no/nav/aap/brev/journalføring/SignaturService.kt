package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.Personinfo
import no.nav.aap.brev.bestilling.SorterbarSignatur
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Signatur
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

    private val NAY_AAP_ENHET = "4491"

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
        if (personinfo.harStrengtFortroligAdresse || sorterbareSignaturer.isEmpty()) {
            return emptyList()
        }

        val sorterteSignaturer = sorterbareSignaturer.sortedBy { it.sorteringsnøkkel }
        val navIdentTilAnsattInfo = sorterteSignaturer.associate { signatur ->
            signatur.navIdent to ansattInfoGateway.hentAnsattInfo(signatur.navIdent)
        }

        val navIdentTilEnhet = sorterteSignaturer.associate { signatur ->
            if (signatur.enhet == null || signatur.enhet == NAY_AAP_ENHET) {
                signatur.navIdent to navIdentTilAnsattInfo.getValue(signatur.navIdent).enhetsnummer
            } else {
                signatur.navIdent to signatur.enhet
            }
        }

        val enheter = enhetGateway.hentEnheter(navIdentTilEnhet.values.toList()).associateBy { it.enhetsNummer }

        return sorterteSignaturer.map { signatur ->
            val enhetsnummer = navIdentTilEnhet.getValue(signatur.navIdent)
            val enhet = enheter.getValue(enhetsnummer)
            Signatur(
                navn = navIdentTilAnsattInfo.getValue(signatur.navIdent).navn,
                enhet = if (brukEnhetsTypeNavn(brevtype)) enhet.enhetstypeNavn else enhet.navn
            )
        }
    }

    private fun brukEnhetsTypeNavn(brevtype: Brevtype): Boolean {
        return when (brevtype) {
            Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            Brevtype.FORVALTNINGSMELDING,
            Brevtype.KLAGE_MOTTATT,
            Brevtype.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                -> {
                true
            }

            Brevtype.VEDTAK_ENDRING,
            Brevtype.BARNETILLEGG_SATS_REGULERING,
            Brevtype.VARSEL_OM_BESTILLING,
            Brevtype.AVSLAG,
            Brevtype.INNVILGELSE,
            Brevtype.KLAGE_AVVIST,
            Brevtype.KLAGE_OPPRETTHOLDELSE,
            Brevtype.KLAGE_TRUKKET,
            Brevtype.VEDTAK_11_7,
            Brevtype.VEDTAK_11_9,
            Brevtype.OMGJØRING_VEDTAK_11_9,
            Brevtype.VEDTAK_11_17,
            Brevtype.VEDTAK_11_18,
            Brevtype.VEDTAK_11_23_SJETTE_LEDD,
            Brevtype.VEDTAK_UTVID_VEDTAKSLENGDE,
            Brevtype.STANS_AV_YTELSE,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_MEDLEMSKAP,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_3,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_4,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_12,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_26,
            Brevtype.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_27,
                -> {
                false
            }
        }
    }
}
