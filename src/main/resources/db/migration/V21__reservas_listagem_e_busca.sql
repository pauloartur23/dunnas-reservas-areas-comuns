-- =========================================================
-- V21 - Funcoes de listagem e busca de reservas
-- Centraliza consultas paginadas com filtros opcionais e
-- busca individual com autorizacao embutida
-- =========================================================

-- Autorizacao: morador so pode ver a propria reserva
CREATE OR REPLACE FUNCTION fn_assert_morador_pode_ver_reserva(
    p_morador_id UUID,
    p_reserva_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM reservas r
        WHERE r.id = p_reserva_id
          AND r.morador_id = p_morador_id
    ) THEN
        RAISE EXCEPTION
            USING ERRCODE = '42501',
                  MESSAGE = 'Morador nao pode acessar esta reserva';
END IF;
END;
$$;


-- Listagem das reservas do proprio morador, com filtros opcionais
CREATE OR REPLACE FUNCTION fn_listar_reservas_do_morador(
    p_morador_id UUID,
    p_status VARCHAR(20) DEFAULT NULL,
    p_area_comum_id UUID DEFAULT NULL,
    p_data DATE DEFAULT NULL
)
RETURNS TABLE (
    id UUID,
    area_comum_id UUID,
    morador_id UUID,
    data DATE,
    hora_inicio TIME,
    hora_fim TIME,
    status VARCHAR(20),
    motivo_negacao VARCHAR(255),
    administrador_decisao_id UUID,
    data_solicitacao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_decisao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_cancelamento TIMESTAMP(6) WITHOUT TIME ZONE,
    versao BIGINT
)
LANGUAGE sql
STABLE
AS $$
SELECT r.id, r.area_comum_id, r.morador_id, r.data, r.hora_inicio, r.hora_fim,
       r.status, r.motivo_negacao, r.administrador_decisao_id,
       r.data_solicitacao, r.data_decisao, r.data_cancelamento, r.versao
FROM reservas r
WHERE r.morador_id = p_morador_id
  AND (p_status IS NULL OR r.status = p_status)
  AND (p_area_comum_id IS NULL OR r.area_comum_id = p_area_comum_id)
  AND (p_data IS NULL OR r.data = p_data)
ORDER BY r.data_solicitacao DESC NULLS LAST, r.id DESC;
$$;


-- Listagem de todas as reservas, visao do administrador
CREATE OR REPLACE FUNCTION fn_listar_reservas_para_admin(
    p_status VARCHAR(20) DEFAULT NULL,
    p_area_comum_id UUID DEFAULT NULL,
    p_data DATE DEFAULT NULL
)
RETURNS TABLE (
    id UUID,
    area_comum_id UUID,
    morador_id UUID,
    data DATE,
    hora_inicio TIME,
    hora_fim TIME,
    status VARCHAR(20),
    motivo_negacao VARCHAR(255),
    administrador_decisao_id UUID,
    data_solicitacao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_decisao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_cancelamento TIMESTAMP(6) WITHOUT TIME ZONE,
    versao BIGINT
)
LANGUAGE sql
STABLE
AS $$
SELECT r.id, r.area_comum_id, r.morador_id, r.data, r.hora_inicio, r.hora_fim,
       r.status, r.motivo_negacao, r.administrador_decisao_id,
       r.data_solicitacao, r.data_decisao, r.data_cancelamento, r.versao
FROM reservas r
WHERE (p_status IS NULL OR r.status = p_status)
  AND (p_area_comum_id IS NULL OR r.area_comum_id = p_area_comum_id)
  AND (p_data IS NULL OR r.data = p_data)
ORDER BY r.data_solicitacao DESC NULLS LAST, r.id DESC;
$$;


-- Busca de uma reserva especifica no contexto do morador
CREATE OR REPLACE FUNCTION fn_buscar_reserva_do_morador(
    p_morador_id UUID,
    p_reserva_id UUID
)
RETURNS TABLE (
    id UUID,
    area_comum_id UUID,
    morador_id UUID,
    data DATE,
    hora_inicio TIME,
    hora_fim TIME,
    status VARCHAR(20),
    motivo_negacao VARCHAR(255),
    administrador_decisao_id UUID,
    data_solicitacao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_decisao TIMESTAMP(6) WITHOUT TIME ZONE,
    data_cancelamento TIMESTAMP(6) WITHOUT TIME ZONE,
    versao BIGINT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    PERFORM fn_assert_morador_pode_ver_reserva(p_morador_id, p_reserva_id);

RETURN QUERY
SELECT r.id, r.area_comum_id, r.morador_id, r.data, r.hora_inicio, r.hora_fim,
       r.status, r.motivo_negacao, r.administrador_decisao_id,
       r.data_solicitacao, r.data_decisao, r.data_cancelamento, r.versao
FROM reservas r
WHERE r.id = p_reserva_id;
END;
$$;