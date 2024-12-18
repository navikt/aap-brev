package no.nav.aap.brev.util.graphql

abstract class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)