-- =========================================================
-- V26 - Duracao maxima de reserva por area comum
-- Adiciona um limite opcional de duracao (em minutos) para
-- reservas de uma area comum. Decisao propria, nao exigida
-- pelo desafio: sem valor cadastrado, a area continua sem
-- limite de duracao (mesmo comportamento de antes desta migration).
-- =========================================================

ALTER TABLE areas_comuns
    ADD COLUMN duracao_maxima_minutos INTEGER;

ALTER TABLE areas_comuns
    ADD CONSTRAINT ck_areas_comuns_duracao_maxima_positiva
        CHECK (duracao_maxima_minutos IS NULL OR duracao_maxima_minutos > 0);