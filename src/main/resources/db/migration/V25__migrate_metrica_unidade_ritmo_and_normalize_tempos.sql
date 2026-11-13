-- V25: Backfill idempotente de metrica.unidade, reclassificacao de RITMO_MEDIO e normalizacao de registro_metrica

-- Passo 1: Tratar metricas de ciclismo/spinning previamente vinculadas a RITMO_MEDIO
UPDATE metrica m
SET campo_strava = 'VELOCIDADE_MEDIA_KMH',
    unidade = 'km/h'
FROM modalidade mod
WHERE mod.id = m.modalidade_id
  AND m.campo_strava = 'RITMO_MEDIO'
  AND m.tipo = 'NUMERICO'
  AND (
      LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ciclismo%'
      OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%spinning%'
  );

UPDATE metrica m
SET campo_strava = NULL
FROM modalidade mod
WHERE mod.id = m.modalidade_id
  AND m.campo_strava = 'RITMO_MEDIO'
  AND m.tipo <> 'NUMERICO'
  AND (
      LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ciclismo%'
      OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%spinning%'
  );

-- Passo 2: Reclassificar demais metricas com RITMO_MEDIO para TEMPO com unidade de ritmo por modalidade
UPDATE metrica m
SET tipo = 'TEMPO',
    unidade = CASE
        WHEN LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%natac%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%swim%'
            THEN 'min/100m'
        WHEN LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%remo%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%rowing%'
            THEN 'min/500m'
        ELSE 'min/km'
    END
FROM modalidade mod
WHERE mod.id = m.modalidade_id
  AND m.campo_strava = 'RITMO_MEDIO'
  AND (
      m.tipo IS DISTINCT FROM 'TEMPO'
      OR m.unidade IS NULL
  );

-- Passo 3: Backfill deterministico de unidade pelo campo_strava
UPDATE metrica m
SET unidade = CASE m.campo_strava
    WHEN 'DISTANCIA_KM' THEN 'km'
    WHEN 'DISTANCIA_METROS' THEN 'm'
    WHEN 'ELEVACAO_GANHO_METROS' THEN 'm'
    WHEN 'DURACAO_MINUTOS' THEN 'min'
    WHEN 'VELOCIDADE_MEDIA_KMH' THEN 'km/h'
    WHEN 'VELOCIDADE_MAXIMA_KMH' THEN 'km/h'
    WHEN 'POTENCIA_MEDIA_WATTS' THEN 'W'
    WHEN 'POTENCIA_MAXIMA_WATTS' THEN 'W'
    WHEN 'CALORIAS' THEN 'kcal'
    WHEN 'FREQUENCIA_CARDIACA_MEDIA' THEN 'bpm'
    WHEN 'FREQUENCIA_CARDIACA_MAXIMA' THEN 'bpm'
    WHEN 'CADENCIA_MEDIA' THEN CASE
        WHEN LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ciclismo%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%spinning%'
            THEN 'rpm'
        ELSE 'spm'
    END
    WHEN 'TEMPO_MOVIMENTO' THEN CASE
        WHEN LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ciclismo%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%spinning%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%triatl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%triathl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%duatl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ironman%'
            THEN 'hh:mm:ss'
        ELSE 'mm:ss'
    END
    WHEN 'TEMPO_TOTAL' THEN CASE
        WHEN LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ciclismo%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%spinning%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%triatl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%triathl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%duatl%'
          OR LOWER(TRANSLATE(mod.nome, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucaaaaaeeeeiiiiooooouuuuc')) LIKE '%ironman%'
            THEN 'hh:mm:ss'
        ELSE 'mm:ss'
    END
    ELSE m.unidade
END
FROM modalidade mod
WHERE mod.id = m.modalidade_id
  AND m.unidade IS NULL
  AND m.campo_strava IS NOT NULL
  AND m.campo_strava <> 'NENHUM';

-- Passo 4: Backfill por sufixo inequivoco no titulo quando compativel com tipo (sem alterar metrica.titulo)
UPDATE metrica m
SET unidade = CASE
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(kg\)\s*$' THEN 'kg'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(km\)\s*$' THEN 'km'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(m\)\s*$' THEN 'm'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(cm\)\s*$' THEN 'cm'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(%\)\s*$' THEN '%'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(kcal\)\s*$' THEN 'kcal'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(min\)\s*$' THEN 'min'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(km/h\)\s*$' THEN 'km/h'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\((?:w|watts)\)\s*$' THEN 'W'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(spm\)\s*$' THEN 'spm'
    WHEN m.tipo = 'NUMERICO' AND TRIM(m.titulo) ~* '\(rpm\)\s*$' THEN 'rpm'
    WHEN m.tipo = 'TEMPO' AND TRIM(m.titulo) ~* '\(min/km\)\s*$' THEN 'min/km'
    WHEN m.tipo = 'TEMPO' AND TRIM(m.titulo) ~* '\(min/100m\)\s*$' THEN 'min/100m'
    WHEN m.tipo = 'TEMPO' AND TRIM(m.titulo) ~* '\(min/500m\)\s*$' THEN 'min/500m'
    ELSE NULL
END
WHERE m.unidade IS NULL
  AND (m.campo_strava IS NULL OR m.campo_strava = 'NENHUM')
  AND m.tipo IN ('NUMERICO', 'TEMPO')
  AND TRIM(m.titulo) ~* '\((kg|km|m|cm|%|kcal|min|km/h|w|watts|spm|rpm|min/km|min/100m|min/500m)\)\s*$';

-- Passo 5: Defaults obrigatorios para eliminar unidade nula em FREQUENCIA_CARDIACA e TEMPO
UPDATE metrica
SET unidade = 'bpm'
WHERE tipo = 'FREQUENCIA_CARDIACA'
  AND unidade IS NULL;

UPDATE metrica
SET unidade = 'mm:ss'
WHERE tipo = 'TEMPO'
  AND unidade IS NULL;

-- Passo 6: Normalizacao segura de registro_metrica (preserva auditoria_desempenho intacta)
-- 6a. Virgula decimal -> ponto em metricas NUMERICO (rejeita separador de milhar ex. 1.200,5)
UPDATE registro_metrica rm
SET valor = REPLACE(TRIM(rm.valor), ',', '.')
FROM metrica m
WHERE m.id = rm.metrica_id
  AND m.tipo = 'NUMERICO'
  AND TRIM(rm.valor) ~ '^-?\d+,\d+$';

-- 6b. Extracao de ritmo valido em registros ligados a RITMO_MEDIO (preserva textos incompativeis como "24,7 km/h")
UPDATE registro_metrica rm
SET valor = CASE
    WHEN LENGTH(SPLIT_PART((regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1], ':', 1)) = 1
        THEN '0' || (regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1]
    ELSE (regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1]
END
FROM metrica m
WHERE m.id = rm.metrica_id
  AND m.campo_strava = 'RITMO_MEDIO'
  AND TRIM(rm.valor) ~ '^\d{1,3}:[0-5]\d(?:\s*(?:min/km|min/100m|min/500m|/km))?$'
  AND rm.valor IS DISTINCT FROM CASE
      WHEN LENGTH(SPLIT_PART((regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1], ':', 1)) = 1
          THEN '0' || (regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1]
      ELSE (regexp_match(TRIM(rm.valor), '^(\d{1,3}:[0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1]
  END;

-- 6c. Cronometros TEMPO em 2 segmentos com mm >= 60 -> hh:mm:ss (protege ritmos lentos, hh:mm:ss e segundos >= 60)
UPDATE registro_metrica rm
SET valor = CASE
    WHEN ((SPLIT_PART(TRIM(rm.valor), ':', 1))::int / 60) < 10
        THEN '0' || ((SPLIT_PART(TRIM(rm.valor), ':', 1))::int / 60)::text
    ELSE ((SPLIT_PART(TRIM(rm.valor), ':', 1))::int / 60)::text
END
    || ':' || LPAD((((SPLIT_PART(TRIM(rm.valor), ':', 1))::int % 60))::text, 2, '0')
    || ':' || SPLIT_PART(TRIM(rm.valor), ':', 2)
FROM metrica m
WHERE m.id = rm.metrica_id
  AND m.tipo = 'TEMPO'
  AND m.unidade IN ('mm:ss', 'hh:mm:ss')
  AND TRIM(rm.valor) ~ '^\d{2,}:[0-5]\d$'
  AND (SPLIT_PART(TRIM(rm.valor), ':', 1))::int >= 60;
