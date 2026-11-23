-- V24: Expand retrocompatível - adiciona unidade em metrica e rastreamento de atividade Strava em desempenho

ALTER TABLE metrica ADD COLUMN IF NOT EXISTS unidade VARCHAR(20);
ALTER TABLE desempenho ADD COLUMN IF NOT EXISTS strava_atividade_id VARCHAR(64);
