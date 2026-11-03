-- V19__modalidade_ativo_and_metrica_partial_unique.sql
-- Adiciona controle de status ativo/desativado na modalidade gerenciada pelo Admin
ALTER TABLE modalidade ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Substitui a constraint incondicional de metrica por indice unico parcial (RN-015, RN-017)
-- permitindo recriar metricas com o mesmo titulo apos descontinuacao
ALTER TABLE metrica DROP CONSTRAINT IF EXISTS uq_metrica_titulo_personal_modalidade;

CREATE UNIQUE INDEX IF NOT EXISTS uk_metrica_titulo_personal_modalidade_ativo 
    ON metrica (personal_trainer_id, modalidade_id, LOWER(titulo)) 
    WHERE ativo = TRUE;
