# Domain & Application Exceptions (`exceptions`)

## Overview
Domain and application runtime exception hierarchy representing business rule violations, temporary login lockouts, invalid metric submissions, missing resources, duplicate entities, and empty report periods.

## Interface
- `[BusinessException.java:BusinessException]`
- `[ContaBloqueadaException.java:ContaBloqueadaException]` (`getTentativas`, `getMinutosBloqueio`)
- `[DuplicateResourceException.java:DuplicateResourceException]`
- `[MetricasInvalidasException.java:MetricasInvalidasException]` (`getCamposInvalidos`)
- `[NotFoundException.java:NotFoundException]`
- `[RelatorioSemDadosException.java:RelatorioSemDadosException]` (`getDataInicioSugerida`, `getDataFimSugerida`, `possuiPeriodoSugerido`)

## Invariants
- `[ContaBloqueadaException.java:ContaBloqueadaException]` defaults to `tentativas = 5` and `minutosBloqueio = 5` (RN-010).
- `[MetricasInvalidasException.java:camposInvalidos]` stores an unmodifiable insertion-ordered map (`Collections.unmodifiableMap(new LinkedHashMap<>(camposInvalidos))`) and uses the single field error message directly when `camposInvalidos.size() == 1`.
- `[RelatorioSemDadosException.java:possuiPeriodoSugerido]` returns `true` only when both `dataInicioSugerida` and `dataFimSugerida` are non-null.

## Side Effects
- None

## Verification
- Run: `./mvnw test`
