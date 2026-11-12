CREATE TABLE sessao_treino_metrica (
    sessao_treino_id UUID NOT NULL,
    metrica_id UUID NOT NULL,
    PRIMARY KEY (sessao_treino_id, metrica_id),
    CONSTRAINT fk_stm_sessao FOREIGN KEY (sessao_treino_id) REFERENCES sessao_treino(id) ON DELETE CASCADE,
    CONSTRAINT fk_stm_metrica FOREIGN KEY (metrica_id) REFERENCES metrica(id) ON DELETE RESTRICT
);

CREATE INDEX idx_stm_sessao ON sessao_treino_metrica(sessao_treino_id);
CREATE INDEX idx_stm_metrica ON sessao_treino_metrica(metrica_id);

INSERT INTO sessao_treino_metrica (sessao_treino_id, metrica_id)
SELECT st.id, rtm.metrica_id
FROM sessao_treino st
INNER JOIN rotina_treino_metrica rtm ON rtm.rotina_treino_id = st.rotina_treino_id
ON CONFLICT DO NOTHING;

INSERT INTO sessao_treino_metrica (sessao_treino_id, metrica_id)
SELECT d.sessao_treino_id, rm.metrica_id
FROM registro_metrica rm
INNER JOIN desempenho d ON d.id = rm.desempenho_id
ON CONFLICT DO NOTHING;

DROP TABLE rotina_treino_metrica;
