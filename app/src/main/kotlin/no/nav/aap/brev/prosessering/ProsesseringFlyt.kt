package no.nav.aap.brev.prosessering

import no.nav.aap.brev.prosessering.steg.Steg

class ProsesseringFlyt private constructor(
    private val rekkefølge: List<Steg>,
    private val stegTilUtfall: HashMap<Steg, ProsesseringStatus>,
    private val utfallTilSteg: HashMap<ProsesseringStatus, Steg>,
) {

    fun fraStatus(prosesseringStatus: ProsesseringStatus?): List<Steg> {
        if (prosesseringStatus == null ||
            // Midlertidig etter fjerning av initielle steg
            prosesseringStatus == ProsesseringStatus.BREVBESTILLING_LØST
        ) {
            return rekkefølge
        }
        if (prosesseringStatus == ProsesseringStatus.AVBRUTT) {
            return emptyList()
        }
        val stegForUtfall = utfallTilSteg[prosesseringStatus]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert steg for status $prosesseringStatus")
        return rekkefølge.dropWhile { it != stegForUtfall }.drop(1)
    }

    fun utfall(steg: Steg): ProsesseringStatus {
        return stegTilUtfall[steg]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert utfall for steg $steg")
    }

    class Builder {
        private val rekkefølge = mutableListOf<Steg>()
        private val stegTilUtfall = mutableMapOf<Steg, ProsesseringStatus>()
        private val utfallTilSteg = mutableMapOf<ProsesseringStatus, Steg>()

        fun med(steg: Steg, utfall: ProsesseringStatus): Builder {
            if (rekkefølge.contains(steg)) {
                throw IllegalArgumentException("Steg $steg er allerede lagt til.")
            }
            if (utfallTilSteg.keys.contains(utfall)) {
                throw IllegalArgumentException("Utfall $utfall er allerede lagt til.")
            }
            rekkefølge.add(steg)
            stegTilUtfall.put(steg, utfall)
            utfallTilSteg.put(utfall, steg)

            return this
        }

        fun build(): ProsesseringFlyt {
            if (rekkefølge.isEmpty()) {
                throw IllegalStateException("Ingen steg å prosessere.")
            }

            return ProsesseringFlyt(
                rekkefølge = rekkefølge,
                stegTilUtfall = HashMap(stegTilUtfall),
                utfallTilSteg = HashMap(utfallTilSteg),
            )
        }
    }
}
