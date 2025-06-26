package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.IdentType
import no.nav.aap.brev.bestilling.Mottaker
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.prosessering.ProsesseringStatus

fun Brevbestilling.tilResponse(): BrevbestillingResponse =
    BrevbestillingResponse(
        referanse = referanse.referanse,
        brev = brev,
        opprettet = opprettet,
        oppdatert = oppdatert,
        behandlingReferanse = behandlingReferanse.referanse,
        brevtype = brevtype,
        språk = språk,
        status = utledStatus(prosesseringStatus)
    )

fun utledStatus(prosesseringStatus: ProsesseringStatus?): Status =
    when (prosesseringStatus) {
        null,
        ProsesseringStatus.STARTET,
        ProsesseringStatus.INNHOLD_HENTET,
        ProsesseringStatus.FAKTAGRUNNLAG_HENTET -> Status.REGISTRERT

        ProsesseringStatus.BREVBESTILLING_LØST -> Status.UNDER_ARBEID

        ProsesseringStatus.BREV_FERDIGSTILT,
        ProsesseringStatus.JOURNALFORT,
        ProsesseringStatus.JOURNALPOST_VEDLEGG_TILKNYTTET,
        ProsesseringStatus.JOURNALPOST_FERDIGSTILT,
        ProsesseringStatus.DISTRIBUERT,
        ProsesseringStatus.FERDIG -> Status.FERDIGSTILT

        ProsesseringStatus.AVBRUTT -> Status.AVBRUTT
    }

internal fun MottakerDto.tilMottaker() = Mottaker(
    ident = ident,
    identType = when (identType) {
        null -> null
        else -> IdentType.valueOf(identType!!.name)
    },
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
