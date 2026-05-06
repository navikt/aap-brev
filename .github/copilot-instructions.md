# Copilot Instructions

## Prosjektoversikt

`aap-brev` er en Kotlin/Ktor-applikasjon som håndterer bestilling, journalføring og distribusjon av brev i NAV sitt AAP-domene (arbeidsavklaringspenger). Den kjører på NAIS (GCP).

## Teknologistack

- **Språk**: Kotlin, JDK 21
- **Rammeverk**: Ktor (server), aap-motor (jobbprosessering)
- **Database**: PostgreSQL med Flyway-migrering og HikariCP
- **Autentisering**: Entra ID (OIDC) via `aap-komponenter`
- **Build**: Gradle med custom `aap.conventions`-plugin i `buildSrc`
- **Testing**: JUnit 5, AssertJ, Testcontainers, MockK

## Arkitektur – Heksagonal (Ports & Adapters)

Prosjektet følger heksagonal arkitektur. Reglene er:

- **Porter (interfaces)**: `*Gateway` og `*Repository` interfaces definerer hva domenet trenger utenfra. Disse eies av domenet og ligger i domenepakken (f.eks. `bestilling/PdfGateway.kt`).
- **Adaptere (implementasjoner)**: Konkrete klasser som `DokarkivGateway`, `BrevbestillingRepositoryImpl` implementerer portene. De kan avhenge av infrastruktur (DB, HTTP), men domenet skal *ikke* importere dem direkte.
- **Avhengighetsregelen**: Domene-services (`*Service`) skal kun importere interfaces, aldri konkrete implementasjoner (`*Impl`, spesifikke gateway-klasser). Dette sikrer at domenet er testbart og uavhengig av infrastruktur.
- **Komposisjon via `konstruer()`**: Wiring gjøres i `companion object fun konstruer(connection: DBConnection)` i hver klasse. Det er her konkrete implementasjoner velges – dette er det eneste stedet det er akseptabelt å referere til `*Impl`-klasser.
- **API-laget** (`api/`) er inngående adaptere (inbound). De kaller `Service.konstruer(connection)` og videresender til domenet.

**Kjent avvik å unngå å gjenta**: `JournalføringService` importerer `BrevbestillingRepositoryImpl` direkte — dette er en feil som ikke skal kopieres.

## Kodekonvensjoner

- Skriv Kotlin idiomatisk – bruk `data class`, `sealed class`, extension functions og `when`-uttrykk fremfor lange if/else-kjeder.
- Følg eksisterende pakkestruktur under `no.nav.aap.brev`. Gruppér etter domene/funksjonelt område (f.eks. `bestilling`, `distribusjon`, `journalføring`, `innhold`).
- Bruk norsk for domene-relaterte navn (klasser, funksjoner, variabler) som speiler NAV-fagbegreper. Bruk engelsk for teknisk infrastrukturkode.
- Databasetilgang gjøres via `dbConnection.transaction { }` – ikke åpne egne connections manuelt.

## Logging

- Bruk `LoggerFactory.getLogger(KlasseNavn::class.java)` for vanlig logging.
- Sensitiv informasjon (fødselsnummer, navn) skal **utelukkende** logges til `SECURE_LOGGER` (`LoggerFactory.getLogger("team-logs")`).
- Aldri logg personidentifiserende data i vanlig logger.

## Testing

- Enhetstester ligger i `app/src/test/kotlin`.
- Integrasjonstester mot database bruker Testcontainers med PostgreSQL.
- Mock ekstern avhengigheter med MockK.
- Bruk `lib-test` for felles testverktøy og hjelpere.
- Foretrekk `assertThat(...)` fra AssertJ fremfor JUnit assertions.

## Modulstruktur

- `app/` – hovedapplikasjon (Ktor-server, domenelogikk, API-ruter)
- `kontrakt/` – delte kontrakter/DTOer publisert som eget artefakt
- `dbflyway/` – Flyway SQL-migreringer
- `lib-test/` – delte testverktøy

## Viktige mønstre

- API-ruter defineres som extension functions med navn `*Api(dataSource)` og registreres i `App.kt`.
- Async jobbprosessering gjøres via `aap-motor` – implementér `JobbUtfører` og registrer i `Motor(...)`.
- Åpne API dokumenteres med OpenAPI via `ktor-openapi-generator`; kjør `./gradlew genererOpenApi` for å regenerere `openapi.json`.
