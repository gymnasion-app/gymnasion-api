-- Contatos de interesse captados pela landing page pública (/conheca)
CREATE TABLE lead (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(150)             NOT NULL,
    email             VARCHAR(100)             NOT NULL,
    modalidade        VARCHAR(50),
    aceita_novidades  BOOLEAN                  NOT NULL DEFAULT FALSE,
    data_criacao      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lead_email UNIQUE (email)
);
