package no.nav.aap.brev.distribusjon

enum class Distribusjonskanal(s: UtsendingkanalCode) {
    PRINT(UtsendingkanalCode.S),
    SDP(UtsendingkanalCode.SDP),
    DITT_NAV(UtsendingkanalCode.NAV_NO),
    LOKAL_PRINT(UtsendingkanalCode.L),
    INGEN_DISTRIBUSJON(UtsendingkanalCode.INGEN_DISTRIBUSJON),
    TRYGDERETTEN(UtsendingkanalCode.TRYGDERETTEN),
    DPVT(UtsendingkanalCode.DPVT);

    private val utsendingskanalCode: UtsendingkanalCode? = null
}
