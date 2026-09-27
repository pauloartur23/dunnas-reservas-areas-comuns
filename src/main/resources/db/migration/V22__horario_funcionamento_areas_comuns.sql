-- =========================================================
-- V22 - Horario de funcionamento das areas comuns
-- Adiciona janela opcional de funcionamento por area
-- =========================================================

ALTER TABLE areas_comuns
    ADD COLUMN horario_abertura TIME,
    ADD COLUMN horario_fechamento TIME;

ALTER TABLE areas_comuns
    ADD CONSTRAINT ck_areas_comuns_horario_funcionamento
        CHECK (
            (horario_abertura IS NULL AND horario_fechamento IS NULL)
                OR (horario_abertura IS NOT NULL
                AND horario_fechamento IS NOT NULL
                AND horario_fechamento > horario_abertura)
            );