# Scheduled Background Jobs (`scheduler`)

## Overview
Spring `@Scheduled` background job component that triggers periodic processing of pending student approval reminders.

## Interface
- `[LembreteAlunosPendentesScheduler.java:LembreteAlunosPendentesScheduler]` (`executar`)

## Invariants
- `[LembreteAlunosPendentesScheduler.java:executar]` runs on the cron expression `${app.notificacoes.alunos-pendentes.cron:0 0 * * * *}` and catches all exceptions so scheduler execution never aborts subsequent runs.

## Side Effects
- `[LembreteAlunosPendentesScheduler.java:executar]` invokes `LembreteAlunosPendentesService.processarLembretes()` on a scheduled background thread and logs failures via SLF4J (`log.error`).

## Verification
- Run: `./mvnw test`
