# Agent Instructions

## Bygge og teste

```bash
# Bygg hele prosjektet
./gradlew build

# Kjør tester
./gradlew test

# Kjør kun app-modulen
./gradlew :app:build

# Regenerer openapi.json
./gradlew genererOpenApi
```

## Kjøre lokalt

```bash
# Start lokal testapp (krever ikke ekstern database)
./gradlew runTestApp
# Appen starter på localhost:8082

# Start lokal database
docker-compose up -d
```

## Prosjektstruktur

```
app/src/main/kotlin/no/nav/aap/brev/
├── App.kt                  # Inngangspunkt, Ktor-oppsett
├── bestilling/             # Brevbestilling og PDF-generering
├── distribusjon/           # Distribusjon av ferdige brev
├── journalføring/          # Journalføring mot Joark/Dokarkiv
├── innhold/                # Brevinnhold fra Sanity CMS
├── person/                 # Personoppslag mot PDL
├── organisasjon/           # Enhets- og ansattoppslag
├── prosessering/           # aap-motor jobbdefinisjoner
├── api/                    # Ktor route-definisjoner
└── feil/                   # Feilhåndtering og validering
```

## Avhengighetsstruktur

- `app` → `kontrakt`, `dbflyway`
- `kontrakt` – publiseres separat som GitHub Package (versjoneres med semantic versioning ved endringer)
- Ekstern kommunikasjon via `aap-komponenter/httpklient`

## Deployment

- Deploy til **dev-gcp** og **prod-gcp** skjer automatisk ved push til `main` via GitHub Actions (`.github/workflows/deploy.yaml`).
- NAIS-manifester ligger i `.nais/`.
- Kontrakt publiseres automatisk ved endringer i `kontrakt/`-mappen.

## Gradle-oppsett

- Privat GitHub Packages krever `githubUser` og `githubPassword` i `~/.gradle/gradle.properties`.
- Custom konvensjons-plugin ligger i `buildSrc/`.
- Versjonshåndtering via `gradle.properties`.

## Viktige miljøvariabler

| Variabel | Beskrivelse |
|---|---|
| `NAIS_DATABASE_BREV_BREV_JDBC_URL` | JDBC-URL til PostgreSQL-databasen |
| `HTTP_PORT` | HTTP-port (default: 8080) |
