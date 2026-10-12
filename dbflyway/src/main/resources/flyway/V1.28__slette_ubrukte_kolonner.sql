/*
V1.18 introduserte nye tabell OPPRETTET_JOURNALPOST som skal brukes i stedet for kolonnene:
    [JOURNALPOST_ID, JOURNALPOST_FERDIGSTILT, DISTRIBUSJON_BESTILLING_ID]
*/

ALTER TABLE brevbestilling
    DROP COLUMN journalpost_id,
    DROP COLUMN journalpost_ferdigstilt,
    DROP COLUMN distribusjon_bestilling_id;
