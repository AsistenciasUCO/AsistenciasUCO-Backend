---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-002 — validación

PR: nueva rama a partir de `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69`, independiente del PR #18 todavía abierto.

| Control | Estado al publicar |
|---|---|
| Lectura contrato y código base | PASS estático |
| Suite nuevo caso de colisión | SOURCE_PREPARED; RED_NOT_CERTIFIED |
| Cambios production | PRIVATE_KEY_RECORD_PREPARED |
| JDK25 clean verify | NOT_RUN |
| Sonar / ArchUnit / CodeQL | NOT_RUN |
| SQL Server `CatalogJpaParityIT` | NOT_RUN |
| Resultado Quality Gate del nuevo SHA | PENDING |

Exigir evidencia remota del nuevo commit antes de declarar GREEN. No cerrar política TTL ni excepciones amplias por esta microfase. Si el nuevo test no compila, reabrir rol tester, congelar nuevamente y reproducir comportamiento. No sustituir evidencia real por métricas del PR #18.
