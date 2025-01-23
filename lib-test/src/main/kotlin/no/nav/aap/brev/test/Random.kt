package no.nav.aap.brev.test

import no.nav.aap.brev.bestilling.BehandlingReferanse
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.bestilling.UnikReferanse
import no.nav.aap.brev.distribusjon.DistribusjonBestillingId
import no.nav.aap.brev.journalføring.DokumentInfoId
import no.nav.aap.brev.journalføring.JournalpostId
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
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

fun randomDokumentInfoId(): DokumentInfoId {
    return DokumentInfoId(Random.nextULong().toString())
}

fun randomBrevtype(): Brevtype {
    return Brevtype.entries.random()
}

fun randomSpråk(): Språk {
    return Språk.entries.random()
}