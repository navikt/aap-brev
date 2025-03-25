package no.nav.aap.brev.journalføring

import no.nav.aap.brev.bestilling.BehandlingsflytGateway
import no.nav.aap.brev.bestilling.Brevbestilling
import no.nav.aap.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.brev.bestilling.BrevbestillingRepository
import no.nav.aap.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.brev.bestilling.PdfBrev
import no.nav.aap.brev.bestilling.PdfBrev.Blokk
import no.nav.aap.brev.bestilling.PdfBrev.FormattertTekst
import no.nav.aap.brev.bestilling.PdfBrev.Innhold
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker
import no.nav.aap.brev.bestilling.PdfBrev.Mottaker.IdentType
import no.nav.aap.brev.bestilling.PdfBrev.Signatur
import no.nav.aap.brev.bestilling.PdfBrev.Tekstbolk
import no.nav.aap.brev.bestilling.PdfGateway
import no.nav.aap.brev.bestilling.PersoninfoGateway
import no.nav.aap.brev.bestilling.PersoninfoV2
import no.nav.aap.brev.bestilling.PersoninfoV2Gateway
import no.nav.aap.brev.bestilling.SaksbehandlingPdfGenGateway
import no.nav.aap.brev.bestilling.Saksnummer
import no.nav.aap.brev.journalføring.JournalføringData.MottakerType
import no.nav.aap.brev.kontrakt.BlokkInnhold
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.organisasjon.AnsattInfoDevGateway
import no.nav.aap.brev.organisasjon.AnsattInfoGateway
import no.nav.aap.brev.organisasjon.EnhetGateway
import no.nav.aap.brev.organisasjon.NomInfoGateway
import no.nav.aap.brev.organisasjon.NorgGateway
import no.nav.aap.brev.person.PdlGateway
import no.nav.aap.brev.util.formaterFullLengde
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import java.time.LocalDate

class JournalføringService(
    private val brevbestillingRepository: BrevbestillingRepository,
    private val personinfoGateway: PersoninfoGateway,
    private val personinfoV2Gateway: PersoninfoV2Gateway,
    private val pdfGateway: PdfGateway,
    private val journalføringGateway: JournalføringGateway,
    private val ansattInfoGateway: AnsattInfoGateway,
    private val enhetGateway: EnhetGateway,
) {

    companion object {
        fun konstruer(connection: DBConnection): JournalføringService {
            return JournalføringService(
                brevbestillingRepository = BrevbestillingRepositoryImpl(connection),
                personinfoGateway = BehandlingsflytGateway(),
                personinfoV2Gateway = PdlGateway(),
                pdfGateway = SaksbehandlingPdfGenGateway(),
                journalføringGateway = DokarkivGateway(),
                ansattInfoGateway = if (Miljø.er() == MiljøKode.DEV) AnsattInfoDevGateway() else NomInfoGateway(),
                enhetGateway = NorgGateway(),
            )
        }
    }

    fun journalførBrevbestilling(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)

        checkNotNull(bestilling.brev) {
            "Kan ikke journalføre bestilling uten brev."
        }
        check(bestilling.journalpostId == null) {
            "Kan ikke journalføre brev for bestilling som allerede er journalført."
        }

        val personinfo =
            if (bestilling.brukerIdent != null) {
                personinfoV2Gateway.hentPersoninfo(bestilling.brukerIdent)
            } else {
                // TODO: Midlertidig for bakoverkompabilitet i dev
                personinfoGateway.hentPersoninfo(bestilling.saksnummer).let {
                    PersoninfoV2(
                        personIdent = it.fnr,
                        navn = it.navn,
                        harStrengtFortroligAdresse = false
                    )
                }
            }

        val automatisk = bestilling.brevtype == Brevtype.VARSEL_OM_BESTILLING
        val signaturer: List<Signatur> = if (personinfo.harStrengtFortroligAdresse || automatisk ) {
            emptyList()
        } else {
            val sorterteSignaturer = bestilling.signaturer.sortedBy { it.rolle }

            val ansattInfoListe = sorterteSignaturer.map {
                ansattInfoGateway.hentAnsattInfo(it.navIdent)
            }

            val enheter = enhetGateway.hentEnhetsnavn(ansattInfoListe.map { it.enhetsnummer })
            val brukEnhetsTypeNavn = when (bestilling.brevtype) {
                Brevtype.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT, Brevtype.FORVALTNINGSMELDING -> {
                    true
                }
                Brevtype.VEDTAK_ENDRING, Brevtype.VARSEL_OM_BESTILLING, Brevtype.AVSLAG, Brevtype.INNVILGELSE -> {
                    false
                }
            }
            ansattInfoListe.map { ansattInfo ->
                val enhet = enheter.single { it.enhetsNummer == ansattInfo.enhetsnummer }
                Signatur(navn = ansattInfo.navn, enhet = if (brukEnhetsTypeNavn) enhet.enhetstypeNavn else enhet.navn)
            }
        }

        val pdfBrev = mapPdfBrev(
            brukerIdent = personinfo.personIdent,
            navn = personinfo.navn,
            saksnummer = bestilling.saksnummer,
            brev = bestilling.brev,
            dato = LocalDate.now(),
            språk = bestilling.språk,
            signaturer = signaturer,
            automatisk = automatisk,
        )
        val pdf = pdfGateway.genererPdf(pdfBrev)

        val journalføringData = JournalføringData(
            brukerFnr = personinfo.personIdent,
            mottakerIdent = personinfo.personIdent,
            mottakerType = MottakerType.FNR,
            mottakerNavn = null,
            saksnummer = bestilling.saksnummer,
            eksternReferanseId = bestilling.referanse.referanse,
            tittelJournalpost = checkNotNull(value = bestilling.brev.journalpostTittel ?: bestilling.brev.overskrift),
            tittelBrev = checkNotNull(value = bestilling.brev.overskrift),
            brevkode = bestilling.brevtype.name,
            overstyrInnsynsregel = false,
        )

        val forsøkFerdigstill = ferdigstillVedOpprettelseAvJournalpost(bestilling)
        val response = journalføringGateway.journalførBrev(
            journalføringData = journalføringData,
            pdf = pdf,
            forsøkFerdigstill = forsøkFerdigstill,
        )
        brevbestillingRepository.lagreJournalpost(
            bestilling.id,
            response.journalpostId,
            response.journalpostferdigstilt
        )
    }

    fun tilknyttVedlegg(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val journalpostId = checkNotNull(bestilling.journalpostId)
        val vedlegg = bestilling.vedlegg
        if (vedlegg.isNotEmpty()) {
            journalføringGateway.tilknyttVedlegg(journalpostId, vedlegg)
        }
    }

    fun ferdigstillJournalpost(referanse: BrevbestillingReferanse) {
        val bestilling = brevbestillingRepository.hent(referanse)
        val journalpostId = checkNotNull(bestilling.journalpostId)
        if (bestilling.journalpostFerdigstilt != true) {
            journalføringGateway.ferdigstillJournalpost(journalpostId)
        }
    }

    private fun ferdigstillVedOpprettelseAvJournalpost(bestilling: Brevbestilling): Boolean {
        return bestilling.vedlegg.isEmpty()
    }

    private fun mapPdfBrev(
        brukerIdent: String,
        navn: String,
        saksnummer: Saksnummer,
        brev: Brev,
        dato: LocalDate,
        språk: Språk,
        signaturer: List<Signatur>,
        automatisk: Boolean,
    ): PdfBrev {
        return PdfBrev(
            mottaker = Mottaker(
                navn = navn,
                ident = brukerIdent,
                identType = IdentType.FNR
            ),
            saksnummer = saksnummer.nummer,
            dato = dato.formaterFullLengde(språk),
            overskrift = brev.overskrift,
            tekstbolker = brev.tekstbolker.map {
                Tekstbolk(
                    overskrift = it.overskrift,
                    innhold = it.innhold.map {
                        Innhold(
                            overskrift = it.overskrift,
                            blokker = it.blokker.map {
                                Blokk(
                                    innhold = it.innhold.mapNotNull {
                                        when (it) {
                                            is BlokkInnhold.FormattertTekst -> FormattertTekst(
                                                tekst = it.tekst,
                                                formattering = it.formattering
                                            )

                                            is BlokkInnhold.Faktagrunnlag -> {
                                                throw IllegalStateException("Kan ikke lage PDF av brev med manglende faktagrunnlag ${it.tekniskNavn}.")
                                            }
                                        }
                                    },
                                    type = it.type
                                )
                            })
                    })
            },
            automatisk = automatisk,
            signaturer = signaturer
        )
    }
}
