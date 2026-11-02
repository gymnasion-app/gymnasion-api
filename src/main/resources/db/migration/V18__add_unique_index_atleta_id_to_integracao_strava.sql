-- V18__add_unique_index_atleta_id_to_integracao_strava.sql
-- Garante a unicidade de atleta Strava por aluno (US-14)
CREATE UNIQUE INDEX IF NOT EXISTS uk_integracao_strava_atleta_id
    ON integracao_strava(atleta_id)
    WHERE atleta_id IS NOT NULL;
