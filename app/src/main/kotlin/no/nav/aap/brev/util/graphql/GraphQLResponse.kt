package no.nav.aap.brev.util.graphql

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)