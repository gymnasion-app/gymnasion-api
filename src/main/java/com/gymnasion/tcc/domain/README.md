# Domain Entities (`domain`)

## Overview
Core JPA domain entities mapping the Gymnasion relational database tables (`usuario`, `personal_trainer`, `aluno`, `personal_convites`, `modalidade`, `turma`, `rotina_treino`, `sessao_treino`, `desempenho`, `metrica`, `registro_metrica`, `auditoria_desempenho`, `integracao_strava`, `notificacao`, `observacao_feedback`, `preferencia_usuario`, `termo_aceite`, and `relatorio`).

## Interface
- `[Aluno.java:Aluno]` - JPA `@Entity` (`@Table(name = "aluno")`) representing a student linked to `Usuario`, `PersonalTrainer`, `StatusConvite`, and practiced `Modalidade` set (`@JoinTable(name = "praticada")`).
- `[AuditoriaDesempenho.java:AuditoriaDesempenho]` - JPA `@Entity` (`@Table(name = "auditoria_desempenho")`) recording performance audit entries with `TipoOperacaoAuditoria`, `CampoAuditado`, `Metrica`, `valorAnterior`, `valorNovo`, `Usuario`, and `dataHora`.
- `[AuditoriaDesempenho.java:onCreate]` - `@PrePersist` callback initializing `dataHora` when `null`.
- `[Desempenho.java:Desempenho]` - JPA `@Entity` (`@Table(name = "desempenho")`) storing session execution results, Borg CR-10 RPE (`nivelCansaco`), observations, and cascaded `List<RegistroMetrica>`.
- `[Desempenho.java:onCreate]` - `@PrePersist` callback initializing `dataRegistro` when `null`.
- `[IntegracaoStrava.java:IntegracaoStrava]` - JPA `@Entity` (`@Table(name = "integracao_strava")`) persisting Strava OAuth 2.0 tokens, expiration, athlete metadata, scope, and reconnection status for an `Aluno`.
- `[Metrica.java:Metrica]` - JPA `@Entity` (`@Table(name = "metrica")`) defining a custom metric per `Modalidade` and `PersonalTrainer` with `TipoMetrica`, `obrigatoria`, `ResponsavelPreenchimento`, `CampoStrava`, and `ativo` flag.
- `[Modalidade.java:Modalidade]` - JPA `@Entity` (`@Table(name = "modalidade")`) representing a sports modality catalog entry with `IDENTITY` `Long id`, `nome`, `descricao`, and `ativo` flag.
- `[Notificacao.java:Notificacao]` - JPA `@Entity` (`@Table(name = "notificacao")`) representing a user notification with `TipoNotificacao`, `lida` flag, `referenciaId`, and `dataCriacao`.
- `[ObservacaoFeedback.java:ObservacaoFeedback]` - JPA `@Entity` (`@Table(name = "observacao_feedback")`) representing an immutable feedback or observation entry linked to `Aluno` and author `Usuario`.
- `[PersonalConvites.java:PersonalConvites]` - JPA `@Entity` (`@Table(name = "personal_convites")`) storing temporary registration invite tokens, usage counters, expiration timestamps, and target `Modalidade`.
- `[PersonalTrainer.java:PersonalTrainer]` - JPA `@Entity` (`@Table(name = "personal_trainer")`) linking a trainer `Usuario` to a primary `Modalidade` and active status.
- `[PreferenciaUsuario.java:PreferenciaUsuario]` - JPA `@Entity` (`@Table(name = "preferencia_usuario")`) storing notification badge, email, and push preferences for a `Usuario`.
- `[RegistroMetrica.java:RegistroMetrica]` - JPA `@Entity` (`@Table(name = "registro_metrica")`) storing a recorded string value for a specific `Metrica` within a `Desempenho`.
- `[Relatorio.java:Relatorio]` - JPA `@Entity` (`@Table(name = "relatorio")`) consolidating a `Set<Desempenho>` (`@JoinTable(name = "consolida")`) over a date range for a `PersonalTrainer`.
- `[RotinaTreino.java:RotinaTreino]` - JPA `@Entity` (`@Table(name = "rotina_treino")`) defining a training routine with weekly frequency, `Modalidade`, `PersonalTrainer`, linked `Aluno` set (`@JoinTable(name = "segue")`), linked `Metrica` set (`@JoinTable(name = "rotina_treino_metrica")`), and cascaded `List<SessaoTreino>`.
- `[SessaoTreino.java:SessaoTreino]` - JPA `@Entity` (`@Table(name = "sessao_treino")`) representing a planned or completed workout session with week number, planned/actual dates, duration, activities, `StatusSessaoTreino`, completion/validation flags, and cascaded `Desempenho`.
- `[TermoAceite.java:TermoAceite]` - JPA `@Entity` (`@Table(name = "termo_aceite")`) recording user acceptance of a terms-of-use version and timestamp.
- `[TermoAceite.java:onCreate]` - `@PrePersist` callback setting `dataAceite`.
- `[Turma.java:Turma]` - JPA `@Entity` (`@Table(name = "turma")`) grouping a `Set<Aluno>` (`@JoinTable(name = "contem")`) under a `PersonalTrainer`, `Modalidade`, and schedule timestamp.
- `[Usuario.java:Usuario]` - JPA `@Entity` (`@Table(name = "usuario")`) implementing Spring Security `UserDetails` with `UUID id`, `nome`, `email`, `senhaHash`, `cpf`, `dataCriacao`, `celular`, and `Role`.
- `[Usuario.java:getAuthorities]` - Maps `Role.ADMIN` to `ROLE_ADMIN`, `ROLE_PERSONAL_TRAINER`, and `ROLE_ALUNO`; `Role.PERSONAL_TRAINER` to `ROLE_PERSONAL_TRAINER`; `Role.ALUNO` to `ROLE_ALUNO`.
- `[Usuario.java:getPassword]` - Returns `senhaHash` for Spring Security authentication.
- `[Usuario.java:getUsername]` - Returns `email` as the principal username.
- Subdirectory: [enums](./enums/README.md) - Domain enumeration types for roles, statuses, metric formats, Strava fields, notifications, and audit metadata.

## Invariants
- `[Usuario.java:Usuario]` - `email` (length 100) and `cpf` (length 11) must be non-null and unique; `data_criacao` is non-null and non-updatable (`updatable = false`).
- `[Usuario.java:getAuthorities]` - `Role.ADMIN` always grants all three authorities (`ROLE_ADMIN`, `ROLE_PERSONAL_TRAINER`, `ROLE_ALUNO`), whereas `PERSONAL_TRAINER` and `ALUNO` grant only their respective single authority.
- `[Aluno.java:usuario]`, `[PersonalTrainer.java:usuario]`, `[PreferenciaUsuario.java:usuario]` - Enforce unique 1:1 foreign key constraints (`usuario_id`, `nullable = false`, `unique = true`) to `usuario`.
- `[Desempenho.java:sessaoTreino]` - Enforces a unique 1:1 foreign key constraint (`sessao_treino_id`, `nullable = false`, `unique = true`) to `sessao_treino`.
- `[IntegracaoStrava.java:aluno]` - Enforces a unique 1:1 foreign key constraint (`aluno_id`, `nullable = false`, `unique = true`) to `aluno`.
- `[ObservacaoFeedback.java:ObservacaoFeedback]` - Columns `tipo`, `conteudo`, and `data_criacao` are declared with `nullable = false, updatable = false` so records are immutable after persistence.
- `[PersonalConvites.java:PersonalConvites]` - `token` is non-null and unique; defaults to `status = StatusConvite.ATIVO`, `maximoUsuarios = 30`, and `quantidadesUsuarios = 0`.
- `[SessaoTreino.java:SessaoTreino]` - Defaults to `semana = 1`, `status = StatusSessaoTreino.PLANEJADA`, `concluidaAluno = false`, and `validadaPersonal = false`.

## Side Effects
- `[AuditoriaDesempenho.java:onCreate]` - `@PrePersist` mutates `dataHora` to `OffsetDateTime.now()` when `null` prior to SQL insert into `auditoria_desempenho`.
- `[Desempenho.java:onCreate]` - `@PrePersist` mutates `dataRegistro` to `OffsetDateTime.now()` when `null` prior to SQL insert into `desempenho`.
- `[Desempenho.java:registrosMetricas]` - Cascades all persistence operations and orphan removal (`CascadeType.ALL, orphanRemoval = true`) to child `RegistroMetrica` rows in `registro_metrica`.
- `[TermoAceite.java:onCreate]` - `@PrePersist` sets `dataAceite` to `OffsetDateTime.now()` prior to SQL insert into `termo_aceite`.
- `[RotinaTreino.java:sessoes]` - Cascades all persistence operations and orphan removal (`CascadeType.ALL, orphanRemoval = true`) to child `SessaoTreino` rows in `sessao_treino`.
- `[SessaoTreino.java:desempenho]` - Cascades all persistence operations (`CascadeType.ALL`) to the associated `Desempenho` row in `desempenho`.
- `[PersonalConvites.java:dataCriacao]` - Hibernate `@CreationTimestamp` populates `data_criacao` on insert into `personal_convites`.

## Verification
- Run: `./mvnw test`
