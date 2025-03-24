package no.nav.aap.brev.organisasjon

import java.time.LocalDate

data class NomData(
    val ressurs: NomDataRessurs?,
)

data class NomDataRessurs(
    val orgTilknytning: List<OrgTilknytning>,
    val visningsnavn: String,
)

data class OrgTilknytning(
    val orgEnhet: OrgEnhet,
    val erDagligOppfolging: Boolean,
    val gyldigFom: LocalDate,
    val gyldigTom: LocalDate?,
)

data class OrgEnhet(val remedyEnhetId: String?)
