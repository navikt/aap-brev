CREATE TABLE BREVBESTILLING
(
    REFERANSE               UUID                                        NOT NULL    PRIMARY KEY,
    DATA                    JSON                                        NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    OPPDATERT_TID           TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    BEHANDLING_REFERANSE    UUID                                        NOT NULL,
    SPRAK                   TEXT                                        NOT NULL,
    BREVTYPE                TEXT                                        NOT NULL
);

CREATE UNIQUE INDEX UIDX_BREVBESTILLING_BEHANDLING_REFERANSE ON BREVBESTILLING (BEHANDLING_REFERANSE);
