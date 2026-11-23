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

-- Passo 2a: RN-017 / US-19 C2 - metricas com historico nao mudam de tipo.
-- As que ainda nao sao TEMPO perdem o vinculo RITMO_MEDIO e preservam tipo e valores;
-- o personal pode criar uma nova metrica TEMPO de ritmo.
UPDATE metrica m
SET campo_strava = NULL
WHERE m.campo_strava = 'RITMO_MEDIO'
  AND m.tipo IS DISTINCT FROM 'TEMPO'
  AND (
      EXISTS (SELECT 1 FROM registro_metrica rm WHERE rm.metrica_id = m.id)
      OR EXISTS (SELECT 1 FROM auditoria_desempenho ad WHERE ad.metrica_id = m.id)
      OR EXISTS (
          SELECT 1 FROM sessao_treino_metrica stm
          JOIN sessao_treino st ON st.id = stm.sessao_treino_id
          WHERE stm.metrica_id = m.id AND st.status = 'CONCLUIDA'
      )
  );

-- Passo 2b: Reclassificar as demais metricas RITMO_MEDIO (sem historico) para TEMPO com unidade de ritmo por modalidade
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

-- 6b. Extracao de ritmo valido em registros ligados a RITMO_MEDIO (apenas metricas TEMPO; preserva textos incompativeis)
--     Mesmo limite do ValidadorValorMetrica: ate 99 minutos (\d{1,2}:[0-5]\d)
UPDATE registro_metrica rm
SET valor = LPAD((regexp_match(TRIM(rm.valor), '^(\d{1,2}):([0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[1], 2, '0')
    || ':' || (regexp_match(TRIM(rm.valor), '^(\d{1,2}):([0-5]\d)(?:\s*(?:min/km|min/100m|min/500m|/km))?$'))[2]
FROM metrica m
WHERE m.id = rm.metrica_id
  AND m.tipo = 'TEMPO'
  AND m.campo_strava = 'RITMO_MEDIO'
  AND TRIM(rm.valor) ~ '^\d{1,2}:[0-5]\d(?:\s*(?:min/km|min/100m|min/500m|/km))?$'
  AND rm.valor !~ '^\d{2}:[0-5]\d$';

-- 6c. Cronometros TEMPO em 2 segmentos com mm >= 60 -> hh:mm:ss (protege ritmos lentos, hh:mm:ss e segundos >= 60)
--     Casts protegidos por CASE (o PostgreSQL nao garante a ordem de avaliacao do WHERE). So converte ate 5999 min,
--     limite que cabe em hh:mm:ss com hh <= 99 (formato aceito pelo ValidadorValorMetrica); acima disso o valor fica intacto.
UPDATE registro_metrica rm
SET valor = LPAD(((SPLIT_PART(TRIM(rm.valor), ':', 1))::int / 60)::text, 2, '0')
    || ':' || LPAD(((SPLIT_PART(TRIM(rm.valor), ':', 1))::int % 60)::text, 2, '0')
    || ':' || SPLIT_PART(TRIM(rm.valor), ':', 2)
FROM metrica m
WHERE m.id = rm.metrica_id
  AND m.tipo = 'TEMPO'
  AND m.unidade IN ('mm:ss', 'hh:mm:ss')
  AND CASE
          WHEN TRIM(rm.valor) ~ '^\d{2,4}:[0-5]\d$'
              THEN (SPLIT_PART(TRIM(rm.valor), ':', 1))::int BETWEEN 60 AND 5999
          ELSE false
      END;
