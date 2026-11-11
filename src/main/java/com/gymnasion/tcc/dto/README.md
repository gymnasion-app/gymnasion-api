# Data Transfer Objects (`dto`)

## Overview
Immutable Java `record` Data Transfer Objects (DTOs) defining REST API request payloads, Jakarta Bean Validation constraints, OpenAPI schemas, and response projections across authentication, student/trainer management, modalities, custom metrics, classes, workout routines, sessions, performance auditing, evolution dashboards, PDF reports, notifications, and Strava integration.

## Interface
- `[AcompanhamentoAlunoResponseDTO.java:AcompanhamentoAlunoResponseDTO]`
- `[AlterarSenhaRequestDTO.java:AlterarSenhaRequestDTO]`
- `[AlunoResponseDTO.java:AlunoResponseDTO]`
- `[AtribuirRotinaAlunoRequestDTO.java:AtribuirRotinaAlunoRequestDTO]`
- `[AtualizarMetricaRequestDTO.java:AtualizarMetricaRequestDTO]`
- `[AtualizarRotinaTreinoRequestDTO.java:AtualizarRotinaTreinoRequestDTO]`
- `[AtualizarSessaoTreinoRequestDTO.java:AtualizarSessaoTreinoRequestDTO]`
- `[AtualizarTurmaRequestDTO.java:AtualizarTurmaRequestDTO]`
- `[AtualizarUsuarioRequestDTO.java:AtualizarUsuarioRequestDTO]`
- `[AuditoriaDesempenhoResponseDTO.java:AuditoriaDesempenhoResponseDTO]` (`metricaTitulo`, `tipoAlteracao`, `responsavelNome`, `dataHoraAlteracao`)
- `[ConviteResponseDTO.java:ConviteResponseDTO]`
- `[CorrigirDesempenhoRequestDTO.java:CorrigirDesempenhoRequestDTO]`
- `[CriarMetricaRequestDTO.java:CriarMetricaRequestDTO]`
- `[CriarObservacaoFeedbackRequestDTO.java:CriarObservacaoFeedbackRequestDTO]`
- `[CriarRotinaTreinoRequestDTO.java:CriarRotinaTreinoRequestDTO]`
- `[CriarSessaoTreinoRequestDTO.java:CriarSessaoTreinoRequestDTO]`
- `[CriarTurmaRequestDTO.java:CriarTurmaRequestDTO]`
- `[DashboardEvolucaoResponseDTO.java:DashboardEvolucaoResponseDTO]` (`AlunoResumoDashboardDTO`, `ModalidadeResumoDashboardDTO`, `PeriodoDashboardDTO`, `ResumoGeralDashboardDTO`, `SerieMetricaDashboardDTO`, `TendenciaMetricaDTO`, `PontoMetricaDashboardDTO`, `EvolucaoRpeDashboardDTO`, `PontoRpeDashboardDTO`)
- `[DesempenhoDetalheResponseDTO.java:DesempenhoDetalheResponseDTO]` (`MetricaDesempenhoDTO`)
- `[DesempenhoResponseDTO.java:DesempenhoResponseDTO]`
- `[FormularioExecucaoResponseDTO.java:FormularioExecucaoResponseDTO]`
- `[GerarConviteRequestDTO.java:GerarConviteRequestDTO]`
- `[IntervaloRegistrosModalidadeDTO.java:IntervaloRegistrosModalidadeDTO]`
- `[LoginRequestDTO.java:LoginRequestDTO]`
- `[MensagemResponseDTO.java:MensagemResponseDTO]`
- `[MetricaPendenteDTO.java:MetricaPendenteDTO]`
- `[MetricaResponseDTO.java:MetricaResponseDTO]`
- `[ModalidadeRequestDTO.java:ModalidadeRequestDTO]`
- `[ModalidadeResponseDTO.java:ModalidadeResponseDTO]`
- `[NotificacaoResponseDTO.java:NotificacaoResponseDTO]`
- `[ObservacaoFeedbackResponseDTO.java:ObservacaoFeedbackResponseDTO]`
- `[PageResponseDTO.java:PageResponseDTO]` (`from`)
- `[PlanejamentoRotinaResponseDTO.java:PlanejamentoRotinaResponseDTO]` (`SemanaPlanejamentoDTO`)
- `[PreferenciaUsuarioRequestDTO.java:PreferenciaUsuarioRequestDTO]`
- `[PreferenciaUsuarioResponseDTO.java:PreferenciaUsuarioResponseDTO]`
- `[RegistrarExecucaoSessaoRequestDTO.java:RegistrarExecucaoSessaoRequestDTO]`
- `[RegistrarResultadoMetricaDTO.java:RegistrarResultadoMetricaDTO]`
- `[RegistroMetricaResponseDTO.java:RegistroMetricaResponseDTO]`
- `[RegistroPersonalCompletoDTO.java:RegistroPersonalCompletoDTO]`
- `[RelatorioPdfDTO.java:RelatorioPdfDTO]`
- `[RelatorioPerformanceDTO.java:RelatorioPerformanceDTO]` (`SecaoModalidadeRelatorioDTO`, `SessaoRelatorioDTO`)
- `[ResumoPendenciasResponseDTO.java:ResumoPendenciasResponseDTO]` (`AlunoPendenciaItemDTO`)
- `[ReutilizarRotinaRequestDTO.java:ReutilizarRotinaRequestDTO]`
- `[RotinaTreinoResponseDTO.java:RotinaTreinoResponseDTO]` (`AlunoResumoDTO`, `MetricaResumoDTO`)
- `[SessaoAcompanhamentoDTO.java:SessaoAcompanhamentoDTO]`
- `[SessaoTreinoResponseDTO.java:SessaoTreinoResponseDTO]`
- `[StravaAtividadeDTO.java:StravaAtividadeDTO]`
- `[StravaAuthUrlResponseDTO.java:StravaAuthUrlResponseDTO]`
- `[StravaConectarRequestDTO.java:StravaConectarRequestDTO]`
- `[StravaStatusResponseDTO.java:StravaStatusResponseDTO]`
- `[TokenResponseDTO.java:TokenResponseDTO]` (`of`)
- `[TurmaResponseDTO.java:TurmaResponseDTO]` (`AlunoResumoDTO`)
- `[UsuarioPerfilResponseDTO.java:UsuarioPerfilResponseDTO]`
- `[UsuarioRequestDTO.java:UsuarioRequestDTO]`
- `[UsuarioResponseDTO.java:UsuarioResponseDTO]`
- `[VincularAlunosRotinaRequestDTO.java:VincularAlunosRotinaRequestDTO]`

## Invariants
- `[CriarRotinaTreinoRequestDTO.java:frequenciaSemanal]` and `[AtualizarRotinaTreinoRequestDTO.java:frequenciaSemanal]` must be between 1 and 7 days (`@Min(1)`, `@Max(7)`), and `[CriarRotinaTreinoRequestDTO.java:alunosIds]` / `[CriarRotinaTreinoRequestDTO.java:metricasIds]` must be non-empty (`@NotEmpty`).
- `[RegistrarExecucaoSessaoRequestDTO.java:nivelCansaco]` Borg CR-10 RPE effort rating must be between 1 and 10 (`@Min(1)`, `@Max(10)`) when provided, and `[RegistrarExecucaoSessaoRequestDTO.java:duracao]` must be at least 1 minute (`@Min(1)`).
- `[CriarSessaoTreinoRequestDTO.java:semana]` and `[AtualizarSessaoTreinoRequestDTO.java:semana]` must be `>= 1` (`@Min(1)`) with non-null planned date (`dataPlanejada`) and `duracao >= 1` minute.
- `[RegistroPersonalCompletoDTO.java:password]` and `[UsuarioRequestDTO.java:password]` require 8 to 255 characters (`@Size(min = 8, max = 255)`), and `cpf` requires 11 characters (`@Size(min = 11, max = 11)`).
- `[CriarMetricaRequestDTO.java:titulo]` and `[AtualizarMetricaRequestDTO.java:titulo]` must be non-blank and at most 100 characters (`@Size(max = 100)`); `[CriarObservacaoFeedbackRequestDTO.java:conteudo]` must be non-blank and at most 2000 characters (`@Size(max = 2000)`); `[ModalidadeRequestDTO.java:nome]` must be non-blank and at most 50 characters (`@Size(max = 50)`).
- `[CriarTurmaRequestDTO.java:alunosIds]` and `[VincularAlunosRotinaRequestDTO.java:alunosIds]` require at least 1 student ID (`@NotEmpty`).
- `[AuditoriaDesempenhoResponseDTO.java:metricaTitulo]`, `[AuditoriaDesempenhoResponseDTO.java:tipoAlteracao]`, `[AuditoriaDesempenhoResponseDTO.java:responsavelNome]`, and `[AuditoriaDesempenhoResponseDTO.java:dataHoraAlteracao]` expose Jackson `@JsonProperty` alias getters preserving backward-compatible JSON serialization keys.

## Side Effects
- None

## Verification
- Run: `./mvnw test`
