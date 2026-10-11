---
status: RESOLVED_PENDING_DB_APPROVAL
work_item: MAINT-003H / CC-003G-01
date: 2026-10-10
evidence_original_run: .workspace/m3d @ e45c36a, target/failsafe-reports (2026-10-10 03:27)
---

# MAINT-003H — Los 3 failures y los 3 skips de Failsafe, uno por uno

## Corrida original (antes de este trabajo)

`failsafe-summary.xml` (sha256 `c77a0a440305e06b…`): `completed=191, failures=3, errors=0, skipped=3`, resultado `255`. Backend
`e45c36a`, DB aislada desplegada desde `feat/utc-d06-post-freeze@3842f70`. Los XML originales se conservan sin tocar en el worktree local
`.workspace/m3d/target/failsafe-reports/` (hashes abajo); no se versionan por contener trazas de entorno.

### Failures (todos con la misma causa raiz: proveedores publicos ausentes en la DB)

| # | Test (XML) | sha256 XML | Mensaje / traza original | Causa | Estado ahora |
|---|---|---|---|---|---|
| F1 | `SqlStoredProcedureContractIT.stored_procedure_publico_coincide_con_el_contrato_real_de_la_db(String, List)[1]` (`usp_sincronizar_usuario`), `SqlStoredProcedureContractIT.java:64` | `27165689fc49…` | `Contrato SQL incompatible: usp_sincronizar_usuario esperaba procedimiento almacenado existente ==> expected: <true> but was: <false>` | `sys.parameters` sin filas: el SP no existe en la DB publicada | **PASS** |
| F2 | mismo test `[7]` (`usp_registrar_o_actualizar_plan_estudio`), mismo `:64` | (mismo XML) | idem para `usp_registrar_o_actualizar_plan_estudio` | idem | **PASS** (con la firma nueva de 5 parametros) |
| F3 | `UsuarioPasswordHashSqlServerIT.crear_usuario_persiste_hash_verificable_y_no_password_plano`, `UsuarioPasswordHashSqlServerIT.java:64` | `d3224e877580…` | `DatabaseOperationException: No fue posible ejecutar el procedimiento almacenado` <- `SQLGrammarException` <- `SQLServerException: Could not find stored procedure 'dbo.usp_sincronizar_usuario'` (`UsuarioJpaRepository.sincronizarUsuario:122` <- `crearUsuario:65` <- `CrearUsuarioUseCaseImpl:37`) | depende de F1 | **PASS** |

No hubo `errors`. Ningun otro test fallo en la corrida original; `AcademicUserCommandsSpParityIT` no falla porque solo imprime resultados
(`PARITY_OUTCOME`), por eso no detectaba la ausencia de los proveedores.

### Skips (todos `Assumptions.assumeTrue` por datos ausentes en la base aislada)

| # | Test | Asuncion / linea | Mensaje | Aprobacion previa | Estado ahora |
|---|---|---|---|---|---|
| S1 | `DocenteRepositorySqlServerIT.docente_puede_no_tener_filas_en_uv_docente` | `DocenteRepositorySqlServerIT.java:74` | `No hay docente sin asignaciones para esta base local.` | [TD-044](../../baseline/TECHNICAL_DEBT.md) — OPEN / NON_BLOCKING (condicion del auditor de LB-001B.4) | **PASS (ejecutado)** |
| S2 | `DocenteRepositorySqlServerIT.uv_docente_puede_devolver_multiples_filas_para_un_docente` | `DocenteRepositorySqlServerIT.java:53` | `No hay docente con multiples asignaciones para esta base local.` | TD-044 | **PASS (ejecutado)** |
| S3 | `AcademicQueryJpaParityIT.horarios_conservan_horas_locales_y_nulos` | `AcademicQueryJpaParityIT.java:164` | `No hay horarios de docente vivos.` | **Ninguna deuda ni aprobacion registrada**: solo aparece como «preservado y reportado» en `MAINT-003G-remediation/VALIDATION.md`. Se registra ahora bajo TD-044 | **PASS (ejecutado)** |

## Como se resolvieron (sin ocultar nada)

- **F1–F3**: el contrato DB se implemento en el repo DB (`feat/cc-003g-01-public-user-plan-providers`, ver [CONTRACT_DECISION](CONTRACT_DECISION.md)).
  Las expectativas de `SqlStoredProcedureContractIT` para `usp_sincronizar_usuario` y todos los demas SP **no cambian**; la unica edicion de
  contrato es agregar `@idUsuarioEjecutor` a la firma de `usp_registrar_o_actualizar_plan_estudio` por la decision P1 (documentada y con la
  prueba dedicada); no se uso `@Disabled`, `assume`, mocks ni alias al SP interno.
- **S1–S3**: no se editaron los tests ni sus `assumeTrue` (`git diff e45c36a..HEAD` sobre `DocenteRepositorySqlServerIT` y
  `AcademicQueryJpaParityIT` esta vacio). Se agrego un **fixture de datos aislado** en el repo DB
  (`test/fixtures/maint_003h_failsafe_data_fixture.sql`, ejecutado por `test/prepare_java_integration_fixture.ps1` solo contra el contenedor de
  pruebas; no es seed de despliegue): un docente con 2 grupos, un docente sin grupos y horarios vivos. Con esos datos los tres escenarios
  **ejecutan sus aserciones reales** y pasan. Esto cierra TD-044 solo para entornos que apliquen el fixture; en una base sin el fixture los
  mismos tests volverian a omitirse (por eso TD-044 se mantiene registrada como dependiente de datos y se anota el fixture como su remediacion).

## Corrida posterior (SHA exactos en [VALIDATION](VALIDATION.md))

`failsafe-summary.xml`: `completed=196, failures=0, errors=0, skipped=0` (191 originales + 5 nuevos de `UsuarioPlanEstudioProvidersSqlServerIT`).
Verificado por clase: `SqlStoredProcedureContractIT` 20/20, `UsuarioPasswordHashSqlServerIT` 1/1, `DocenteRepositorySqlServerIT` 3/3 sin skips,
`AcademicQueryJpaParityIT` 8/8 sin skips, `UsuarioPlanEstudioProvidersSqlServerIT` 5/5, `AcademicUserCommandsSpParityIT` 8/8.
