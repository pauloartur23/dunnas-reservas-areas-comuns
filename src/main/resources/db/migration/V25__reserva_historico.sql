-- =========================================================
-- V25 - Historico de reserva (registro de auditoria)
-- Cada mudanca de status de uma reserva gera uma linha aqui:
-- quem fez, quando, de qual status pra qual, e observacao
-- (usada para guardar o motivo quando for negacao).
-- Sao snapshots imutaveis: nao ha FK para usuarios, o nome do
-- autor fica gravado como texto no momento do evento, entao o
-- historico nao muda mesmo se o usuario for editado ou desativado depois.
-- =========================================================

CREATE TABLE reserva_historico (
                                   id              UUID PRIMARY KEY,
                                   reserva_id      UUID NOT NULL REFERENCES reservas(id),
                                   status_anterior VARCHAR(20),
                                   status_novo     VARCHAR(20) NOT NULL,
                                   autor_tipo      VARCHAR(20) NOT NULL,
                                   autor_nome      VARCHAR(255) NOT NULL,
                                   observacao      VARCHAR(255),
                                   data_evento     TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_reserva_historico_reserva_id ON reserva_historico (reserva_id);