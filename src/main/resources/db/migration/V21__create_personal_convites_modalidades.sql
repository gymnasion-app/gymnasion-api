CREATE TABLE personal_convites_modalidades (
    convite_id UUID NOT NULL REFERENCES personal_convites(id) ON DELETE CASCADE,
    modalidade_id BIGINT NOT NULL REFERENCES modalidade(id) ON DELETE CASCADE,
    PRIMARY KEY (convite_id, modalidade_id)
);

INSERT INTO personal_convites_modalidades (convite_id, modalidade_id)
SELECT id, modalidade_id
FROM personal_convites
WHERE modalidade_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE personal_convites
ALTER COLUMN modalidade_id DROP NOT NULL;
