package no.nav.aap.brev.bestilling

import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import java.net.URI
import java.time.LocalDate

class SaksbehandlingPdfGenGateway : PdfGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.saksbehandling_pdfgen.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.saksbehandling_pdfgen.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider()
    )

    override fun genererPdf(
        personinfo: Personinfo,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate,
    ): Pdf {
        val uri = baseUri.resolve("/api/v1/genpdf/aap-saksbehandling-pdfgen/brev")
        val httpRequest = PostRequest(
            body = mapPdfBrev(
                personinfo = personinfo,
                saksnummer = saksnummer,
                brev = brev,
                dato = dato,
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/pdf")
            )
        )
        val bytes = client.post(uri, httpRequest, { body, _ ->
            body.readAllBytes()
        })

        require(bytes != null) {
            "Fikk tom respons fra pdfgen"
        }

        return Pdf(bytes)
    }

    private fun mapPdfBrev(
        personinfo: Personinfo,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate,
    ): PdfBrev {
        return PdfBrev(
            mottaker = Mottaker(navn = personinfo.navn, ident = personinfo.fnr),
            saksnummer = saksnummer,
            dato = dato,
            overskrift = brev.overskrift,
            tekstbolker = brev.tekstbolker.map {
                Tekstbolk(
                    overskrift = it.overskrift,
                    innhold = it.innhold.map {
                        Innhold(
                            overskrift = it.overskrift,
                            blokker = it.blokker.map {
                                Blokk(
                                    innhold = it.innhold.mapNotNull {
                                        when (it) {
                                            is BlokkInnhold.FormattertTekst -> FormattertTekst(
                                                tekst = it.tekst,
                                                formattering = it.formattering
                                            )

                                            else -> null
                                        }
                                    },
                                    type = it.type
                                )
                            })
                    })
            },
        )
    }
}