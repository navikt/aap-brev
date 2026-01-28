package no.nav.aap.brev.distribusjon

import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest.Distribusjonstidspunkt
import no.nav.aap.brev.distribusjon.DistribuerJournalpostRequest.Distribusjonstype
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.prometheus
import no.nav.aap.brev.util.HåndterConflictResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import java.net.URI

class DokdistfordelingGateway : DistribusjonGateway {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokdistfordeling.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokdistfordeling.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus,
    )

    override fun distribuerJournalpost(
        journalpostId: JournalpostId,
        brevtype: Brevtype,
        mottaker: Mottaker
    ): DistribusjonBestillingId {
        val request = DistribuerJournalpostRequest(
            journalpostId = journalpostId.id,
            bestillendeFagsystem = "KELVIN",
            dokumentProdApp = "KELVIN",
            distribusjonstype = utledDistribusjonstype(brevtype),
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            adresse = mottaker.adresse()
        )
        val httpRequest = PostRequest(
            body = request
        )
        val uri = baseUri.resolve("/rest/v1/distribuerjournalpost")
        val response =
            checkNotNull(client.post<DistribuerJournalpostRequest, DistribuerJournalpostResponse>(uri, httpRequest))
        return DistribusjonBestillingId(response.bestillingsId)
    }

    private fun utledDistribusjonstype(brevtype: Brevtype): Distribusjonstype {
        return when (brevtype) {
            Brevtype.INNVILGELSE,
            Brevtype.VEDTAK_UTVID_VEDTAKSLENGDE,
            Brevtype.AVSLAG,
            Brevtype.VEDTAK_ENDRING,
            Brevtype.BARNETILLEGG_SATS_REGULERING,
            Brevtype.KLAGE_AVVIST,
            Brevtype.VEDTAK_11_7,
            Brevtype.VEDTAK_11_9,
            Brevtype.OMGJØRING_VEDTAK_11_9,
            Brevtype.VEDTAK_11_17,
            Brevtype.VEDTAK_11_18,
            Brevtype.VEDTAK_11_23_SJETTE_LEDD
                -> Distribusjonstype.VEDTAK

            Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            Brevtype.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
            Brevtype.FORVALTNINGSMELDING,
                -> Distribusjonstype.VIKTIG

            Brevtype.KLAGE_MOTTATT,
            Brevtype.KLAGE_TRUKKET,
            Brevtype.VARSEL_OM_BESTILLING,
            Brevtype.KLAGE_OPPRETTHOLDELSE
                -> Distribusjonstype.ANNET
        }
    }
}

data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val bestillendeFagsystem: String,
    val dokumentProdApp: String,
    val distribusjonstype: Distribusjonstype,
    val distribusjonstidspunkt: Distribusjonstidspunkt,
    val adresse: DokdistAdresse? = null,
) {
    enum class Distribusjonstype {
        VEDTAK, VIKTIG, ANNET
    }

    enum class Distribusjonstidspunkt {
        UMIDDELBART, KJERNETID
    }
}

data class DokdistAdresse(
    val adresseType: AdresseType,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String
)

enum class AdresseType {
    norskPostadresse, utenlandskPostadresse
}

internal fun Mottaker.adresse(): DokdistAdresse? {
    if (this.navnOgAdresse == null) {
        return null
    }
    val adressetype = if (this.navnOgAdresse.adresse.landkode == "NO")
        AdresseType.norskPostadresse else AdresseType.utenlandskPostadresse

    this.navnOgAdresse.adresse.let { adresse ->
        return DokdistAdresse(
            adresseType = adressetype,
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 = adresse.adresselinje3,
            postnummer = adresse.postnummer,
            poststed = adresse.poststed,
            land = adresse.landkode
        )
    }
}

data class DistribuerJournalpostResponse(val bestillingsId: String)
