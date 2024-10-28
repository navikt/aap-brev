package no.nav.aap.brev.api

import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
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
        ProsesseringStatus.DISTRIBUERT,
        ProsesseringStatus.FERDIG -> Status.FERDIGSTILT
    }
