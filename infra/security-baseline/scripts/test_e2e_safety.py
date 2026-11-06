"""Pruebas offline (stdlib) de las guardas de los E2E que modifican estado.

Uso: python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"

No requieren Docker, SQL Server, Keycloak ni lab.env: la base y Docker se simulan. Cubren el rechazo de bases no
desechables, la restauración desde la instantánea (sin valores fijos), la detección de residuos, el borrado acotado del usuario
sintético y que ningún secreto viaje en la línea de comandos. Los dos E2E se prueban hasta su guarda: con un destino protegido
terminan con código 2 SIN llamar a Keycloak ni modificar nada.
"""
import contextlib
import io
import os
import sys
import tempfile
import types
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault("USERPROFILE", tempfile.gettempdir())  # lab_pdp calcula rutas al importarse; no lee archivos
import e2e_identity_binding as IDENTITY  # noqa: E402
import e2e_provisioning as PROVISIONING  # noqa: E402
import e2e_profile_assignment as PROFILES  # noqa: E402
import e2e_safety as S  # noqa: E402

DOCENTE = "e1f2a3b4-0000-0000-0000-000000000003"
OTHER = "e1f2a3b4-0000-0000-0000-000000000004"
LINK = "aaaaaaaa-0000-0000-0000-000000000001"
EXTRA_LINK = "aaaaaaaa-0000-0000-0000-000000000002"
ISSUER = "http://keycloak.localhost:19090/realms/security-baseline"


def container(name="asistencias-e2e-sqlserver", label="true", host_port="14333"):
    return {"Name": "/" + name, "Config": {"Labels": {S.LABEL: label} if label else {}},
            "HostConfig": {"PortBindings": {"1433/tcp": [{"HostIp": "127.0.0.1", "HostPort": host_port}]}}}


class FakeTarget:
    """Responde a consultas T-SQL por fragmento y registra todo lo ejecutado."""

    def __init__(self, answers=None, database="gestionasistenciadb_e2e", name="asistencias-e2e-sqlserver"):
        self.container, self.database, self.answers, self.executed = name, database, answers or {}, []

    def _answer(self, query):
        self.executed.append(query)
        for fragment, answer in self.answers.items():
            if fragment in query:
                return answer
        return ""

    def sql(self, query, database=None):
        return str(self._answer(query))

    def rows(self, query, database=None):
        answer = self._answer(query)
        return answer if isinstance(answer, list) else [line.split("|") for line in str(answer).splitlines() if line]

    def scalar(self, query, database=None):
        found = self.rows(query)
        return found[0][0] if found else None


def disposable_target():
    return FakeTarget({"sys.extended_properties": "true", "sys.dm_exec_sessions": "3"})


def refused(call):
    with contextlib.redirect_stderr(io.StringIO()):
        with unittest.TestCase().assertRaises(S.UnsafeTarget) as caught:
            call()
    return caught.exception


class ContainerGuardTest(unittest.TestCase):
    def test_clean_disposable_container_has_no_problems(self):
        self.assertEqual(S.container_problems(container(), "gestionasistenciadb_e2e"), [])

    def test_development_container_is_protected_even_if_labelled_and_on_another_port(self):
        problems = S.container_problems(container(name="sql_server_asistencias", host_port="14333"), "gestionasistenciadb_e2e")
        self.assertTrue(any("protegida" in p for p in problems))

    def test_missing_label_is_rejected(self):
        self.assertTrue(any(S.LABEL in p for p in S.container_problems(container(label=None), "x_e2e")))
        self.assertTrue(any(S.LABEL in p for p in S.container_problems(container(label="false"), "x_e2e")))

    def test_container_publishing_the_development_port_is_rejected(self):
        self.assertTrue(any("1433" in p for p in S.container_problems(container(host_port="1433"), "x_e2e")))

    def test_habitual_development_database_name_is_rejected(self):
        self.assertTrue(any("gestionasistenciadb" in p for p in S.container_problems(container(), "gestionasistenciadb")))


class RequireDisposableTest(unittest.TestCase):
    def require(self, target, info=None, pid=4242, backend="http://127.0.0.1:8080"):
        return S.require_disposable(target, backend, inspect=lambda name: info or container(), listener=lambda port: pid)

    def test_unlabelled_container_is_rejected_without_querying_the_database(self):
        target = disposable_target()
        refused(lambda: self.require(target, info=container(label=None)))
        self.assertEqual(target.executed, [])

    def test_development_container_is_rejected_without_querying_the_database(self):
        target = disposable_target()
        refused(lambda: self.require(target, info=container(name="sql_server_asistencias")))
        self.assertEqual(target.executed, [])

    def test_labelled_container_without_database_marker_is_rejected(self):
        target = FakeTarget({"sys.extended_properties": "", "sys.dm_exec_sessions": "3"})
        error = refused(lambda: self.require(target))
        self.assertTrue(any(S.MARKER_PROPERTY in p for p in error.problems))

    def test_marker_with_another_value_is_rejected(self):
        target = FakeTarget({"sys.extended_properties": "false", "sys.dm_exec_sessions": "3"})
        refused(lambda: self.require(target))

    def test_backend_not_connected_to_the_disposable_database_is_rejected(self):
        target = FakeTarget({"sys.extended_properties": "true", "sys.dm_exec_sessions": "0"})
        error = refused(lambda: self.require(target))
        self.assertTrue(any("sesiones" in p for p in error.problems))

    def test_backend_that_is_not_listening_is_rejected(self):
        refused(lambda: self.require(disposable_target(), pid=None))

    def test_remote_backend_is_rejected(self):
        refused(lambda: self.require(disposable_target(), backend="http://10.0.0.5:8080"))

    def test_all_three_signals_together_are_accepted(self):
        target = disposable_target()
        self.assertIs(self.require(target), target)
        self.assertTrue(any("host_process_id = 4242" in q for q in target.executed))

    def test_rejection_exits_with_code_two(self):
        self.assertEqual(refused(lambda: self.require(disposable_target(), info=container(label=None))).code, 2)


class ParsingTest(unittest.TestCase):
    NETSTAT = ("  TCP    0.0.0.0:1433     0.0.0.0:0   LISTENING   111\n"
               "  TCP    127.0.0.1:8080   0.0.0.0:0   LISTENING   222\n"
               "  TCP    127.0.0.1:8080   127.0.0.1:55000   ESTABLISHED   333\n"
               "  TCP    [::]:18080       [::]:0      LISTENING   444\n")

    def test_listener_pid_ignores_established_and_other_ports(self):
        self.assertEqual(S.listener_pid(8080, self.NETSTAT), 222)
        self.assertEqual(S.listener_pid(18080, self.NETSTAT), 444)
        self.assertIsNone(S.listener_pid(9999, self.NETSTAT))

    def test_listener_pid_does_not_depend_on_the_windows_language(self):
        spanish = ("  TCP    127.0.0.1:8080   0.0.0.0:0   ESCUCHANDO   222\n"
                   "  TCP    127.0.0.1:8080   127.0.0.1:55000   ESTABLECIDO   333\n")
        self.assertEqual(S.listener_pid(8080, spanish), 222)
        self.assertIsNone(S.listener_pid(8080, None))

    def test_cli_requires_container_and_password_file(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                S.parse_args([], "x")
            with self.assertRaises(SystemExit):
                S.parse_args(["--sql-container", "c"], "x")
        args = S.parse_args(["--sql-container", "c", "--sql-password-file", "f"], "x")
        self.assertEqual((args.database, args.backend), (S.DEFAULT_DATABASE, S.DEFAULT_BACKEND))

    def test_guid_and_identifier_reject_injection(self):
        self.assertEqual(S.guid(DOCENTE.upper()), DOCENTE)
        for bad in ("x'; DROP TABLE dbo.Usuario;--", DOCENTE + "'", "", None):
            with self.assertRaises(ValueError):
                S.guid(bad)
        with self.assertRaises(ValueError):
            S.identifier("db; DROP")
        self.assertEqual(S.literal("o'brien"), "N'o''brien'")

    def test_password_travels_by_environment_not_by_command_line(self):
        captured = {}

        def runner(command, **kwargs):
            captured.update(command=command, env=kwargs["env"])
            return types.SimpleNamespace(returncode=0, stdout="ok\n", stderr="")

        target = S.Target("c", "s3cr3t-value", "db_e2e", runner=runner)
        self.assertEqual(target.sql("SELECT 1"), "ok")
        self.assertNotIn("s3cr3t-value", " ".join(captured["command"]))
        self.assertEqual(captured["env"]["SQLCMDPASSWORD"], "s3cr3t-value")
        self.assertIn("SQLCMDPASSWORD", captured["command"])

    def test_sql_failure_does_not_echo_the_password(self):
        runner = lambda command, **kwargs: types.SimpleNamespace(returncode=1, stdout="Login failed", stderr="")  # noqa: E731
        with self.assertRaises(RuntimeError) as caught:
            S.Target("c", "s3cr3t-value", "db_e2e", runner=runner).sql("SELECT 1")
        self.assertNotIn("s3cr3t-value", str(caught.exception))


def snapshot(users=None, identities=None, counts=None):
    return {"users": users if users is not None else {DOCENTE: "1", OTHER: "1"},
            "identities": identities if identities is not None else {LINK: (DOCENTE, ISSUER, "sub-1", "1", "NULL")},
            "counts": counts if counts is not None else {"dbo.Usuario": 5, "dbo.IdentidadExterna": 1}}


class RestoreTest(unittest.TestCase):
    def test_identical_state_needs_no_statements(self):
        self.assertEqual(S.restore_statements(snapshot(), snapshot()), [])
        self.assertEqual(S.residue_report(snapshot(), snapshot()), [])

    def test_revoked_link_is_restored_to_its_original_values(self):
        after = snapshot(identities={LINK: (DOCENTE, ISSUER, "sub-1", "0", "2026-10-26 13:00:00.1234567")})
        statements = S.restore_statements(snapshot(), after)
        self.assertEqual(len(statements), 1)
        self.assertIn("estado = 1", statements[0])
        self.assertIn("fechaRevocacion = NULL", statements[0])
        self.assertIn(LINK, statements[0])

    def test_restore_uses_the_captured_values_and_not_constants(self):
        # Estado inicial NO "habitual": vínculo ya inactivo con fecha, usuario inactivo. Debe volver exactamente a eso.
        before = snapshot(users={DOCENTE: "0"}, identities={LINK: (DOCENTE, ISSUER, "sub-1", "0", "2026-01-02 03:04:05.6789012")})
        after = snapshot(users={DOCENTE: "1"}, identities={LINK: (DOCENTE, ISSUER, "sub-1", "1", "NULL")})
        statements = " ".join(S.restore_statements(before, after))
        self.assertIn(f"estado = 0 WHERE id = '{DOCENTE}'", statements)
        self.assertIn("estado = 0, fechaRevocacion = CONVERT(DATETIME2, '2026-01-02 03:04:05.6789012', 121)", statements)

    def test_deactivated_user_is_restored(self):
        statements = S.restore_statements(snapshot(), snapshot(users={DOCENTE: "0", OTHER: "1"}))
        self.assertEqual(statements, [f"UPDATE dbo.Usuario SET estado = 1 WHERE id = '{DOCENTE}'"])

    def test_synthetic_links_created_during_the_run_are_deleted(self):
        after = snapshot(identities={LINK: (DOCENTE, ISSUER, "sub-1", "1", "NULL"), EXTRA_LINK: (OTHER, ISSUER, "x", "1", "NULL")})
        self.assertEqual(S.restore_statements(snapshot(), after), [f"DELETE FROM dbo.IdentidadExterna WHERE id = '{EXTRA_LINK}'"])

    def test_residue_report_names_tables_links_and_users(self):
        after = snapshot(users={DOCENTE: "0", OTHER: "1"}, counts={"dbo.Usuario": 6, "dbo.IdentidadExterna": 1},
                         identities={LINK: (DOCENTE, ISSUER, "sub-1", "0", "NULL")})
        report = " | ".join(S.residue_report(snapshot(), after))
        self.assertIn("dbo.Usuario: 5 -> 6", report)
        self.assertIn("IdentidadExterna difiere", report)
        self.assertIn("Usuario.estado difiere", report)

    def test_append_only_audit_growth_is_informed_but_other_growth_and_shrinkage_are_residues(self):
        audit = {"dbo.Usuario": 5, "dbo.IdentidadExterna": 1, "dbo.AuditoriaEvento": 269}
        before = snapshot(counts={"dbo.Usuario": 5, "dbo.IdentidadExterna": 1, "dbo.AuditoriaEvento": 268})
        after = snapshot(counts=audit)
        self.assertEqual(S.audit_growth(before, after), {"dbo.AuditoriaEvento": 1})
        self.assertEqual(S.residue_report(before, after, tolerated_growth=S.APPEND_ONLY_TABLES), [])
        self.assertTrue(S.residue_report(before, after))  # sin tolerancia explícita sigue siendo una diferencia
        shrunk = snapshot(counts=dict(audit, **{"dbo.AuditoriaEvento": 100}))
        self.assertTrue(S.residue_report(before, shrunk, tolerated_growth=S.APPEND_ONLY_TABLES))
        leaked = snapshot(counts=dict(audit, **{"dbo.Usuario": 6}))
        self.assertTrue(S.residue_report(before, leaked, tolerated_growth=S.APPEND_ONLY_TABLES))

    def test_restore_and_verify_applies_statements_and_reports_what_remains(self):
        class Target(FakeTarget):
            def __init__(self):
                super().__init__()
                self.state = snapshot(users={DOCENTE: "0", OTHER: "1"})

        target = Target()
        with mock.patch.object(S, "capture_state", side_effect=lambda t: target.state), \
                mock.patch.object(target, "sql", side_effect=lambda q, database=None: target.state.update(users={DOCENTE: "1", OTHER: "1"})):
            self.assertEqual(S.restore_and_verify(target, snapshot()), [])

        stuck = Target()
        with mock.patch.object(S, "capture_state", side_effect=lambda t: stuck.state), \
                mock.patch.object(stuck, "sql", return_value=""):
            self.assertTrue(S.restore_and_verify(stuck, snapshot()))


class DeleteCreatedUserTest(unittest.TestCase):
    FKS = [["dbo.Docente", "usuario"], ["dbo.IdentidadExterna", "usuario"], ["dbo.ArchivoSoporte", "usuarioPropietario"]]

    def target(self):
        return FakeTarget({"FROM dbo.uv_usuario": DOCENTE, "sys.foreign_keys": self.FKS})

    def test_nothing_is_deleted_when_the_user_existed_before_the_run(self):
        target = self.target()
        self.assertEqual(S.delete_created_user(target, "e2e@uco.edu.co", existed_before=True), 0)
        self.assertEqual(target.executed, [])

    def test_created_user_and_fk_dependents_are_deleted_in_order(self):
        target = self.target()
        self.assertEqual(S.delete_created_user(target, "e2e@uco.edu.co", existed_before=False), 1)
        deletes = [q.replace("SET NOCOUNT ON; ", "") for q in target.executed if q.startswith("DELETE")]
        self.assertEqual(deletes, [
            f"DELETE FROM dbo.Docente WHERE [usuario] = '{DOCENTE}'",
            f"DELETE FROM dbo.IdentidadExterna WHERE [usuario] = '{DOCENTE}'",
            f"DELETE FROM dbo.ArchivoSoporte WHERE [usuarioPropietario] = '{DOCENTE}'",
            f"DELETE FROM dbo.Usuario WHERE id = '{DOCENTE}'"])

    def test_missing_user_deletes_nothing(self):
        target = FakeTarget({"FROM dbo.uv_usuario": "", "sys.foreign_keys": self.FKS})
        self.assertEqual(S.delete_created_user(target, "e2e@uco.edu.co", existed_before=False), 0)
        self.assertFalse(any(q.startswith("DELETE") for q in target.executed))


class MarkDisposableTest(unittest.TestCase):
    def test_marking_requires_a_labelled_unprotected_container(self):
        for info in (container(label=None), container(name="sql_server_asistencias")):
            target = FakeTarget()
            refused(lambda: S.mark_disposable(target, inspect=lambda name: info))
            self.assertEqual(target.executed, [])

    def test_marking_a_protected_database_name_is_rejected(self):
        refused(lambda: S.mark_disposable(FakeTarget(database="gestionasistenciadb"), inspect=lambda name: container()))

    def test_marking_adds_then_updates_the_property(self):
        target = FakeTarget({"COUNT(*)": "0"})
        S.mark_disposable(target, inspect=lambda name: container())
        self.assertIn("sp_addextendedproperty", target.executed[-1])
        target = FakeTarget({"COUNT(*)": "1"})
        S.mark_disposable(target, inspect=lambda name: container())
        self.assertIn("sp_updateextendedproperty", target.executed[-1])


class ScriptsRefuseUnsafeTargetsTest(unittest.TestCase):
    """Con un destino no desechable los E2E terminan con código 2 antes de tocar Keycloak, SQL o el backend."""

    def run_script(self, module, container_name="sql_server_asistencias", label=None):
        with tempfile.TemporaryDirectory() as folder:
            password_file = os.path.join(folder, "pw")
            with open(password_file, "w", encoding="ascii") as handle:
                handle.write("not-a-real-password")
            argv = ["--sql-container", container_name, "--sql-password-file", password_file]
            boom = AssertionError("el E2E tocó un servicio antes de validar el destino")
            with mock.patch.object(S, "inspect_container", return_value=container(name=container_name, label=label,
                                                                                    host_port="1433" if label is None else "14333")), \
                    mock.patch.object(S.Target, "sql", side_effect=boom), \
                    mock.patch.object(module.L, "read_env", side_effect=boom), \
                    mock.patch.object(module.L, "kc_admin_token", side_effect=boom), \
                    mock.patch.object(module.L, "http", side_effect=boom), \
                    mock.patch.object(module.E, "request", side_effect=boom), \
                    mock.patch.object(module.spa_token, "get_token", side_effect=boom), \
                    contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit) as caught:
                    module.main(argv)
            return caught.exception.code

    def test_identity_binding_refuses_the_development_database(self):
        self.assertEqual(self.run_script(IDENTITY), 2)

    def test_provisioning_refuses_the_development_database(self):
        self.assertEqual(self.run_script(PROVISIONING), 2)

    def test_unlabelled_container_is_refused_by_both(self):
        self.assertEqual(self.run_script(IDENTITY, "otra-base"), 2)
        self.assertEqual(self.run_script(PROVISIONING, "otra-base"), 2)

    def test_arguments_are_mandatory(self):
        for module in (IDENTITY, PROVISIONING):
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as caught:
                module.main([])
            self.assertNotEqual(caught.exception.code, 0)


class ProfileAssignmentTest(unittest.TestCase):
    def test_revoked_assignments_are_history_not_grants(self):
        now = "2026-10-26T18:57:07"
        self.assertTrue(PROFILES.effective({"validUntil": None}, now))
        self.assertTrue(PROFILES.effective({}, now))
        self.assertTrue(PROFILES.effective({"validUntil": "2026-10-26T18:58:00.123456789Z"}, now))
        self.assertFalse(PROFILES.effective({"validUntil": "2026-10-26T18:57:06.356939792Z"}, now))

    def test_assignment_state_ignores_revoked_rows_and_diff_reports_new_grants(self):
        rows = {"/api/v1/profiles/p1/assignments": [{"id": "a1", "userId": "u1", "validUntil": None},
                                                    {"id": "a2", "userId": "u2", "validUntil": "2020-01-01T00:00:00Z"}],
                "/api/v1/roles/r1/assignments": []}
        before = PROFILES.assignment_state({"Docencia": "p1"}, {"DOCENTE": "r1"}, lambda path: rows[path])
        self.assertEqual(before[("profile", "Docencia")], {("a1", "u1")})
        rows["/api/v1/roles/r1/assignments"] = [{"id": "g1", "userId": "u9"}]
        after = PROFILES.assignment_state({"Docencia": "p1"}, {"DOCENTE": "r1"}, lambda path: rows[path])
        self.assertEqual(PROFILES.assignment_diff(before, before), [])
        self.assertEqual(len(PROFILES.assignment_diff(before, after)), 1)

    def test_expectations_come_from_the_access_catalog_not_from_a_hardcoded_matrix(self):
        everything_denied = lambda path: "DENY"  # noqa: E731
        self.assertEqual(PROFILES.mismatches(set(), everything_denied), [])
        # COORDINADOR también ve /admin/areas según el catálogo: negarlo es una discrepancia, y permitirlo a DOCENTE también.
        self.assertTrue(any("/api/v1/admin/areas" in m for m in PROFILES.mismatches({"COORDINADOR"}, everything_denied)))
        allow_docente_only = lambda path: "ALLOW" if path == PROFILES.ROUTES["DOCENTE"] else "DENY"  # noqa: E731
        self.assertEqual(PROFILES.mismatches({"DOCENTE"}, allow_docente_only), [])
        self.assertTrue(PROFILES.mismatches(set(), allow_docente_only))
        self.assertTrue(PROFILES.mismatches({"DOCENTE"}, lambda path: "OTHER"))

    def test_pep_outcomes_distinguish_security_denial_from_business_denial(self):
        self.assertTrue(PROFILES.pep_denied(403, {}))
        self.assertFalse(PROFILES.pep_denied(403, {"X-Decision-Id": "d"}))  # 403 de negocio tras un ALLOW
        self.assertTrue(PROFILES.pep_allowed(403, {"X-Decision-Id": "d"}))
        self.assertFalse(PROFILES.pep_allowed(401, {"X-Decision-Id": "d"}))
        self.assertFalse(PROFILES.pep_allowed(200, {}))

    def test_script_refuses_the_development_database_before_touching_anything(self):
        self.assertEqual(ScriptsRefuseUnsafeTargetsTest().run_script(PROFILES), 2)


class KeycloakViewTest(unittest.TestCase):
    def test_volatile_fields_are_ignored_when_comparing_restored_accounts(self):
        original = {"username": "u", "email": "a@b", "emailVerified": True, "enabled": True, "firstName": "A", "lastName": "B",
                    "attributes": {"idUsuario": ["x"]}, "requiredActions": [], "createdTimestamp": 1, "notBefore": 0}
        restored = dict(original, createdTimestamp=2, notBefore=99, access={"manage": True})
        self.assertEqual(IDENTITY.keycloak_view(original), IDENTITY.keycloak_view(restored))
        self.assertNotEqual(IDENTITY.keycloak_view(original), IDENTITY.keycloak_view(dict(restored, email="otro@b")))


if __name__ == "__main__":
    unittest.main()
