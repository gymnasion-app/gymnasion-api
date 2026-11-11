# Database Migrations (`db/migration`)

## Overview
Flyway PostgreSQL schema migration scripts (`V1` through `V20`) defining and evolving all relational tables, join tables, constraints, and seed data (such as sports modalities) for the Gymnasion backend.

## Interface
- `[V1__create_tables.sql:usuario]` Core user account table (`id`, `email` UNIQUE, `cpf` UNIQUE, `senha_hash`, `celular`, `role`, `data_criacao`).
- `[V1__create_tables.sql:personal_trainer]` Personal trainer profile table (`id`, `usuario_id` UNIQUE FK -> `usuario`, `modalidade_id` FK -> `modalidade`, `ativo`).
- `[V1__create_tables.sql:aluno]` Student profile table (`id`, `usuario_id` UNIQUE FK -> `usuario`, `personal_id` FK -> `personal_trainer`, `status` added in `[V3__update_aluno_columns.sql:status]`).
- `[V1__create_tables.sql:define_usuario_personal_trainer_aluno]` Legacy ternary join table (`usuario_id` FK -> `usuario`, `personal_trainer_id` FK -> `personal_trainer`, `aluno_id` FK -> `aluno`).
- `[V1__create_tables.sql:modalidade]` Sports modality catalog table (`id` BIGSERIAL, `nome`, `descricao`, `ativo` added in `[V19__modalidade_ativo_and_metrica_partial_unique.sql:ativo]`), seeded in `[V2__insert_modalidades.sql]` and `[V20__insert_novas_modalidades.sql]`.
- `[V1__create_tables.sql:praticada]` Student-modality join table (`aluno_id` FK -> `aluno`, `modalidade_id` FK -> `modalidade`).
- `[V1__create_tables.sql:personal_convites]` Temporary invitation token table (`id`, `token` UNIQUE, `personal_id`, `modalidade_id` added in `[V4__add_modalidade_to_personal_convites.sql:modalidade_id]`, `status`, `maximo_usuarios`, `quantidades_usuarios`, `data_expiracao`, `data_criacao`).
- `[V1__create_tables.sql:metrica]` Custom performance metric definition table (`id`, `titulo`, `tipo`, `obrigatoria` and `quem_preenche` added in `[V10__add_obrigatoria_and_quem_preenche_to_metrica.sql]`, `campo_strava` added in `[V16__add_campo_strava_to_metrica.sql:campo_strava]`, `modalidade_id` FK -> `modalidade`, `personal_trainer_id` FK -> `personal_trainer`, `ativo`).
- `[V1__create_tables.sql:turma]` Student group/class table (`id`, `nome`, `personal_id` FK -> `personal_trainer`, `modalidade_id` added in `[V8__add_modalidade_to_turma.sql:modalidade_id]`, `horario`).
- `[V1__create_tables.sql:contem]` Class-student join table (`turma_id` FK -> `turma`, `aluno_id` FK -> `aluno`).
- `[V1__create_tables.sql:rotina_treino]` Training routine table (`id`, `titulo`, `objetivos` added in `[V5__add_objetivo_rotina_treino.sql:objetivos]`, `frequencia_semanal`, `ativo`, `modalidade_id` added in `[V6__add_modalidade_to_rotina_treino.sql:modalidade_id]`, `personal_trainer_id` FK -> `personal_trainer`).
- `[V1__create_tables.sql:segue]` Routine-student join table (`rotina_treino_id` FK -> `rotina_treino`, `aluno_id` FK -> `aluno`).
- `[V7__create_rotina_treino_metrica.sql:rotina_treino_metrica]` Routine-metric join table (`rotina_treino_id` FK -> `rotina_treino`, `metrica_id` FK -> `metrica`).
- `[V1__create_tables.sql:sessao_treino]` Planned and executed workout session table (`id`, `rotina_treino_id` FK -> `rotina_treino`, `semana`, `data_planejada`, `data_real`, `duracao`, `atividades`, `status`, `concluida_aluno`, `validada_personal` updated in `[V11__update_sessao_treino_and_desempenho_for_us10.sql]`).
- `[V1__create_tables.sql:desempenho]` Session performance record table (`id`, `sessao_treino_id` UNIQUE FK -> `sessao_treino`, `aluno_id` FK -> `aluno`, `registrado_por`, `data_registro`, `nivel_cansaco`, `observacoes` updated in `[V11__update_sessao_treino_and_desempenho_for_us10.sql]`).
- `[V1__create_tables.sql:registro_metrica]` Recorded metric value table (`id`, `desempenho_id` FK -> `desempenho`, `metrica_id` FK -> `metrica`, `valor`).
- `[V12__create_auditoria_desempenho.sql:auditoria_desempenho]` Immutable performance correction audit log table (`id`, `desempenho_id` FK -> `desempenho`, `tipo_operacao`, `campo`, `metrica_id` FK -> `metrica`, `valor_anterior`, `valor_novo`, `usuario_id` FK -> `usuario`, `data_hora`).
- `[V9__create_notificacao_table.sql:notificacao]` In-app notification table (`id`, `usuario_id` FK -> `usuario`, `titulo`, `mensagem`, `tipo`, `lida`, `referencia_id`, `data_criacao`).
- `[V13__create_observacao_feedback.sql:observacao_feedback]` Immutable profile observation and feedback table (`id`, `aluno_id` FK -> `aluno`, `autor_id` FK -> `usuario`, `tipo`, `conteudo`, `data_criacao`).
- `[V1__create_tables.sql:relatorio]` Consolidated performance report table (`id`, `data_inicio`, `data_fim`, `personal_trainer_id` FK -> `personal_trainer`).
- `[V1__create_tables.sql:consolida]` Report-performance join table (`relatorio_id` FK -> `relatorio`, `desempenho_id` FK -> `desempenho`).
- `[V1__create_tables.sql:integracao_strava]` Strava OAuth 2.0 integration table (`id`, `aluno_id` UNIQUE FK -> `aluno`, `access_token`, `refresh_token`, `expira_em`, `atleta_id` and `nome_atleta` added in `[V14__add_atleta_fields_to_integracao_strava.sql]`, `escopo`, `conectado_em`, `reconexao_pendente` added in `[V15__add_controle_conexao_to_integracao_strava.sql]`).
- `[V17__create_preferencia_usuario.sql:preferencia_usuario]` User notification preferences table (`id`, `usuario_id` UNIQUE FK -> `usuario`, `show_notification_badge`, `notificacoes_email`, `notificacoes_push`, `data_atualizacao`).
- `[V1__create_tables.sql:termo_aceite]` LGPD terms of use acceptance record table (`id`, `usuario_id` FK -> `usuario`, `versao`, `data_aceite`).

## Invariants
- `[V1__create_tables.sql:usuario]` `email` and `cpf` columns enforce uniqueness across all user accounts.
- `[V18__add_unique_index_atleta_id_to_integracao_strava.sql:uk_integracao_strava_atleta_id]` `atleta_id` (`WHERE atleta_id IS NOT NULL`) and `aluno_id` enforce a strict 1:1 unique relationship between a student and a Strava athlete account.
- `[V19__modalidade_ativo_and_metrica_partial_unique.sql:uk_metrica_titulo_personal_modalidade_ativo]` Partial unique index (`WHERE ativo = true`) ensures no duplicate active metric titles (case-insensitive) exist for the same personal trainer and modality while allowing soft-deleted (`ativo = false`) historical metrics.
- Applied migration files (`V1` through `V20`) are checksum-verified by Flyway (`spring.flyway.enabled=true`) and must never be modified in place; schema changes require a new versioned `V<N>__*.sql` file.

## Side Effects
- `[db/migration:V1..V20]` Executes PostgreSQL DDL (`CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`) and seed DML (`INSERT INTO modalidade`) on application startup via Flyway.

## Verification
- Run: `./mvnw test`
