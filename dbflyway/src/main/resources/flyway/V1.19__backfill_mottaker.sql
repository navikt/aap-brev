insert into mottaker (brevbestilling_id, bestilling_mottaker_referanse, ident, ident_type)
select id, unik_referanse, bruker_ident, 'FNR'
from brevbestilling b
where bruker_ident is not null
  and not exists (select id from mottaker where brevbestilling_id = b.id)