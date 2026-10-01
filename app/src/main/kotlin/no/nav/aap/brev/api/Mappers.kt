package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Adresse
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.bestilling.NavnOgAdresse
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.BrevdataDto
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.UUID

fun Brevbestilling.tilResponse(
    mottaker: MottakerDto? = null,
    kopimottaker: MottakerDto? = null
): BrevbestillingResponse =
    BrevbestillingResponse(
        referanse = referanse.referanse,
        brev = brev,
        brevmal = brevmal?.json?.let { DefaultJsonMapper.toJson(it) },
        brevdata = brevdata?.tilBrevdataDto(),
        opprettet = opprettet,
        oppdatert = oppdatert,
        behandlingReferanse = behandlingReferanse.referanse,
        brevtype = brevtype,
        språk = språk,
        status = utledStatus(status, prosesseringStatus),
        mottaker = mottaker,
        kopimottaker = kopimottaker,
    )

fun utledStatus(status: Status?, prosesseringStatus: ProsesseringStatus?): Status =
    status ?: when (prosesseringStatus) {
        null,
        ProsesseringStatus.BREVBESTILLING_LØST -> Status.UNDER_ARBEID

        ProsesseringStatus.STARTET,
        ProsesseringStatus.BREV_FERDIGSTILT,
        ProsesseringStatus.JOURNALFORT,
        ProsesseringStatus.JOURNALPOST_VEDLEGG_TILKNYTTET,
        ProsesseringStatus.JOURNALPOST_FERDIGSTILT,
        ProsesseringStatus.DISTRIBUERT,
        ProsesseringStatus.FERDIG -> Status.FERDIGSTILT
        ProsesseringStatus.AVBRUTT -> Status.AVBRUTT
    }

internal fun MottakerDto.tilMottaker(bestillingReferanse: UUID, type: Mottaker.Type, index: Int) = Mottaker(
    ident = ident,
    identType = when (identType) {
        null -> null
        else -> IdentType.valueOf(identType!!.name)
    },
    bestillingMottakerReferanse = "$bestillingReferanse-${index + 1}",
    navnOgAdresse = navnOgAdresse?.let {
        NavnOgAdresse(
            navn = it.navn,
            adresse = Adresse(
                landkode = it.adresse.landkode,
                adresselinje1 = it.adresse.adresselinje1,
                adresselinje2 = it.adresse.adresselinje2,
                adresselinje3 = it.adresse.adresselinje3,
                postnummer = it.adresse.postnummer,
                poststed = it.adresse.poststed
            )
        )
    },
    type = type,
)

/**
 * Brukes for den eldre (v2) ferdigstill-flyten, der mottakere fortsatt sendes som liste
 * uten eksplisitt rolle. Første element regnes som hovedperson, resten som kopi.
 */
internal fun List<MottakerDto>.tilMottakere(bestillingReferanse: UUID) = this.mapIndexed { index, mottaker ->
    mottaker.tilMottaker(
        bestillingReferanse = bestillingReferanse,
        type = if (index == 0) Mottaker.Type.HOVED else Mottaker.Type.KOPI,
        index = index
    )
}

internal fun Mottaker.tilMottakerDto() = MottakerDto(
    ident = ident,
    identType = identType?.let { no.nav.aap.brev.kontrakt.IdentType.valueOf(it.name) },
    navnOgAdresse = navnOgAdresse?.let {
        no.nav.aap.brev.kontrakt.NavnOgAdresse(
            navn = it.navn,
            adresse = no.nav.aap.brev.kontrakt.Adresse(
                landkode = it.adresse.landkode,
                adresselinje1 = it.adresse.adresselinje1,
                adresselinje2 = it.adresse.adresselinje2,
                adresselinje3 = it.adresse.adresselinje3,
                postnummer = it.adresse.postnummer,
                poststed = it.adresse.poststed
            )
        )
    },
)

/**
 * Deler opp lagringslisten (rad per mottaker) i de to navngitte feltene som eksponeres i API-et.
 */
internal fun List<Mottaker>.tilMottakerOgKopimottaker(): Pair<MottakerDto?, MottakerDto?> {
    val mottaker = singleOrNull { it.type == Mottaker.Type.HOVED }?.tilMottakerDto()
    val kopimottaker = singleOrNull { it.type == Mottaker.Type.KOPI }?.tilMottakerDto()

    return mottaker to kopimottaker
}

fun Brevdata.tilBrevdataDto(): BrevdataDto {
    return BrevdataDto(
        delmaler = delmaler.map { delmal -> BrevdataDto.Delmal(id = delmal.id) },
        valg = valg.map { valg ->
            BrevdataDto.Valg(
                id = valg.id,
                key = valg.key,
            )
        },
        betingetTekst = betingetTekst.map { tekst -> BrevdataDto.BetingetTekst(tekst.id) },
        fritekster = fritekster.map { fritekst ->
            BrevdataDto.Fritekst(
                parentId = fritekst.parentId,
                key = fritekst.key,
                fritekst = DefaultJsonMapper.toJson(fritekst.fritekst)
            )
        },
    )
}
