-- =========================================================
-- V20 - Reservas de areas comuns
-- Cria a tabela de reservas e a protecao contra conflito
-- de horario em reservas aprovadas, garantida pelo proprio banco
-- =========================================================

-- Necessaria para permitir "=" dentro de uma constraint de exclusao GiST
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservas (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          area_comum_id UUID NOT NULL,
                          morador_id UUID NOT NULL,
                          data DATE NOT NULL,
                          hora_inicio TIME NOT NULL,
                          hora_fim TIME NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          motivo_negacao VARCHAR(255),
                          administrador_decisao_id UUID,
                          data_solicitacao TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
                          data_decisao TIMESTAMP(6) WITHOUT TIME ZONE,
                          data_cancelamento TIMESTAMP(6) WITHOUT TIME ZONE,
                          versao BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT fk_reservas_area_comum
                              FOREIGN KEY (area_comum_id) REFERENCES areas_comuns (id),
                          CONSTRAINT fk_reservas_morador
                              FOREIGN KEY (morador_id) REFERENCES moradores (id),
                          CONSTRAINT fk_reservas_administrador
                              FOREIGN KEY (administrador_decisao_id) REFERENCES administradores (id),
                          CONSTRAINT ck_reservas_horario_valido
                              CHECK (hora_fim > hora_inicio)
);

CREATE INDEX idx_reservas_area_comum_id ON reservas (area_comum_id);
CREATE INDEX idx_reservas_morador_id ON reservas (morador_id);
CREATE INDEX idx_reservas_administrador_id ON reservas (administrador_decisao_id);

-- Garante, no proprio banco, que nunca existam duas reservas APROVADA
-- da mesma area com intervalo de horario sobreposto (RN-01-06, RN-01-07)
ALTER TABLE reservas
    ADD CONSTRAINT excl_reservas_conflito_aprovada
    EXCLUDE USING gist (
        area_comum_id WITH =,
        tsrange(data + hora_inicio, data + hora_fim) WITH &&
    )
    WHERE (status = 'APROVADA');