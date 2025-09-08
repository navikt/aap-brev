package no.nav.aap.brev.distribusjon

data class DistribusjonGrunnlag(
    val brukerId: String,
    val mottakerId: String,
    val tema: String,
    val antallDokumenter: Int?,
    val dokumenttypeId: String?,
    val erArkivert: Boolean?,
    val forsendelseStoerrelse: Int?,
)
