# aap-brev

[![release](https://github.com/navikt/aap-brev/actions/workflows/deploy.yaml/badge.svg)](https://github.com/navikt/aap-brev/actions/workflows/deploy.yaml)

aap-brev håndterer bestilling, journalføring og distribusjon av brev. Se sysdoc for teknisk
beskrivelse: https://aap-sysdoc.ansatt.nav.no/funksjonalitet/Brev/teknisk/

## API-dokumentasjon

APIene er dokumentert med Swagger: https://aap-brev.intern.dev.nav.no/swagger-ui/index.html

## Komme i gang

Bruker Gradle wrapper, så bare klon og kjør `./gradlew build`.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#ytelse-aap-værsågod`.

## Lokalt utviklingsmiljø

### Laste ned private pakker

For at Gradle skal finne private pakker på Github, legg dette i `$HOME/.gradle/gradle.properties`

```
githubUser=<github-brukernavn>
githubPassword=<github-token>
```

### Kjøre lokalt

Kjør`TestAppKt`. Appen vil da kjøre på localhost:8082. Alternativt, for å unngå å starte IntelliJ, gå i rotmappen og
kjør:

```./gradlew runTestApp ```
(denne kjører med TestContainers)

#### Lokal db, pdfgen og sanity

For at lokal sanity skal fungere må du kjøre opp docker-compose med `docker-compose up -d`. Dette starter opp lokal
pdfgen, database og sanity-proxy.

Må også legge til env-variabel `SANITY_API_READ_TOKEN` med token for å lese fra sanity.

Hent secret med nais cli:

```shell
nais secret get -t aap -e dev-gcp sanity-api-read-token --with-values
```

Lagre secret lokalt (MacOS):

```shell
security add-generic-password -a "$USER" -s "SANITY_API_READ_TOKEN" -w "<DITT_TOKEN_HER>"
```

Legg så til denne i `.zshrc`, `.zprofile`, `.bashrc`, e.l.:

```
export SANITY_API_READ_TOKEN=$(security find-generic-password -w -a "$USER" -s "SANITY_API_READ_TOKEN")
```

### Kjøre lokalt mot dev-gcp

**⚠️OBS:** Denne oppskriften fungerer ikke lenger etter endringer i Nais. Kjør appen lokalt i stedet. Om du har behov
for dev-data kan du klone databasen. Se i oppskrift for db-dump her: [aap-cli](https://github.com/navikt/aap-cli#dump-gcp-dbsh)

---
#### Deprecated oppskrift
Prosjektet inneholder en run config som kan kjøres av IntelliJ. Burde være synlig under "Run configurations" med navnet
`dev-gcp.run.xml`.

For at det skal kjøre lokalt må du gjøre følgende:
1. Hent secret med [aap-cli/get-secret.sh](https://github.com/navikt/aap-cli): \
   `get-secret` \
2. Kjør opp lokal database med: \
   `docker-compose up -d`
3. Om du ønsker å hente data fra dev til lokal maskin kan du bruke [dump-gcp-db.sh](https://github.com/navikt/aap-cli?tab=readme-ov-file#dump-gcp-dbsh)
4. Kjør `dev-gcp` fra IntelliJ.

Etter dette vil appen kjøre mot reelle data. Her kan du velge om du vil koble deg på gjennom autentisert frontend eller
f.eks. gyldig token med cURL e.l.

OBS: Krever at du har `EnvFile`-plugin i IntelliJ. 
