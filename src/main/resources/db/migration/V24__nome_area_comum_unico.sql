-- =========================================================
-- V24 - Nome unico para areas comuns (case-insensitive)
-- "Piscina" e "piscina" passam a ser tratados como o mesmo nome;
-- cadastro ou edicao com nome duplicado e bloqueado no banco.
-- Regra adicionada por nos: o desafio nao especifica nada sobre
-- unicidade de nome de area comum.
-- =========================================================

CREATE UNIQUE INDEX ux_areas_comuns_nome_lower ON areas_comuns (lower(nome));