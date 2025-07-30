package no.nav.aap.brev.bestilling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper

interface MottakerRepository {
    fun lagreMottakere(brevbestillingId: BrevbestillingId, mottakere: List<Mottaker>)
    fun hentMottakere(brevbestillingId: BrevbestillingId): List<Mottaker>
    fun hentMottakere(brevbestillingReferanse: BrevbestillingReferanse): List<Mottaker>
}

class MottakerRepositoryImpl(private val connection: DBConnection) : MottakerRepository {

    override fun lagreMottakere(brevbestillingId: BrevbestillingId, mottakere: List<Mottaker>) {
        val eksisterendeMottakere = hentMottakere(brevbestillingId)
        if (eksisterendeMottakere.isNotEmpty()) return
        val query = """
            INSERT INTO MOTTAKER(BREVBESTILLING_ID, IDENT, IDENT_TYPE, NAVN_OG_ADRESSE, BESTILLING_MOTTAKER_REFERANSE) VALUES (?, ?, ?, ?::jsonb, ?)
        """.trimIndent()
        connection.executeBatch(query, mottakere) {
            setParams {
                setLong(1, brevbestillingId.id)
                setString(2, it.ident)
                setEnumName(3, it.identType)
                setString(4, it.navnOgAdresse?.let { n -> DefaultJsonMapper.toJson(n) })
                setString(5, it.bestillingMottakerReferanse)
            }
        }
    }

    override fun hentMottakere(brevbestillingId: BrevbestillingId): List<Mottaker> {
        val query = """
            SELECT * FROM MOTTAKER WHERE BREVBESTILLING_ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, brevbestillingId.id)
            }
            setRowMapper { row ->
                mapMottaker(row)
            }
        }
    }

    override fun hentMottakere(brevbestillingReferanse: BrevbestillingReferanse): List<Mottaker> {
        val query = """
            SELECT * FROM MOTTAKER WHERE BREVBESTILLING_ID IN (SELECT ID FROM BREVBESTILLING WHERE REFERANSE = ?)
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setUUID(1, brevbestillingReferanse.referanse)
            }
            setRowMapper { row ->
                mapMottaker(row)
            }
        }
    }

    private fun mapMottaker(row: Row): Mottaker {
        return Mottaker(
            id = row.getLong("ID"),
            ident = row.getStringOrNull("IDENT"),
            identType = row.getEnumOrNull("IDENT_TYPE"),
            navnOgAdresse = row.getStringOrNull("NAVN_OG_ADRESSE")?.let { DefaultJsonMapper.fromJson(it) },
            bestillingMottakerReferanse = row.getString("BESTILLING_MOTTAKER_REFERANSE")
        )
    }
}

data class Mottaker(
    val id: Long? = null,
    val ident: String? = null,
    val identType: IdentType? = null,
    val navnOgAdresse: NavnOgAdresse? = null,
    val bestillingMottakerReferanse: String,
) {
    init {
        require(navnOgAdresse != null || identType == IdentType.FNR) {
            "navnOgAdresse må være satt dersom identType ikke er FNR."
        }
        require(
            (identType != null && ident != null)
                    || (identType == null && ident == null)
        ) {
            "idenType og ident må være satt sammen, eller begge må være null"
        }
    }
}


data class NavnOgAdresse(
    val navn: String,
    val adresse: Adresse,
)

data class Adresse(
    val landkode: String,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
) {
    init {
        require(
            (landkode == "NOR" && postnummer != null && poststed != null) ||
                    (landkode != "NOR" && postnummer == null && poststed == null)
        ) {
            "Postnummer og poststed må være satt for norsk"
        }
    }
}

enum class IdentType {
    FNR, HPRNR, ORGNR, UTL_ORG
}