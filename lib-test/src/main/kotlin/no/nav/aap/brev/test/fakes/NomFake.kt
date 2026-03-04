package no.nav.aap.brev.test.fakes

import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.brev.organisasjon.NomData
import no.nav.aap.brev.organisasjon.NomDataRessurs
import no.nav.aap.brev.organisasjon.NomRessursVariables
import no.nav.aap.brev.organisasjon.OrgEnhet
import no.nav.aap.brev.organisasjon.OrgTilknytning
import no.nav.aap.brev.util.graphql.GraphQLResponse
import no.nav.aap.brev.util.graphql.GraphqlRequest
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDate
import kotlin.random.Random

private val navIdentTilNomData = mutableMapOf<String, NomData>()
private var defaultManglerData = true


fun nomDataForNavIdent(navIdent: String, nomData: NomData) {
    navIdentTilNomData.put(navIdent, nomData)
}

fun nomDataForAlleIdenter() {
    defaultManglerData = false
}

fun Application.nomFake() {
    applicationFakeFelles("nom")
    routing {
        post("/graphql") {
            val request = DefaultJsonMapper.fromJson<GraphqlRequest<NomRessursVariables>>(call.receiveText())
            val navIdent = request.variables.navIdent
            val data = navIdentTilNomData[navIdent] ?: if (!defaultManglerData) {
                NomData(
                    NomDataRessurs(
                        orgTilknytninger = listOf(
                            OrgTilknytning(
                                orgEnhet = OrgEnhet(Random.nextLong(1000, 9999).toString()),
                                erDagligOppfolging = true,
                                gyldigFom = LocalDate.MIN,
                                gyldigTom = null
                            )
                        ), visningsnavn = "f$navIdent e$navIdent"
                    )
                ).also {
                    nomDataForNavIdent(navIdent, it)
                }
            } else {
                null
            }
            val response = GraphQLResponse(
                data,
                emptyList()
            )

            call.respond(response)
        }
    }
}
