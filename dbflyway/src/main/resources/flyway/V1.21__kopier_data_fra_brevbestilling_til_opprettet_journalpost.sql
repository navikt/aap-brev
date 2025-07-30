insert into opprettet_journalpost (journalpost_id, ferdigstilt, mottaker_id, distribusjon_bestilling_id)
select b.journalpost_id, b.journalpost_ferdigstilt, m.id, b.distribusjon_bestilling_id
from mottaker m
         inner join brevbestilling b on m.brevbestilling_id = b.id
where b.journalpost_id is not null
  and not exists(select b.journalpost_id from opprettet_journalpost oj where oj.journalpost_id = b.journalpost_id)