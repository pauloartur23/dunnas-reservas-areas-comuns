-- =========================================================
-- V19 - Areas comuns
-- Cria a tabela de areas comuns disponiveis para reserva
-- =========================================================

CREATE TABLE areas_comuns (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     nome VARCHAR(255) NOT NULL,
     descricao VARCHAR(255),
     ativa BOOLEAN NOT NULL DEFAULT TRUE
);