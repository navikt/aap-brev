package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.Brevdata
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.OppdaterBrevdataRequest
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.util.UUID

fun Brevbestilling.tilResponse(): BrevbestillingResponse =
    BrevbestillingResponse(
        referanse = referanse.referanse,
        brev = brev,
        opprettet = opprettet,
        oppdatert = oppdatert,
        behandlingReferanse = behandlingReferanse.referanse,
        brevtype = brevtype,
        språk = språk,
        status = utledStatus(status, prosesseringStatus)
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

internal fun MottakerDto.tilMottaker(bestillingReferanse: UUID, index: Int) = Mottaker(
    ident = ident,
    identType = when (identType) {
        null -> null
        else -> IdentType.valueOf(identType!!.name)
    },
    bestillingMottakerReferanse = "$bestillingReferanse-${index + 1}",
    navnOgAdresse = navnOgAdresse?.let {
        no.nav.aap.brev.bestilling.NavnOgAdresse(
            navn = it.navn,
            adresse = no.nav.aap.brev.bestilling.Adresse(
                landkode = it.adresse.landkode,
                adresselinje1 = it.adresse.adresselinje1,
                adresselinje2 = it.adresse.adresselinje2,
                adresselinje3 = it.adresse.adresselinje3,
                postnummer = it.adresse.postnummer,
                poststed = it.adresse.poststed
            )
        )
    }
)

internal fun List<MottakerDto>.tilMottakere(bestillingReferanse: UUID) = this.mapIndexed { index, mottaker ->
    mottaker.tilMottaker(
        bestillingReferanse = bestillingReferanse,
        index = index
    )
}

fun OppdaterBrevdataRequest.tilBrevdata(): Brevdata {
    return Brevdata(
        delmaler = delmaler.map { delmal -> Brevdata.Delmal(id = delmal.id) },
        faktagrunnlag = faktagrunnlag.map { faktagrunnlagMedVerdi ->
            Brevdata.Faktagrunnlag(
                tekniskNavn = faktagrunnlagMedVerdi.tekniskNavn,
                verdi = faktagrunnlagMedVerdi.verdi
            )
        },
        periodetekster = periodetekster.map { periodetekst ->
            Brevdata.Periodetekst(
                id = periodetekst.id,
                faktagrunnlag = periodetekst.faktagrunnlag.map { faktagrunnlagMedVerdi ->
                    Brevdata.Faktagrunnlag(
                        tekniskNavn = faktagrunnlagMedVerdi.tekniskNavn,
                        verdi = faktagrunnlagMedVerdi.verdi
                    )
                }
            )
        },
        valg = valg.map { valg ->
            Brevdata.Valg(
                id = valg.id,
                key = valg.key,
            )
        },
        betingetTekst = betingetTekst.map { tekst -> Brevdata.BetingetTekst(tekst.id) },
        fritekster = fritekster.map { fritekst ->
            Brevdata.Fritekst(
                parentId = fritekst.parentId,
                key = fritekst.key,
                fritekst = DefaultJsonMapper.fromJson(fritekst.fritekstJson)
            )
        },
    )
}
