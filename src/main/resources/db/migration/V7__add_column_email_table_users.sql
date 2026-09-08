-- NOTA: Se essa tabela tivesse dados em produção (ex: 2M de registros),
-- essa migration ia quebrar o banco por conta do NOT NULL direto.
-- Nesses cenários eu faria em etapas: cria como NULL, popula os e-mails antigos
-- e depois altera pra NOT NULL. Rodei direto assim só por premissa de dev mesmo.
ALTER TABLE users ADD email VARCHAR(255) UNIQUE NOT NULL;
