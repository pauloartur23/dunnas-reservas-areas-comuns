-- =========================================================
-- V23 - Dias de funcionamento das areas comuns
-- Permite restringir a area comum a determinados dias da semana (opcional)
-- =========================================================

CREATE TABLE area_comum_dia_funcionamento (
                                              area_comum_id UUID NOT NULL,
                                              dia_semana VARCHAR(15) NOT NULL,
                                              PRIMARY KEY (area_comum_id, dia_semana),
                                              CONSTRAINT fk_area_comum_dia_funcionamento_area
                                                  FOREIGN KEY (area_comum_id) REFERENCES areas_comuns (id)
);

CREATE INDEX idx_area_comum_dia_funcionamento_area_id ON area_comum_dia_funcionamento (area_comum_id);