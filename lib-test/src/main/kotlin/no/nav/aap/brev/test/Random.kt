package no.nav.aap.brev.test

import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.bestilling.UnikReferanse
import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.prosessering.ProsesseringStatus
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextULong

fun randomBehandlingReferanse() = BehandlingReferanse(UUID.randomUUID())

fun randomUnikReferanse() = UnikReferanse(UUID.randomUUID().toString())

fun randomSaksnummer() = Saksnummer(Random.nextInt(1000..9999).toString())

fun randomDistribusjonBestillingId(): DistribusjonBestillingId {
    return DistribusjonBestillingId(UUID.randomUUID().toString())
}

fun randomJournalpostId(): JournalpostId {
    return JournalpostId(Random.nextULong().toString())
}

fun randomBrukerIdent(): String {
    return Random.nextLong(10000000000L, 99999999999L).toString()
}

fun randomNavIdent(): String {
    return ('A' .. 'Z').random() + Random.nextLong(100000, 999999).toString()
}

fun randomDokumentInfoId(): DokumentInfoId {
    return DokumentInfoId(Random.nextULong().toString())
}

fun randomBrevtype(): Brevtype {
    return Brevtype.entries.random()
}

fun randomSpråk(): Språk {
    return Språk.entries.random()
}

fun randomProsesseringStatus(): ProsesseringStatus {
    return ProsesseringStatus.entries.random()
}
