# Domain Enumerations (`domain/enums`)

## Overview
Domain enumeration types defining system security roles, student invitation statuses, training session lifecycle states, custom metric data types and fill responsibilities, Strava API v3 field mappings, notification categories, profile entry types, and performance audit metadata.

## Interface
- `[CampoAuditado.java:CampoAuditado]` - Enum (`METRICA`, `DATA_REAL`, `OBSERVACOES`) identifying performance record fields affected by audit inclusion or correction.
- `[CampoStrava.java:CampoStrava]` - Enum (`DISTANCIA_KM`, `DISTANCIA_METROS`, `TEMPO_MOVIMENTO`, `TEMPO_TOTAL`, `DURACAO_MINUTOS`, `VELOCIDADE_MEDIA_KMH`, `VELOCIDADE_MAXIMA_KMH`, `RITMO_MEDIO`, `FREQUENCIA_CARDIACA_MEDIA`, `FREQUENCIA_CARDIACA_MAXIMA`, `CADENCIA_MEDIA`, `POTENCIA_MEDIA_WATTS`, `POTENCIA_MAXIMA_WATTS`, `ELEVACAO_GANHO_METROS`, `CALORIAS`, `NENHUM`) mapping Strava API v3 activity fields to custom metrics.
- `[CampoStrava.java:isCompativelCom]` - `public boolean isCompativelCom(TipoMetrica tipo)` validating compatibility between a Strava field and a metric data type.
- `[ResponsavelPreenchimento.java:ResponsavelPreenchimento]` - Enum (`ALUNO`, `PERSONAL`, `AMBOS`) specifying who can record a custom metric during workout execution.
- `[Role.java:Role]` - Enum (`ADMIN`, `ALUNO`, `PERSONAL_TRAINER`) defining user authorization roles.
- `[StatusConvite.java:StatusConvite]` - Enum (`INATIVO`, `ATIVO`, `PENDENTE`, `RECUSADO`) representing invitation and student link lifecycle states.
- `[StatusSessaoTreino.java:StatusSessaoTreino]` - Enum (`PLANEJADA`, `CONCLUIDA`) representing training session execution states.
- `[TipoMetrica.java:TipoMetrica]` - Enum (`NUMERICO`, `TEMPO`, `FREQUENCIA_CARDIACA`, `TEXTO`) defining custom metric value formats.
- `[TipoNotificacao.java:TipoNotificacao]` - Enum (`NOVA_SOLICITACAO`, `ALUNOS_PENDENTES`, `TREINO_CONCLUIDO`, `CONVITE_ESGOTADO`, `STRAVA_EXPIRADO`, `RELATORIO_PRONTO`, `SOLICITACAO_APROVADA`, `METRICAS_PENDENTES`) categorizing system notifications.
- `[TipoOperacaoAuditoria.java:TipoOperacaoAuditoria]` - Enum (`INCLUSAO`, `CORRECAO`) distinguishing initial value entry from modification in performance audits.
- `[TipoRegistroPerfil.java:TipoRegistroPerfil]` - Enum (`FEEDBACK`, `OBSERVACAO`) distinguishing trainer feedback from student observations on a student profile.

## Invariants
- `[CampoStrava.java:isCompativelCom]` returns `true` when `this == NENHUM` or `tipo == null`; requires `TipoMetrica.NUMERICO` for `DISTANCIA_KM`, `DISTANCIA_METROS`, `DURACAO_MINUTOS`, `VELOCIDADE_MEDIA_KMH`, `VELOCIDADE_MAXIMA_KMH`, `CADENCIA_MEDIA`, `POTENCIA_MEDIA_WATTS`, `POTENCIA_MAXIMA_WATTS`, `ELEVACAO_GANHO_METROS`, and `CALORIAS`; requires `TipoMetrica.TEMPO` for `TEMPO_MOVIMENTO` and `TEMPO_TOTAL`; requires `TipoMetrica.FREQUENCIA_CARDIACA` for `FREQUENCIA_CARDIACA_MEDIA` and `FREQUENCIA_CARDIACA_MAXIMA`; requires `TipoMetrica.TEXTO` or `TipoMetrica.NUMERICO` for `RITMO_MEDIO`.

## Side Effects
- None

## Verification
- Run: `./mvnw test`
