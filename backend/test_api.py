import tempfile
import time
import unittest
from pathlib import Path
from types import SimpleNamespace
from uuid import uuid4

from fastapi.testclient import TestClient

from backend.engine import NexusEngine, StrictRouter, public_addresses, public_url
from backend.server import PublicError, create_app
from backend.store import Store

TOKEN = "test-only-token-not-a-deployment-secret-000000"


class RecordingEngine:
    """Test double only. Production constructs NexusEngine, never this object."""
    def __init__(self):
        self.calls = 0

    def execute(self, req, store):
        self.calls += 1
        if req.text == "fail":
            raise RuntimeError("secret=do-not-leak")
        if req.text == "offline":
            raise PublicError("No selected LLM is available.")
        if req.action == "chat":
            store.add(str(req.session_id), "user", req.text)
        return {"action": req.action}

    def media_path(self, key):
        return None


class ApiTests(unittest.TestCase):
    def setUp(self):
        test_root = Path(__file__).resolve().parent.parent / "backend-data" / "tests"
        test_root.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=test_root)
        self.engine = RecordingEngine()
        self.app = create_app(self.engine, Path(self.temp.name), TOKEN)
        self.client = TestClient(self.app)
        self.client.__enter__()
        self.client.headers["Authorization"] = f"Bearer {TOKEN}"

    def tearDown(self):
        self.client.__exit__(None, None, None)
        self.temp.cleanup()

    def wait(self, job):
        for _ in range(100):
            result = self.client.get("/api/jobs/" + job["id"]).json()
            if result["state"] in {"succeeded", "failed"}:
                return result
            time.sleep(.01)
        self.fail("Job did not complete")

    def test_all_endpoints_require_token(self):
        self.client.headers.pop("Authorization")
        for path in ["/api/health", "/api/sessions", "/api/sessions/" + str(uuid4()), "/api/jobs/" + str(uuid4()), "/api/media/abc"]:
            self.assertEqual(401, self.client.get(path).status_code)
        self.assertEqual(401, self.client.post("/api/jobs", json={"action": "overview"}).status_code)

    def test_health_contract(self):
        response = self.client.get("/api/health")
        self.assertEqual(2, response.json()["api_version"])
        self.assertEqual("no-store", response.headers["cache-control"])

    def test_idempotency_and_session_persistence(self):
        body = {"action": "chat", "text": "test", "request_id": str(uuid4()), "session_id": str(uuid4())}
        job = self.client.post("/api/jobs", json=body)
        self.assertEqual(202, job.status_code)
        self.assertEqual("succeeded", self.wait(job.json())["state"])
        self.client.post("/api/jobs", json=body)
        self.assertEqual(1, self.engine.calls)
        messages = self.client.get("/api/sessions/" + body["session_id"]).json()["messages"]
        self.assertEqual("test", messages[0]["content"])
        self.assertEqual(1, len(self.client.get("/api/sessions").json()["sessions"]))
        body["text"] = "different"
        self.assertEqual(409, self.client.post("/api/jobs", json=body).status_code)

    def test_validation(self):
        for body in [{"action": "shell"}, {"action": "chat", "text": " "},
                     {"action": "images", "text": "x", "options": {"width": 999}},
                     {"action": "web", "text": "x", "options": {"max_results": 999}},
                     {"action": "overview", "secret": "x"}]:
            self.assertEqual(422, self.client.post("/api/jobs", json=body).status_code)
        self.assertEqual(413, self.client.post("/api/jobs", content=b"x" * 1_100_001).status_code)

    def test_failed_job_does_not_leak_exception(self):
        result = self.wait(self.client.post("/api/jobs", json={"action": "chat", "text": "fail"}).json())
        self.assertEqual("failed", result["state"])
        self.assertNotIn("secret", result["error"])
        self.assertIsNone(result["result"])

    def test_offline_has_no_fake_reply(self):
        result = self.wait(self.client.post("/api/jobs", json={"action": "chat", "text": "offline"}).json())
        self.assertEqual("failed", result["state"])
        self.assertIsNone(result["result"])

    def test_unknown_resources(self):
        self.assertEqual(404, self.client.get("/api/jobs/" + str(uuid4())).status_code)
        self.assertEqual(404, self.client.get("/api/media/" + "a" * 64).status_code)

    def test_restart_marks_incomplete_jobs_failed(self):
        from backend.models import JobRequest
        store = Store(Path(self.temp.name) / "restart.sqlite3")
        req = JobRequest(action="overview")
        store.enqueue(req)
        reopened = Store(store.path)
        self.assertEqual("failed", reopened.get(str(req.request_id))["state"])

    def test_strict_router_rejects_engine_fallback(self):
        router = SimpleNamespace(generate=lambda _: SimpleNamespace(success=False, provider="fallback", text="not real"))
        with self.assertRaises(PublicError):
            StrictRouter(router).generate(None)

    def test_a_request_stopped_by_the_content_rule_shows_the_rules_own_words(self):
        """The engine raises ContentRuleBlocked for research, knowledge and images; the phone must not call that a failure of the engine."""
        class ContentRuleBlocked(RuntimeError):
            def __init__(self):
                super().__init__("internal detail")
                self.reply = "Not written. This app has one content rule."

        engine = NexusEngine.__new__(NexusEngine)  # no engine checkout is needed to test the mapping

        def stopped(req, store):
            raise ContentRuleBlocked()

        engine._execute = stopped
        with self.assertRaises(PublicError) as raised:
            engine.execute(None, None)
        self.assertEqual("Not written. This app has one content rule.", str(raised.exception))

        def broken(req, store):
            raise RuntimeError("secret=do-not-leak")

        engine._execute = broken
        with self.assertRaises(RuntimeError):  # anything else stays an internal error, reported without its text
            engine.execute(None, None)

        class Lookalike(RuntimeError):
            reply = "attacker-chosen text"

        def lookalike(req, store):
            raise Lookalike("x")

        engine._execute = lookalike
        with self.assertRaises(Lookalike):  # only the engine's own exception is shown as written
            engine.execute(None, None)

    def test_a_checked_website_address_is_returned_for_pinning(self):
        # An address written out needs no name server, so this runs offline.
        self.assertEqual(["93.184.216.34"], public_addresses("https://93.184.216.34/page"))
        self.assertEqual("https://93.184.216.34/page", public_url("https://93.184.216.34/page"))
        with self.assertRaises(PublicError):
            public_addresses("http://192.168.1.10/")

    def test_private_ingest_url_blocked(self):
        for url in ["file:///etc/passwd", "http://127.0.0.1", "http://10.0.2.2", "http://169.254.169.254", "https://user:pass@example.com"]:
            with self.assertRaises(PublicError):
                public_url(url)


if __name__ == "__main__":
    unittest.main()
