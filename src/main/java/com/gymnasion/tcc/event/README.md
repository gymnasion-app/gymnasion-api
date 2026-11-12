# Domain Events & Listeners (`event`)

## Overview
Domain event records and asynchronous post-commit transactional listeners (`@Async`, `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`) that trigger email notifications for student invitations, pending student reminders, and registration approval/rejection.

## Interface
- `[ConviteAlunoCriadoEvent.java:ConviteAlunoCriadoEvent]`
- `[ConviteEmailListener.java:ConviteEmailListener]` (`onConviteCriado`)
- `[LembreteAlunosPendentesEmailListener.java:LembreteAlunosPendentesEmailListener]` (`onLembrete`)
- `[LembreteAlunosPendentesEvent.java:LembreteAlunosPendentesEvent]`
- `[SolicitacaoAlunoAvaliadaEvent.java:SolicitacaoAlunoAvaliadaEvent]`
- `[SolicitacaoAlunoEmailListener.java:SolicitacaoAlunoEmailListener]` (`onSolicitacaoAvaliada`)

## Invariants
- `[ConviteEmailListener.java:onConviteCriado]`, `[LembreteAlunosPendentesEmailListener.java:onLembrete]`, and `[SolicitacaoAlunoEmailListener.java:onSolicitacaoAvaliada]` execute only after the publishing database transaction commits (`TransactionPhase.AFTER_COMMIT`) and short-circuit without calling `EmailService` when the recipient email is `null` or blank.

## Side Effects
- `[ConviteEmailListener.java:onConviteCriado]` dispatches an asynchronous invitation email via `EmailService.enviarConviteAluno` on an `@Async` thread.
- `[LembreteAlunosPendentesEmailListener.java:onLembrete]` dispatches an asynchronous pending-students reminder email via `EmailService.notificarAlunosPendentes` on an `@Async` thread.
- `[SolicitacaoAlunoEmailListener.java:onSolicitacaoAvaliada]` dispatches an asynchronous approval (`EmailService.notificarAprovacaoAluno`) or rejection (`EmailService.notificarRecusaAluno`) email on an `@Async` thread.

## Verification
- Run: `./mvnw test`
