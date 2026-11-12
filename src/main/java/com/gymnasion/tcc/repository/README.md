# Persistence Repositories (`repository`)

## Overview
Spring Data JPA repository interfaces providing persistence queries, multi-tenant data isolation by Personal Trainer/Aluno, eager entity graph fetching (`JOIN FETCH`), and bulk update/delete operations against relational tables.

## Interface
- `[AlunoRepository.java:AlunoRepository]` (`findByPersonalIdAndStatus`, `findByPersonalId`, `findByIdAndPersonalId`, `findByUsuario`, `findByUsuarioId`, `countByPersonalIdAndStatus`, `findByStatusCadastradosAntesDe`)
- `[AuditoriaDesempenhoRepository.java:AuditoriaDesempenhoRepository]` (`findByDesempenhoIdOrderByDataHoraAsc`, `existsByMetricaId`)
- `[DesempenhoRepository.java:DesempenhoRepository]` (`findBySessaoTreinoId`, `findByAlunoIdWithMetricas`, `findByAlunoIdAndPeriodoWithMetricas`, `findIntervalosRegistrosByAlunoId`)
- `[IntegracaoStravaRepository.java:IntegracaoStravaRepository]` (`findByAluno`, `existsByAtletaIdAndAlunoIdNot`)
- `[MetricaRepository.java:MetricaRepository]` (`existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCase`, `existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCaseAndAtivoTrue`, `existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCaseAndIdNot`, `existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCaseAndIdNotAndAtivoTrue`, `findByPersonalTrainerIdAndModalidadeId`, `findByIdAndPersonalTrainerId`)
- `[ModalidadeRepository.java:ModalidadeRepository]` (`existsByNomeIgnoreCase`, `existsByNomeIgnoreCaseAndIdNot`, `findByAtivoTrue`, `findByAtivoTrueOrderByNomeAsc`, `findAllByOrderByNomeAsc`)
- `[NotificacaoRepository.java:NotificacaoRepository]` (`findByUsuarioIdOrderByDataCriacaoDesc`, `countByUsuarioIdAndLidaFalse`, `findByUsuarioIdAndTipoAndReferenciaId`, `deleteByUsuarioIdAndTipoAndReferenciaId`, `marcarTodasComoLidas`)
- `[ObservacaoFeedbackRepository.java:ObservacaoFeedbackRepository]` (`findByAlunoIdOrderByDataCriacaoDesc`)
- `[PersonalConvitesRepository.java:PersonalConvitesRepository]` (`findByToken`, `findByPersonalId`, `findByPersonalIdAndStatus`, `incrementarQuantidadeUsuarios`)
- `[PersonalTrainerRepository.java:PersonalTrainerRepository]` (`getByUsuario`, `getById`, `findByUsuarioId`)
- `[PreferenciaUsuarioRepository.java:PreferenciaUsuarioRepository]` (`findByUsuario`, `findByUsuarioId`)
- `[RegistroMetricaRepository.java:RegistroMetricaRepository]` (`existsByMetricaId`, `countAlunosComRegistrosByMetricaId`)
- `[RelatorioRepository.java:RelatorioRepository]`
- `[RotinaTreinoRepository.java:RotinaTreinoRepository]` (`findByPersonalTrainerId`, `findByPersonalTrainerIdAndAlunosId`, `findByAlunosId`, `findByIdAndPersonalTrainerId`, `findByPersonalTrainerIdAndModalidadeId`, `existsByAlunoIdAndAtivoTrue`, `existsByAlunoIdAndAtivoTrueAndIdNot`, `existsByAlunoIdAndModalidadeIdAndAtivoTrue`, `existsByAlunoIdAndModalidadeIdAndAtivoTrueAndIdNot`, `findByAlunoIdAndModalidadeIdAndAtivoTrue`, `findMetricasAtivasByRotinaIds`)
- `[SessaoTreinoRepository.java:SessaoTreinoRepository]` (`findByRotinaTreinoIdOrderBySemanaAscDataPlanejadaAsc`, `findByRotinaTreinoIdAndSemanaOrderByDataPlanejadaAsc`, `findByRotinaTreinoIdAndStatusOrderByDataRealDesc`, `findByIdAndPersonalTrainerId`, `findByIdAndAlunoId`, `findByAlunoIdAndStatusOrderByDataRealDesc`, `existsByMetricaIdAndStatus`, `findAlunosByMetricaIdAndStatus`, `findAlunosByMetricaId`, `removerMetricaDeSessoesPlanejadas`)
- `[TermoAceiteRepository.java:TermoAceiteRepository]` (`deleteByUsuario`)
- `[TurmaRepository.java:TurmaRepository]` (`findByPersonalId`, `findByIdAndPersonalId`)
- `[UsuarioRepository.java:UsuarioRepository]` (`findByEmail`, `existsByEmail`, `existsByCpf`)

## Invariants
- `[DesempenhoRepository.java:findByAlunoIdAndPeriodoWithMetricas]` and `[DesempenhoRepository.java:findIntervalosRegistrosByAlunoId]` evaluate session dates using `COALESCE(s.dataReal, s.dataPlanejada)` so unexecuted or fallback dates resolve consistently.
- `[AlunoRepository.java:findByIdAndPersonalId]`, `[MetricaRepository.java:findByIdAndPersonalTrainerId]`, `[RotinaTreinoRepository.java:findByIdAndPersonalTrainerId]`, `[SessaoTreinoRepository.java:findByIdAndPersonalTrainerId]`, and `[TurmaRepository.java:findByIdAndPersonalId]` scope entity lookups by `personalId` / `personalTrainerId` to enforce multi-tenant isolation.

## Side Effects
- Executes SQL/JPQL read and write operations against the relational database via Spring Data JPA (`JpaRepository`).
- `[NotificacaoRepository.java:marcarTodasComoLidas]` executes a `@Modifying` bulk `UPDATE Notificacao n SET n.lida = true` for all unread notifications of a user.
- `[PersonalConvitesRepository.java:incrementarQuantidadeUsuarios]` executes a `@Modifying` bulk `UPDATE PersonalConvites p SET p.quantidadesUsuarios = p.quantidadesUsuarios + 1` for the invite ID.
- `[NotificacaoRepository.java:deleteByUsuarioIdAndTipoAndReferenciaId]` and `[TermoAceiteRepository.java:deleteByUsuario]` delete matching rows from the database.
- `[SessaoTreinoRepository.java:removerMetricaDeSessoesPlanejadas]` executes a native `@Modifying` `DELETE FROM sessao_treino_metrica` scoped to `PLANEJADA` sessions when a metric is deleted or discontinued.

## Verification
- Run: `./mvnw test`
