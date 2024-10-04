package no.nav.aap.brev.prosessering.steg

import no.nav.aap.komponenter.dbconnect.DBConnection

sealed interface Steg {
    fun konstruer(connection: DBConnection): StegUtfører
}
