# aap-brev
[![release](https://github.com/navikt/aap-brev/actions/workflows/deploy.yaml/badge.svg)](https://github.com/navikt/aap-brev/actions/workflows/deploy.yaml)

aap-brev håndterer bestilling, journalføring og distribusjon av brev. 

## API-dokumentasjon

APIene er dokumentert med Swagger: localhost:8082/swagger-ui/index.html

## Komme i gang

Bruker Gradle wrapper, så bare klon og kjør `./gradlew build`.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#po-aap-team-aap`.

## Lokalt utviklingsmiljø

### Laste ned private pakker
For at Gradle skal finne private pakker på Github, legg dette i `$HOME/.gradle/gradle.properties`

```
githubUser=<github-brukernavn>
githubPassword=<github-token>
```

### Kjøre lokalt

Kjør`TestAppKt`. Appen vil da kjøre på localhost:8082. Alternativt, for å unngå å starte IntelliJ, gå i rotmappen og kjør:

```./gradlew runTestApp ```

### Kjøre lokalt mot dev-gcp

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
